package com.mapxus.positioning.sample_app.page.positioning.mapview

import android.location.Location
import com.mapxus.map.mapxusmap.api.services.model.building.FloorInfo
import com.mapxus.map.mapxusmap.api.services.model.floor.SharedFloor
import com.mapxus.map.mapxusmap.positioning.ErrorInfo
import com.mapxus.map.mapxusmap.positioning.IndoorLocation
import com.mapxus.map.mapxusmap.positioning.IndoorLocationProvider
import com.mapxus.positioning.api.UserFeedbackInfo
import com.mapxus.positioning.api.UserFeedbackType
import com.mapxus.positioning.api.positioning.MapxusFloor
import com.mapxus.positioning.api.positioning.MapxusLocation
import com.mapxus.positioning.api.positioning.MapxusPositioningListener
import com.mapxus.positioning.api.positioning.PositioningMode
import com.mapxus.positioning.api.positioning.PositioningState

private const val TAG = "MapxusPositioningProvider"

class MapxusPositioningProvider() : IndoorLocationProvider(), MapxusPositioningListener {
    private var started = false

    override fun supportsFloor(): Boolean {
        return true
    }

    override fun start() {
        started = true
    }

    override fun stop() {
        started = false
    }

    override fun isStarted(): Boolean {
        return started
    }

    override fun onStateChange(state: PositioningState) {
        when (state) {
            PositioningState.STOPPED -> {
                started = false
                dispatchOnProviderStopped()
            }

            PositioningState.RUNNING -> dispatchOnProviderStarted()
            PositioningState.INITIALIZING -> {}
            PositioningState.STOPPING -> {}
        }
    }

    override fun onBearingChange(bearing: Float) {
        dispatchCompassChange(bearing, 0)
    }

    override fun onLocationChange(location: MapxusLocation) {
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
        dispatchIndoorLocationChange(indoorLocation)
    }

    override fun onWheelchairSpeedChange(speed: Float) {
    }

    override fun onPositioningModeChange(mode: PositioningMode) {
    }

    override fun onFeedback(userFeedbackInfo: UserFeedbackInfo) {
        if (userFeedbackInfo.type >= UserFeedbackType.ERROR_LEVEL3)
            dispatchOnProviderError(
                ErrorInfo(
                    userFeedbackInfo.code.ordinal,
                    userFeedbackInfo.message
                )
            )
    }
}
