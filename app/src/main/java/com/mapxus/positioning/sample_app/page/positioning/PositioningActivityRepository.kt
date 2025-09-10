package com.mapxus.positioning.sample_app.page.positioning

import android.content.Context
import androidx.collection.LruCache
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
import com.mapxus.positioning.sample_app.page.positioning.mapview.LayerProvider.Companion.generateFeatureFormMapxusLocation
import com.mapxus.positioning.sample_app.page.positioning.mapview.POSITION_ACCURACY_ID
import com.mapxus.positioning.sample_app.page.positioning.mapview.POSITION_ARROW_ID
import com.mapxus.positioning.sample_app.page.positioning.mapview.POSITION_ID
import com.mapxus.positioning.sample_app.page.positioning.mapview.PROPERTY_BEARING
import com.mapxus.positioning.sample_app.page.positioning.model.PositioningActivityEvent
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.geojson.FeatureCollection

private const val TAG = "PositioningActivityRepository"

class PositioningActivityRepository(
    private val context: Context,
    private val viewModelScope: CoroutineScope,
    private val mapxusPositioningClient: MapxusPositioningClient,
) : MapxusPositioningListener {
    private var currentLocation: MapxusLocation? = null

    private val _positioningActivityUiState: MutableStateFlow<PositioningActivityUiState> =
        MutableStateFlow(PositioningActivityUiState())
    val positioningActivityUiState = _positioningActivityUiState.asStateFlow()

    private val _positioningActivityEvent: MutableSharedFlow<PositioningActivityEvent> =
        MutableSharedFlow()
    val positioningActivityEvent = _positioningActivityEvent.asSharedFlow()

    private val feedbackMessageThread = CoroutineScope(Dispatchers.Main)

    private val feedbackMessageCache = LruCache<Long, UserFeedbackInfo>(3)

    /**
     *  venue id to venueInfo
     */
    private val siteCache = LruCache<String, VenueInfo>(3)

    private val venueSearch: VenueSearch = VenueSearch.newInstance()

    init {
        mapxusPositioningClient.addPositioningListener(this)
        viewModelScope.launch {
            val preferences = context.applicationContext.appSettingDataStore.data.first()
            val isAlwaysFollow =
                preferences[AppSettingDataStoreKeys.IS_FOLLOW_MAP] ?: false
            "is always Follow Map $isAlwaysFollow".logI(TAG)

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

            _positioningActivityUiState.update {
                it.copy(
                    isAlwaysFollow = isAlwaysFollow
                )
            }
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

    fun updateAlwaysFollowMap() {
        _positioningActivityUiState.update {
            it.copy(
                isAlwaysFollow = !_positioningActivityUiState.value.isAlwaysFollow
            )
        }
    }

    override fun onStateChange(state: PositioningState) {
        "receive state change: $state".logI(TAG)
        viewModelScope.launch {
            _positioningActivityEvent.emit(
                PositioningActivityEvent.PositioningStateChangeEvent(
                    state
                )
            )
        }
        _positioningActivityUiState.update {
            it.copy(
                currentPositioningState = state,
            )
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

    override fun onBearingChange(bearing: Float) {
        currentLocation?.let { location ->
            generateFeatureFormMapxusLocation(location).also {
                it.addNumberProperty(PROPERTY_BEARING, bearing)
            }
        }?.let {
            viewModelScope.launch(Dispatchers.Main) {
                _positioningActivityEvent.emit(
                    PositioningActivityEvent.UpdateSourceEvent(
                        POSITION_ARROW_ID,
                        FeatureCollection.fromFeature(it)
                    )
                )
                if (_positioningActivityUiState.value.isAlwaysFollow) {
                    _positioningActivityEvent.emit(PositioningActivityEvent.BearingEvent(bearing.toDouble()))
                }
            }
        }
    }

    override fun onLocationChange(location: MapxusLocation) {

        viewModelScope.launch {
            if (currentLocation?.mapxusFloor?.id != location.mapxusFloor?.id) {
                updateSiteInfo(location)
            }

            val positionFeature = generateFeatureFormMapxusLocation(location)
            currentLocation = location
            _positioningActivityEvent.emit(
                PositioningActivityEvent.LocationEvent(
                    location,
                    _positioningActivityUiState.value.isAlwaysFollow
                )
            )
            withContext(Dispatchers.Main) {
                _positioningActivityEvent.emit(
                    PositioningActivityEvent.UpdateSourceEvent(
                        POSITION_ID,
                        FeatureCollection.fromFeature(positionFeature),
                    )
                )
                updateAccuracyRadius(true)
            }
        }
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
            } else if (mapxusLocation.venueId != null && siteCache[mapxusLocation.venueId!!] != null) {
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

    fun updateAccuracyRadius(
        newLocation: Boolean = false
    ) {
        currentLocation?.let { location ->
            viewModelScope.launch {
                _positioningActivityEvent.emit(
                    PositioningActivityEvent.UpdateAccuracyRadiusEvent(
                        POSITION_ACCURACY_ID,
                        location,
                        newLocation
                    )
                )
            }
        }
    }

    fun isSettingCustomLocation(isSetting: Boolean) {
        _positioningActivityUiState.update {
            it.copy(
                isSettingCustomLocation = isSetting
            )
        }
    }

    fun clearCache() {
        currentLocation = null
    }

    fun onCleared() {
        feedbackMessageThread.cancel()
    }


}