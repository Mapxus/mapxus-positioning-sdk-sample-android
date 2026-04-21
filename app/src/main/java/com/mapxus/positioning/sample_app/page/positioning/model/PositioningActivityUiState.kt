package com.mapxus.positioning.sample_app.page.positioning.model

import com.mapxus.map.mapxusmap.api.map.FollowUserMode
import com.mapxus.positioning.api.UserFeedbackInfo
import com.mapxus.positioning.api.positioning.MapxusLocation
import com.mapxus.positioning.api.positioning.PositioningMode
import com.mapxus.positioning.api.positioning.PositioningState
import com.mapxus.positioning.api.positioning.UserMode

data class PositioningActivityUiState(
    val currentPositioningState: PositioningState? = null,
    val currentLocateSiteName: String = "",
    val userMode: UserMode = UserMode.PEDESTRIAN,
    val followUserMode: Int = FollowUserMode.FOLLOW_USER,
    val isSettingCustomLocation: Boolean = false,
    val currentPositioningMode: PositioningMode? = null,
    val currentLocation: MapxusLocation? = null,
    val feedbackMessages: List<UserFeedbackInfo> = emptyList(),
) {
    val isShowLoadingDialog: Boolean
        get() = currentPositioningState == PositioningState.INITIALIZING
}
