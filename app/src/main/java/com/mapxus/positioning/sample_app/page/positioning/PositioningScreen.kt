package com.mapxus.positioning.sample_app.page.positioning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.mapxus.map.mapxusmap.api.map.FollowUserMode
import com.mapxus.map.mapxusmap.api.map.MapxusMap
import com.mapxus.map.mapxusmap.api.map.MapxusMapZoomMode
import com.mapxus.positioning.sample_app.page.positioning.mapview.MapxusMap
import com.mapxus.positioning.sample_app.page.positioning.ui.BottomContainer
import com.mapxus.positioning.sample_app.page.positioning.ui.LeftContainer
import com.mapxus.positioning.sample_app.ui.component.LifecycleEffect
import com.mapxus.positioning.sample_app.ui.component.LoadingDialog
import com.mapxus.positioning.sample_app.ui.component.MapxusToastContainer
import com.mapxus.positioning.sample_app.utils.showToast
import com.mapxus.positioning.sample_app.utils.toMapxusToastData
import org.maplibre.android.maps.MapLibreMap

private const val TAG = "PositioningScreen"

@Composable
fun PositioningScreen(
    viewModel: PositioningActivityViewModel,
) {
    val context = LocalContext.current

    val positioningActivityUiState by viewModel.positioningActivityUiState.collectAsState()
    var mapxusMap by remember { mutableStateOf<MapxusMap?>(null) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }

    LaunchedEffect(positioningActivityUiState.currentLocation) {
        positioningActivityUiState.currentLocation?.mapxusFloor?.id?.let { floorId ->
            if (mapxusMap?.followUserMode == FollowUserMode.NONE) {
                mapxusMap?.selectFloorById(floorId, MapxusMapZoomMode.ZoomDisable, null)
            }
        }
    }

    LifecycleEffect(
        onPause = {
            //core sdk 方法 ，设置定位蓝点是否显示
            mapxusMap?.setLocationEnabled(false)
            viewModel.stop()
        }
    )

    LoadingDialog(positioningActivityUiState.isShowLoadingDialog)
    //background
    MapxusMap(
        isSettingCustomLocation = { viewModel.isSettingCustomLocation() },
        onSetCustomLocation = {
            viewModel.customLocation = it
        },
        onGetMapxusMap = {
            mapxusMap = it
            //core sdk 方法 ，将对象设置进map中
            it.setLocationProvider(viewModel.mapxusPositioningProvider)
        },
        onGetMap = {
            map = it
        },
    )

    if (mapxusMap != null) {
        //foreground
        ForegroundView(
            viewModel = viewModel,
            onClickedCustomLocation = {
                if (!it) {
                    mapxusMap?.removeMapxusPointAnnotations()
                    viewModel.customLocation = null
                }
                viewModel.isSettingCustomLocation(it)
            },
            onClickedStartPositionButton = {
                mapxusMap?.removeMapxusPointAnnotations()
                //core sdk 方法 ，设置定位蓝点是否显示
                mapxusMap?.setLocationEnabled(true)
                //core sdk 方法 ，设置监听follow user mode 事件
                mapxusMap?.addOnFollowUserModeChangedListener(viewModel.followUserModeChangedListener)
                viewModel.startPositioning()
            },
            onClickedStartCustomLocationPositioningButton = {
                if (viewModel.customLocation != null) {
                    viewModel.isSettingCustomLocation(false)
                    mapxusMap?.removeMapxusPointAnnotations()
                    //core sdk 方法 ，设置定位蓝点是否显示
                    mapxusMap?.setLocationEnabled(true)
                    //core sdk 方法 ，设置监听follow user mode 事件
                    mapxusMap?.addOnFollowUserModeChangedListener(viewModel.followUserModeChangedListener)
                    viewModel.startPositioning()
                    true
                } else {
                    context.showToast("Please provide customized location then start.")
                    false
                }
            },
            onMapFollowButtonClick = {
                val result = when (mapxusMap?.followUserMode) {
                    FollowUserMode.FOLLOW_USER -> FollowUserMode.FOLLOW_USER_AND_HEADING
                    FollowUserMode.FOLLOW_USER_AND_HEADING -> FollowUserMode.NONE
                    else -> FollowUserMode.FOLLOW_USER
                }
                mapxusMap?.followUserMode = result
            },
            onSearchResultItemClick = { buildingId ->
                mapxusMap?.selectBuildingById(buildingId)
            },
            onClickedStopPositionButton = {
                mapxusMap?.removeMapxusPointAnnotations()
                //core sdk 方法 ，设置定位蓝点是否显示
                mapxusMap?.setLocationEnabled(false)
                viewModel.stop()
            },
            onClickedRefreshLocationButton = {
                val result = viewModel.refreshLocation()
                context.showToast("Request location refresh ${if (result) "succeed" else "failed"}")
            },
            onPositioningModeButtonClick = {
                viewModel.togglePositioningMode()
            }
        )
    }
}

@Composable
private fun ForegroundView(
    viewModel: PositioningActivityViewModel,
    onClickedCustomLocation: (Boolean) -> Unit,
    onClickedStartPositionButton: () -> Unit,
    onClickedStopPositionButton: () -> Unit,
    onClickedRefreshLocationButton: () -> Unit,
    onClickedStartCustomLocationPositioningButton: () -> Boolean,
    onMapFollowButtonClick: () -> Unit,
    onSearchResultItemClick: (String) -> Unit,
    onPositioningModeButtonClick: () -> Unit
) {
    val positioningActivityUiState by viewModel.positioningActivityUiState.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            //left
            LeftContainer(
                positioningState = positioningActivityUiState.currentPositioningState,
                userMode = positioningActivityUiState.userMode,
                currentLocateSiteName = positioningActivityUiState.currentLocateSiteName,
            )
        }

        //bottom
        BottomContainer(
            modifier = Modifier.align(Alignment.BottomCenter),
            followUserMode = positioningActivityUiState.followUserMode,
            userMode = positioningActivityUiState.userMode,
            positioningState = positioningActivityUiState.currentPositioningState,
            onPositioningModeButtonClick = onPositioningModeButtonClick,
            onStartPositionButtonClick = onClickedStartPositionButton,
            onStartCustomLocationPositioningButtonClick = onClickedStartCustomLocationPositioningButton,
            onStopPositioningButtonClick = onClickedStopPositionButton,
            onRefreshLocationButtonClick = onClickedRefreshLocationButton,
            onCustomLocationButtonClick = {
                onClickedCustomLocation(true)
            },
            onCancelCustomLocationButtonClick = {
                onClickedCustomLocation(false)
            },
            onSearchResultItemClick = onSearchResultItemClick,
            onMapFollowButtonClick = onMapFollowButtonClick,
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
        ) {
            MapxusToastContainer(
                data = positioningActivityUiState.feedbackMessages.map {
                    it.toMapxusToastData()
                }
            )
        }
    }
}