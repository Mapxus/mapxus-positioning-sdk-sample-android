package com.mapxus.positioning.sample_app.page.positioning.model

import com.mapxus.positioning.api.positioning.MapxusLocation
import com.mapxus.positioning.api.positioning.PositioningState

sealed class PositioningActivityEvent {

    data class LocationEvent(val mapxusLocation: MapxusLocation, val isAlwaysFollowMap: Boolean) :
        PositioningActivityEvent()

    data class BearingEvent(val bearing: Double) : PositioningActivityEvent()

    data class PositioningStateChangeEvent(val positioningState: PositioningState) :
        PositioningActivityEvent()

}