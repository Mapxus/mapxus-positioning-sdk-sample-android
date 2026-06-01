package com.mapxus.positioning.sample_app.page.positioning

import android.app.Application
import android.location.Location
import androidx.collection.LruCache
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mapxus.map.mapxusmap.api.map.MapxusMap
import com.mapxus.map.mapxusmap.api.services.VenueSearch
import com.mapxus.map.mapxusmap.api.services.model.DetailSearchOption
import com.mapxus.map.mapxusmap.api.services.model.building.FloorInfo
import com.mapxus.map.mapxusmap.api.services.model.floor.SharedFloor
import com.mapxus.map.mapxusmap.api.services.model.venue.VenueInfo
import com.mapxus.map.mapxusmap.positioning.IndoorLocation
import com.mapxus.positioning.api.UserFeedbackInfo
import com.mapxus.positioning.api.positioning.BearingAccuracy
import com.mapxus.positioning.api.positioning.MapxusFloor
import com.mapxus.positioning.api.positioning.MapxusLocation
import com.mapxus.positioning.api.positioning.MapxusPositioningClient
import com.mapxus.positioning.api.positioning.MapxusPositioningListener
import com.mapxus.positioning.api.positioning.PositioningMode
import com.mapxus.positioning.api.positioning.PositioningState
import com.mapxus.positioning.api.positioning.UserMode
import com.mapxus.positioning.sample_app.page.positioning.mapview.MapxusPositioningProvider
import com.mapxus.positioning.sample_app.page.positioning.model.PositioningActivityUiState
import com.mapxus.positioning.sample_app.utils.AppSettingDataStoreKeys
import com.mapxus.positioning.sample_app.utils.appSettingDataStore
import com.mapxus.positioning.sample_app.utils.getName
import com.mapxus.positioning.sample_app.utils.logD
import com.mapxus.positioning.sample_app.utils.logI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "PositioningActivityViewModel"

/**
 * Positioning activity view model
 *
 * 处理数据相关
 *
 * @property context
 * @constructor Create empty Positioning activity view model
 */
class PositioningActivityViewModel(
    private val context: Application,
) : AndroidViewModel(context), MapxusPositioningListener {

    /**
     * Mapxus positioning provider
     *
     * core sdk 显示蓝点逻辑对象
     */
    val mapxusPositioningProvider: MapxusPositioningProvider = MapxusPositioningProvider()

    /**
     * Mapxus positioning client
     *
     * 定位SDK Client
     */
    private val mapxusPositioningClient: MapxusPositioningClient =
        MapxusPositioningClient.getInstance(context.applicationContext)

    /**
     * 当前位置
     */
    private var currentLocation: MapxusLocation? = null

    /**
     * UI显示变化
     */
    private val _positioningActivityUiState: MutableStateFlow<PositioningActivityUiState> =
        MutableStateFlow(PositioningActivityUiState())
    val positioningActivityUiState = _positioningActivityUiState.asStateFlow()

    /**
     * Feedback message thread
     *
     * 用于feedback信息展示的线程
     */
    private val feedbackMessageThread = CoroutineScope(Dispatchers.Main)

    /**
     * Feedback message cache
     *
     * 用于feedback信息展示的缓存
     *
     */
    private val feedbackMessageCache = LruCache<Long, UserFeedbackInfo>(3)

    /**
     *  venue id to venueInfo
     *
     *  场地信息缓存
     */
    private val siteCache = LruCache<String, VenueInfo>(3)

    /**
     * Venue search
     *
     * core sdk 搜索venue信息对象
     */
    private val venueSearch: VenueSearch = VenueSearch.newInstance()

    /**
     * Follow user mode changed listener
     *
     * core sdk 监听跟随模式变化
     */
    val followUserModeChangedListener = MapxusMap.OnFollowUserModeChangedListener { p0 ->
        _positioningActivityUiState.update {
            it.copy(
                followUserMode = p0
            )
        }
    }

    /**
     * Custom location
     *
     * 自定义位置缓存
     */
    var customLocation: MapxusLocation? = null

    init {
        //添加监听器
        mapxusPositioningClient.addPositioningListener(this)
        //更新缓存
        viewModelScope.launch {
            val preferences = context.applicationContext.appSettingDataStore.data.first()

            val currentPositioningMode =
                preferences[AppSettingDataStoreKeys.POSITIONING_MODE].takeIf { !it.isNullOrBlank() }
                    ?.let {
                        val result = UserMode.valueOf(it)
                        mapxusPositioningClient.setUserMode(result)
                        result
                    }
            " preference:$currentPositioningMode ".logD()
            "current positioning mode $currentPositioningMode".logI(TAG)

            updateUserMode(currentPositioningMode ?: UserMode.PEDESTRIAN)
        }
    }

    /**
     * Update user mode
     *
     * 更新用户模式缓存
     *
     * @param userMode
     */
    fun updateUserMode(userMode: UserMode) {
        mapxusPositioningClient.setUserMode(userMode)
        _positioningActivityUiState.update {
            it.copy(
                userMode = userMode
            )
        }
    }

    /**
     * Start positioning
     *
     */
    fun startPositioning() {
        //设置当前用户模式
        mapxusPositioningClient.setUserMode(positioningActivityUiState.value.userMode)
        "start customLocation: $customLocation ".logI(TAG)
        val location = customLocation
        if (location != null) {
            mapxusPositioningClient.startWithInitialLocation(location)
        } else {
            mapxusPositioningClient.start()
        }
    }

    /**
     * Toggle positioning mode
     *
     * 切换用户模式并更新缓存
     *
     */
    fun togglePositioningMode() {
        viewModelScope.launch {
            val userMode = when (positioningActivityUiState.value.userMode) {
                UserMode.PEDESTRIAN -> UserMode.WHEELCHAIR
                UserMode.WHEELCHAIR -> UserMode.PEDESTRIAN
            }
            "togglePositioningMode ${positioningActivityUiState.value.userMode} $userMode".logD()
            context.appSettingDataStore.edit { preferences ->
                preferences[AppSettingDataStoreKeys.POSITIONING_MODE] = userMode.name
            }

            updateUserMode(userMode)
        }
    }

    /**
     * Is setting custom location
     *
     * 更新是否正在自定义位置状态
     *
     * @param isSetting
     */
    fun isSettingCustomLocation(isSetting: Boolean) {
        _positioningActivityUiState.update {
            it.copy(
                isSettingCustomLocation = isSetting
            )
        }
    }

    /**
     * Is setting custom location
     *
     * 是否正在自定义位置状态
     *
     * @return
     */
    fun isSettingCustomLocation(): Boolean {
        return positioningActivityUiState.value.isSettingCustomLocation
    }

    /**
     * Clear cache
     *
     * 清理缓存
     *
     */
    fun clearCache() {
        currentLocation = null
    }

    /**
     * Stop
     *
     * 停止定位
     */
    fun stop() {
        clearCache()
        mapxusPositioningClient.stop()
    }

    /**
     * Refresh location
     *
     * 刷新位置
     *
     * @return
     */
    fun refreshLocation(): Boolean {
        return mapxusPositioningClient.refreshLocation()
    }

    /**
     * On cleared
     *
     * view model 触发
     *
     */
    override fun onCleared() {
        super.onCleared()
        feedbackMessageThread.cancel()
        mapxusPositioningClient.removePositioningListener(
            this
        )
    }

    override fun onStateChange(state: PositioningState) {
        "receive state change: $state".logI(TAG)
        _positioningActivityUiState.update {
            it.copy(
                currentPositioningState = state,
            )
        }
    }

    override fun onBearingChange(bearing: Float) {
        //更新蓝点方向
        mapxusPositioningProvider.dispatchCompassChange(bearing, 0)
    }

    override fun onBearingAccuracyChange(accuracy: BearingAccuracy) {
        if (accuracy == BearingAccuracy.LOW) {
            mapxusPositioningClient.pause()
            _positioningActivityUiState.update {
                it.copy(
                    isShowPoorAccuracyNeedCalibratingDialog = true
                )
            }
        }

        _positioningActivityUiState.update {
            it.copy(
                currentAccuracyLevel = accuracy
            )
        }
    }

    fun dismissPoorAccuracyNeedCalibratingDialogAndResumePositioning() {
        mapxusPositioningClient.resume()
        _positioningActivityUiState.update {
            it.copy(
                isShowPoorAccuracyNeedCalibratingDialog = false
            )
        }
    }

    override fun onLocationChange(location: MapxusLocation) {
        if (currentLocation?.mapxusFloor?.id != location.mapxusFloor?.id) {
            updateSiteInfo(location)
        }

        _positioningActivityUiState.update {
            it.copy(
                currentLocation = location,
            )
        }

        //处理蓝点位置更新
        val theLocation = Location("MapxusPositioning")
        theLocation.latitude = location.latitude
        theLocation.longitude = location.longitude
        theLocation.time = System.currentTimeMillis()
        val building = location.buildingId
        val floorInfo = location.mapxusFloor?.run {
            when (type) {
                MapxusFloor.Type.FLOOR -> FloorInfo(id, code, ordinal)
                MapxusFloor.Type.SHARED_FLOOR -> SharedFloor(id, code, ordinal)
            }
        }
        val indoorLocation = IndoorLocation(building, floorInfo, theLocation)
        indoorLocation.accuracy = location.accuracy
        //分发
        mapxusPositioningProvider.dispatchIndoorLocationChange(indoorLocation)
    }

    override fun onWheelchairSpeedChange(speed: Float) {
    }

    override fun onPositioningModeChange(mode: PositioningMode) {
        _positioningActivityUiState.update {
            it.copy(
                currentPositioningMode = mode
            )
        }
    }

    /**
     * Update site info
     *
     * 更新场地信息
     *
     * @param mapxusLocation
     */
    private fun updateSiteInfo(mapxusLocation: MapxusLocation) {
        viewModelScope.launch(Dispatchers.Main) {
            var currentLocateSiteName = ""
            "updateBuildingInfo mapxusLocation $mapxusLocation  $currentLocation".logD(TAG)
            if (mapxusLocation.venueId == null) {
                currentLocateSiteName = "outdoor"
            } else if (mapxusLocation.mapxusFloor!!.type == MapxusFloor.Type.FLOOR && siteCache[mapxusLocation.venueId!!] != null) {
                val buildingName =
                    siteCache[mapxusLocation.venueId!!]?.buildings?.find { it.buildingId == mapxusLocation.buildingId }?.buildingNamesMap?.getName()
                currentLocateSiteName =
                    "${mapxusLocation.mapxusFloor?.code} , $buildingName"
            } else if (siteCache[mapxusLocation.venueId!!] != null) {
                val venueInfo = siteCache[mapxusLocation.venueId!!]
                currentLocateSiteName =
                    "${mapxusLocation.mapxusFloor?.code} , ${venueInfo?.nameMap?.getName()}-VB"
            } else {
                venueSearch.searchVenueDetail(
                    DetailSearchOption().id(mapxusLocation.venueId)
                ) { venueDetailResult ->
                    venueDetailResult.takeIf { venueResult -> venueResult.status == 0 && !venueResult.venueInfoList.isNullOrEmpty() }
                        ?.venueInfoList?.first()?.let { venueInfo ->
                            siteCache.put(mapxusLocation.venueId!!, venueInfo)
                            if (currentLocation?.venueId == venueInfo.id) {
                                updateSiteInfo(
                                    currentLocation!!
                                )
                            }
                        }

                }
            }

            _positioningActivityUiState.update {
                it.copy(
                    currentLocateSiteName = currentLocateSiteName
                )
            }
        }
    }

    override fun onFeedback(userFeedbackInfo: UserFeedbackInfo) {
        val key = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.Main) {
            "onFeedback:$userFeedbackInfo ".logI(TAG)
            feedbackMessageCache.put(key, userFeedbackInfo)
            updateFeedbackMessage()
        }

        feedbackMessageThread.launch {
            delay(3000)
            feedbackMessageCache.remove(key)
            updateFeedbackMessage()
        }
    }

    /**
     * Update feedback message
     *
     * 更新feedback信息
     *
     */
    private fun updateFeedbackMessage() {
        _positioningActivityUiState.update {
            it.copy(
                feedbackMessages = feedbackMessageCache.snapshot().values.toList()
                    .reversed()
            )
        }
    }
}