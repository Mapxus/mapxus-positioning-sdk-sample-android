package com.mapxus.positioning.sample_app.page.positioning.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.mapxus.positioning.api.positioning.PositioningState
import com.mapxus.positioning.api.positioning.UserMode
import com.mapxus.positioning.sample_app.ui.component.DebugTextRow

/**
 * Created by Edison on 2024/2/21.
 * Describe:
 */
@Composable
fun LeftContainer(
    positioningState: PositioningState?,
    userMode: UserMode,
    currentLocateSiteName: String,
) {
    Column(
        horizontalAlignment = Alignment.Start,
    ) {
        DebugTextRow(
            textTitle = "Positioning Client状态 -> ",
            textValue = positioningState?.toString() ?: ""
        )

        DebugTextRow(
            textTitle = "当前用户模式 -> ",
            textValue = userMode.name
        )

        DebugTextRow(
            textTitle = "当前(楼层名,建筑名) -> ",
            textValue = currentLocateSiteName
        )
    }
}