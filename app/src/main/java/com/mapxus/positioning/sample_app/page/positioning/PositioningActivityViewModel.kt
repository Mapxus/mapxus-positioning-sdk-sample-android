package com.mapxus.positioning.sample_app.page.positioning

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mapxus.positioning.api.positioning.MapxusLocation
import com.mapxus.positioning.api.positioning.MapxusPositioningClient
import com.mapxus.positioning.api.positioning.UserMode
import com.mapxus.positioning.sample_app.utils.AppSettingDataStoreKeys
import com.mapxus.positioning.sample_app.utils.appSettingDataStore
import com.mapxus.positioning.sample_app.utils.logD
import com.mapxus.positioning.sample_app.utils.logI
import kotlinx.coroutines.launch

private const val TAG = "PositioningActivityViewModel"

class PositioningActivityViewModel(
    private val context: Application,
) : AndroidViewModel(context) {

    private val mapxusPositioningClient: MapxusPositioningClient =
        MapxusPositioningClient.getInstance(context.applicationContext)

    private val positioningActivityRepository: PositioningActivityRepository =
        PositioningActivityRepository(
            context.applicationContext,
            viewModelScope,
            mapxusPositioningClient,
        )

    val positioningActivityEvent = positioningActivityRepository.positioningActivityEvent
    val positioningActivityUiState = positioningActivityRepository.positioningActivityUiState

    var customLocation: MapxusLocation? = null

    fun startPositioning(
    ) {
        mapxusPositioningClient.setUserMode(positioningActivityRepository.positioningActivityUiState.value.userMode)
        "start replayFiles: customLocation: $customLocation ".logI(TAG)
        val location = customLocation
        if (location != null) {
            mapxusPositioningClient.startWithInitialLocation(location)
        } else {
            mapxusPositioningClient.start()
        }
    }

    fun updateAccuracyRadius(
        newLocation: Boolean = false
    ) {
        positioningActivityRepository.updateAccuracyRadius(newLocation)
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

            positioningActivityRepository.updateUserMode(userMode)
        }
    }

    fun switchAlwaysFollowMap() {
        viewModelScope.launch {
            context.appSettingDataStore.edit { preferences ->
                preferences[AppSettingDataStoreKeys.IS_FOLLOW_MAP] =
                    !positioningActivityUiState.value.isAlwaysFollow
            }
            positioningActivityRepository.updateAlwaysFollowMap()
        }
    }

    fun isSettingCustomLocation(isSetting: Boolean) {
        positioningActivityRepository.isSettingCustomLocation(isSetting)
    }

    fun isSettingCustomLocation(): Boolean {
        return positioningActivityRepository.positioningActivityUiState.value.isSettingCustomLocation
    }

    fun clearCache() {
        positioningActivityRepository.clearCache()
    }

    fun stop() {
        viewModelScope.launch {
            clearCache()
            mapxusPositioningClient.stop()
        }
    }

    fun refreshLocation(): Boolean {
        return mapxusPositioningClient.refreshLocation()
    }

    override fun onCleared() {
        super.onCleared()
        positioningActivityRepository.onCleared()
        mapxusPositioningClient.removePositioningListener(
            positioningActivityRepository
        )
    }

}