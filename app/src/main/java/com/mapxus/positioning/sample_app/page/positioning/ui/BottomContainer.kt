package com.mapxus.positioning.sample_app.page.positioning.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapxus.positioning.api.positioning.PositioningState
import com.mapxus.positioning.api.positioning.UserMode
import com.mapxus.positioning.sample_app.ui.component.TextButtonSmall
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Created by Edison on 2024/2/21.
 * Describe:
 */
@Preview
@Composable
fun BottomContainer(
    modifier: Modifier = Modifier,
    viewModel: BottomContainerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    isFollowMap: Boolean = true,
    positioningState: PositioningState? = PositioningState.INITIALIZING,
    userMode: UserMode = UserMode.WHEELCHAIR,
    onPositioningModeButtonClick: () -> Unit = {},
    onStartPositionButtonClick: () -> Unit = {},
    onStartCustomLocationPositioningButtonClick: () -> Boolean = { false },
    onStopPositioningButtonClick: () -> Unit = {},
    onRefreshLocationButtonClick: () -> Unit = {},
    onCustomLocationButtonClick: () -> Unit = {},
    onCancelCustomLocationButtonClick: () -> Unit = {},
    onSearchResultItemClick: (String) -> Unit = {},
    onMapFollowButtonClick: () -> Unit = {},
) {
    val bottomContainerUiState by viewModel.bottomContainerUiState.collectAsState()
    val positioningModeButtonText by remember(userMode) {
        mutableStateOf("当前模式是: $userMode")
    }

    LaunchedEffect(positioningState) {
        if (positioningState == PositioningState.STOPPED) {
            viewModel.stop()
        }
    }

    Box(modifier = modifier) {
        Column {
            if (!bottomContainerUiState.isStarted) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButtonSmall(positioningModeButtonText) {
                        onPositioningModeButtonClick()
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
            ) {

                if (bottomContainerUiState.isStarted) {
                    TextButtonSmall("停止定位") {
                        viewModel.stop()
                        onStopPositioningButtonClick()
                    }
                }

                if (bottomContainerUiState.isStarted) {
                    TextButtonSmall("刷新定位", enabled = bottomContainerUiState.refreshEnable) {
                        viewModel.refreshButtonClicked()
                        onRefreshLocationButtonClick()
                    }
                }

                if (bottomContainerUiState.isStarted) {
                    TextButtonSmall(
                        if (isFollowMap) {
                            "当前是地图跟随模式"
                        } else {
                            "当前是自由模式"
                        },
                        onClick = onMapFollowButtonClick
                    )
                }

                if (!bottomContainerUiState.isSettingCustomLocation && !bottomContainerUiState.isStarted) {
                    TextButtonSmall("开始定位") {
                        onStartPositionButtonClick()
                        viewModel.start()
                    }
                    TextButtonSmall("自定义定位") {
                        viewModel.isSettingCustomLocation(true)
                        onCustomLocationButtonClick()
                    }
                }

                if (bottomContainerUiState.isSettingCustomLocation && !bottomContainerUiState.isStarted) {
                    TextButtonSmall("开始自定义定位") {
                        val result = onStartCustomLocationPositioningButtonClick()
                        if (result) {
                            viewModel.isSettingCustomLocation(false)
                            onCancelCustomLocationButtonClick()
                            viewModel.start()
                        }
                    }
                    TextButtonSmall("取消自定义定位") {
                        viewModel.isSettingCustomLocation(false)
                        onCancelCustomLocationButtonClick()
                    }
                    TextButtonSmall("搜索建筑") {
                        viewModel.isSearchingBuilding(true)
                    }
                }
            }
        }

        if (bottomContainerUiState.isSearchingBuilding) {
            SearchContainer(
                onCancelButtonClick = {
                    viewModel.isSearchingBuilding(false)
                },
                onSearchResultItemClick = {
                    viewModel.isSearchingBuilding(false)
                    onSearchResultItemClick(it)
                }
            )
        }
    }
}

class BottomContainerViewModel : ViewModel() {
    private val _bottomContainerUiState: MutableStateFlow<BottomContainerUiState> =
        MutableStateFlow(BottomContainerUiState())
    val bottomContainerUiState = _bottomContainerUiState.asStateFlow()

    fun start() {
        _bottomContainerUiState.update {
            it.copy(
                isStarted = true,
            )
        }
    }

    fun stop() {
        _bottomContainerUiState.update {
            it.copy(
                isStarted = false,
            )
        }
    }

    fun refreshButtonClicked() {
        _bottomContainerUiState.update {
            it.copy(
                refreshEnable = false,
            )
        }

        viewModelScope.launch {
            delay(3_000)
            _bottomContainerUiState.update {
                it.copy(
                    refreshEnable = true,
                )
            }
        }
    }

    fun isSettingCustomLocation(value: Boolean) {
        _bottomContainerUiState.update {
            it.copy(
                isSettingCustomLocation = value
            )
        }
    }

    fun isSearchingBuilding(value: Boolean) {
        _bottomContainerUiState.update {
            it.copy(
                isSearchingBuilding = value
            )
        }
    }
}

data class BottomContainerUiState(
    val isSearchingBuilding: Boolean = false,
    val isSettingCustomLocation: Boolean = false,
    val isStarted: Boolean = false,
    val refreshEnable: Boolean = true,
)
