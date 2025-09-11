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

/**
 * Mapxus positioning provider
 *
 * core sdk 控制显示蓝点
 *
 * @constructor Create empty Mapxus positioning provider
 */
class MapxusPositioningProvider() : IndoorLocationProvider(), MapxusPositioningListener {

    //ignore
    override fun supportsFloor(): Boolean {
        return true
    }

    //ignore
    override fun start() {
    }

    //ignore
    override fun stop() {
    }

    //ignore
    override fun isStarted(): Boolean {
        return true
    }

    /**
     * Callback when the positioning state changes.
     * Positioning state can be one of the following values:
     * STOPPED: Positioning is stopped
     * INITIALIZING: Positioning is initializing
     * RUNNING: Positioning is running
     * STOPPING: Positioning is stopping
     * You can also get positioning state actively by MapxusPositioningClient.getState()
     *
     * @param state Positioning State
     * @see PositioningState
     */
    override fun onStateChange(state: PositioningState) {
        when (state) {
            PositioningState.STOPPED -> {
                dispatchOnProviderStopped()
            }

            PositioningState.RUNNING -> dispatchOnProviderStarted()
            PositioningState.INITIALIZING -> {}
            PositioningState.STOPPING -> {}
        }
    }

    /**
     * Callback for the user's real-time bearing during positioning.
     * The bearing value ranges between (0, 360], where:
     * 360 represents true north,
     * 90 represents true east,
     * 180 represents true south, and
     * 270 represents true west.
     *
     * @param bearing The bearing value, in the range (0, 360].
     */
    override fun onBearingChange(bearing: Float) {
        dispatchCompassChange(bearing, 0)
    }

    /**
     * Location information callback, including venue ID , building ID, floor, longitude & latitude,
     * positioning accuracy, and timestamp.
     * If it is an outdoor location result, the venueId, buildingId, and floor will be null.
     * If it is an indoor location on a shared floor, the buildingId will be null.
     * Callback cases:
     * Indoor location
     * on floor: venueId, buildingId, mapxusFloor(type="FLOOR"),latitude, longitude, accuracy, timestamp
     * on shared floor: venueId, mapxusFloor(type="SHARED_FLOOR"), latitude, longitude, accuracy, timestamp
     * Outdoor location: latitude, longitude, accuracy，timestamp
     * @param location Location of the user
     * @see MapxusLocation
     */
    override fun onLocationChange(location: MapxusLocation) {
        //组建core sdk需要的location对象
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
        dispatchIndoorLocationChange(indoorLocation)
    }

    /**
     * Callback of the real-time speed change of the wheelchair while positioning.
     * First speed change callback happens with default value(1.0 m/s) when positioning starts.
     *
     * @param speed m/s
     */
    override fun onWheelchairSpeedChange(speed: Float) {
    }

    /**
     * Callback of the positioning mode change
     *
     * @param mode Positioning mode
     */
    override fun onPositioningModeChange(mode: PositioningMode) {
    }

    /**
     * Callback that contains feedback information while using the client.
     *
     *
     * UserFeedbackInfo.userFeedbackType can be one of the following values:
     * ERROR_LEVEL1: Info feedback that may mildly affect the positioning experience of SDK.
     * ERROR_LEVEL2: Warning feedback that may severely affect the positioning
     * experience of SDK and recommend user to stop positioning.
     * ERROR_LEVEL3: Error feedback that forces SDK to stop positioning.
     *
     * @param userFeedbackInfo Feedback info
     * @see UserFeedbackInfo
     */
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
