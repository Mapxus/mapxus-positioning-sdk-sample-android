package com.mapxus.positioning.sample_app.page.positioning

import android.app.Application
import androidx.collection.LruCache
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mapxus.map.mapxusmap.api.map.MapxusMap
import com.mapxus.map.mapxusmap.api.services.VenueSearch
import com.mapxus.map.mapxusmap.api.services.model.DetailSearchOption
import com.mapxus.map.mapxusmap.api.services.model.venue.VenueInfo
import com.mapxus.positioning.api.UserFeedbackInfo
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
    val followUserModeChangedListener = object : MapxusMap.OnFollowUserModeChangedListener {
        override fun OnFollowUserModeChanged(p0: Int) {
            _positioningActivityUiState.update {
                it.copy(
                    followUserMode = p0
                )
            }
        }
    }

    /**
     * Custom location
     *
     * 自定义位置缓存
     */
    var customLocation: MapxusLocation? = null

    init {
        mapxusPositioningClient.addPositioningListener(this)
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
     * @param mapxusPositioningProvider core sdk 显示定位蓝点对象
     */
    fun startPositioning(
        mapxusPositioningProvider: MapxusPositioningProvider
    ) {
        //core sdk 显示定位蓝点对象添加监听器
        mapxusPositioningClient.addPositioningListener(mapxusPositioningProvider)
        mapxusPositioningClient.setUserMode(positioningActivityUiState.value.userMode)
        "start replayFiles: customLocation: $customLocation ".logI(TAG)
        val location = customLocation
        if (location != null) {
            mapxusPositioningClient.startWithInitialLocation(location)
        } else {
            mapxusPositioningClient.start()
        }
    }

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

    fun isSettingCustomLocation(isSetting: Boolean) {
        _positioningActivityUiState.update {
            it.copy(
                isSettingCustomLocation = isSetting
            )
        }
    }

    fun isSettingCustomLocation(): Boolean {
        return positioningActivityUiState.value.isSettingCustomLocation
    }

    fun clearCache() {
        currentLocation = null
    }

    /**
     * Stop
     *
     * @param mapxusPositioningProvider core sdk 显示定位蓝点对象
     */
    fun stop(
        mapxusPositioningProvider: MapxusPositioningProvider
    ) {
        clearCache()
        mapxusPositioningClient.stop()
        //core sdk 显示定位蓝点对象 remove 监听器
        mapxusPositioningClient.removePositioningListener(mapxusPositioningProvider)
    }

    fun refreshLocation(): Boolean {
        return mapxusPositioningClient.refreshLocation()
    }

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
    }

    override fun onLocationChange(location: MapxusLocation) {
        if (currentLocation?.mapxusFloor?.id != location.mapxusFloor?.id) {
            updateSiteInfo(location)
        }

        currentLocation = location
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

    private fun updateFeedbackMessage() {
        _positioningActivityUiState.update {
            it.copy(
                feedbackMessages = feedbackMessageCache.snapshot().values.toList()
                    .reversed()
            )
        }
    }

}