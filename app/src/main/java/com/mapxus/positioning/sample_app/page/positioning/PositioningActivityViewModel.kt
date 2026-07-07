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
 * Handles data-related logic
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
     * core sdk object for the blue dot display logic
     */
    val mapxusPositioningProvider: MapxusPositioningProvider = MapxusPositioningProvider()

    /**
     * Mapxus positioning client
     *
     * Positioning SDK Client
     */
    private val mapxusPositioningClient: MapxusPositioningClient =
        MapxusPositioningClient.getInstance(context.applicationContext)

    /**
     * Current location
     */
    private var currentLocation: MapxusLocation? = null

    /**
     * UI display changes
     */
    private val _positioningActivityUiState: MutableStateFlow<PositioningActivityUiState> =
        MutableStateFlow(PositioningActivityUiState())
    val positioningActivityUiState = _positioningActivityUiState.asStateFlow()

    /**
     * Feedback message thread
     *
     * Thread used for displaying feedback messages
     */
    private val feedbackMessageThread = CoroutineScope(Dispatchers.Main)

    /**
     * Feedback message cache
     *
     * Cache used for displaying feedback messages
     *
     */
    private val feedbackMessageCache = LruCache<Long, UserFeedbackInfo>(3)

    /**
     *  venue id to venueInfo
     *
     *  Venue info cache
     */
    private val siteCache = LruCache<String, VenueInfo>(3)

    /**
     * Venue search
     *
     * core sdk object for searching venue info
     */
    private val venueSearch: VenueSearch = VenueSearch.newInstance()

    /**
     * Follow user mode changed listener
     *
     * core sdk listener for follow mode changes
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
     * Custom location cache
     */
    var customLocation: MapxusLocation? = null

    init {
        //add listener
        mapxusPositioningClient.addPositioningListener(this)
        //update cache
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
     * Update user mode cache
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
        //set current user mode
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
     * Switch user mode and update cache
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
     * Update whether currently setting a custom location
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
     * Whether currently setting a custom location
     *
     * @return
     */
    fun isSettingCustomLocation(): Boolean {
        return positioningActivityUiState.value.isSettingCustomLocation
    }

    /**
     * Clear cache
     *
     * Clear cache
     *
     */
    fun clearCache() {
        currentLocation = null
    }

    /**
     * Stop
     *
     * Stop positioning
     */
    fun stop() {
        clearCache()
        //clear blue dot cache
        mapxusPositioningProvider.dispatchIndoorLocationChange(
            IndoorLocation(
                null,
                null,
                Location("MapxusPositioning").apply {
                    latitude = 0.0
                    longitude = 0.0
                })
        )
        mapxusPositioningClient.stop()
    }

    /**
     * Refresh location
     *
     * Refresh location
     *
     * @return
     */
    fun refreshLocation(): Boolean {
        return mapxusPositioningClient.refreshLocation()
    }

    /**
     * On cleared
     *
     * Triggered by the view model
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
        //update blue dot bearing
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

        //handle blue dot location update
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
        //dispatch
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
     * Update venue info
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
     * Update feedback message
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