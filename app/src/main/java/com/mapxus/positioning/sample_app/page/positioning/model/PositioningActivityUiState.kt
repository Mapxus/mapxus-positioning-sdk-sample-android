package com.mapxus.positioning.sample_app.page.positioning.model

import com.mapxus.positioning.api.UserFeedbackInfo
import com.mapxus.positioning.api.positioning.PositioningMode
import com.mapxus.positioning.api.positioning.PositioningState
import com.mapxus.positioning.api.positioning.UserMode

data class PositioningActivityUiState(
    val currentPositioningState: PositioningState? = null,
    val currentLocateSiteName: String = "",
    val userMode: UserMode = UserMode.PEDESTRIAN,
    val isAlwaysFollow: Boolean = false,
    val isSettingCustomLocation: Boolean = false,
    val currentPositioningMode: PositioningMode? = null,
    val feedbackMessages: List<UserFeedbackInfo> = emptyList(),
) {
    val isShowLoadingDialog: Boolean
        get() = currentPositioningState == PositioningState.INITIALIZING
}
