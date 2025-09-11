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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.mapxus.map.mapxusmap.api.map.MapxusMap
import com.mapxus.positioning.api.positioning.PositioningState
import com.mapxus.positioning.sample_app.page.positioning.mapview.LayerProvider
import com.mapxus.positioning.sample_app.page.positioning.mapview.MapxusMap
import com.mapxus.positioning.sample_app.page.positioning.mapview.MapxusPositioningProvider
import com.mapxus.positioning.sample_app.page.positioning.mapview.followUserLocation
import com.mapxus.positioning.sample_app.page.positioning.mapview.headingMap
import com.mapxus.positioning.sample_app.page.positioning.model.PositioningActivityEvent
import com.mapxus.positioning.sample_app.page.positioning.ui.BottomContainer
import com.mapxus.positioning.sample_app.page.positioning.ui.LeftContainer
import com.mapxus.positioning.sample_app.ui.component.LifecycleEffect
import com.mapxus.positioning.sample_app.ui.component.LoadingDialog
import com.mapxus.positioning.sample_app.ui.component.MapxusToastContainer
import com.mapxus.positioning.sample_app.utils.logI
import com.mapxus.positioning.sample_app.utils.showToast
import com.mapxus.positioning.sample_app.utils.toMapxusToastData
import kotlinx.coroutines.launch
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
    var layerProvider by remember { mutableStateOf<LayerProvider?>(null) }
    val mapxusPositioningProvider: MapxusPositioningProvider = remember {
        MapxusPositioningProvider()
    }

    LifecycleEffect(
        onPause = {
            //core sdk 方法 ，设置定位蓝点是否显示
            mapxusMap?.setLocationEnabled(false)
            viewModel.stop(mapxusPositioningProvider)
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
            it.setLocationProvider(mapxusPositioningProvider)
        },
        onGetMap = {
            map = it
            it.getStyle { style ->
                it.addOnCameraIdleListener {
                    viewModel.updateAccuracyRadius(false)
                }
                layerProvider = LayerProvider(
                    style = style
                )
            }
        },
    )

    if (layerProvider != null) {
        HandleUiEvent(
            viewModel = viewModel,
            layerProvider = layerProvider!!,
            map = map!!,
            mapxusmap = mapxusMap!!,
        )
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
                layerProvider?.generateDebugLayer()
                mapxusMap?.setLocationEnabled(true)
                viewModel.startPositioning(mapxusPositioningProvider)
            },
            onClickedStartCustomLocationPositioningButton = {
                if (viewModel.customLocation != null) {
                    viewModel.isSettingCustomLocation(false)
                    mapxusMap?.removeMapxusPointAnnotations()
                    layerProvider?.clearMap()
                    mapxusMap?.setLocationEnabled(true)
                    viewModel.startPositioning(
                        mapxusPositioningProvider
                    )
                    layerProvider?.generateDebugLayer()
                    true
                } else {
                    context.showToast("Please provide customized location then start.")
                    false
                }
            },
            onMapFollowButtonClick = {
                viewModel.switchAlwaysFollowMap()
            },
            onSearchResultItemClick = { buildingId ->
                mapxusMap?.selectBuildingById(buildingId)
            },
            onClickedStopPositionButton = {
                mapxusMap?.removeMapxusPointAnnotations()
                layerProvider?.clearMap()
                mapxusMap?.setLocationEnabled(false)
                viewModel.stop(mapxusPositioningProvider)
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
private fun HandleUiEvent(
    viewModel: PositioningActivityViewModel,
    layerProvider: LayerProvider,
    map: MapLibreMap,
    mapxusmap: MapxusMap,
) {

    var lastCameraZoom = remember {
        0.0
    }

    var lastLocationIsOutDoor = remember {
        false
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
            launch {
                viewModel.positioningActivityEvent.collect { event ->
                    when (event) {
                        is PositioningActivityEvent.UpdateAccuracyRadiusEvent -> {
                            lastCameraZoom = layerProvider.updateAccuracyRadiusSource(
                                lastCameraZoom, event.isNewLocation, event.mapxusLocation, map
                            )
                        }

                        is PositioningActivityEvent.UpdateSourceEvent -> {
                            layerProvider.updateSource(
                                event.featureCollection,
                                event.layerId
                            )
                        }

                        is PositioningActivityEvent.LocationEvent -> {
                            val location = event.mapxusLocation
                            "receive currentLocation  $location".logI(TAG)
                            if (lastLocationIsOutDoor && location.venueId == null) {
                                "keep outdoor , not following".logI(TAG)
                                return@collect
                            }
                            followUserLocation(
                                mapxusmap,
                                map,
                                event.isAlwaysFollowMap,
                                location
                            )
                            lastLocationIsOutDoor = location.venueId == null
                        }

                        is PositioningActivityEvent.BearingEvent -> {
                            headingMap(event.bearing, map)
                        }

                        is PositioningActivityEvent.PositioningStateChangeEvent -> {
                            if (event.positioningState == PositioningState.STOPPED) {
                                mapxusmap.removeMapxusPointAnnotations()
                                layerProvider.clearMap()
                                viewModel.clearCache()
                            }
                        }
                    }
                }
            }
        }
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
            isFollowMap = positioningActivityUiState.isAlwaysFollow,
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