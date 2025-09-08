package com.mapxus.positioning.sample_app.page.positioning.mapview

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mapxus.map.mapxusmap.api.map.MapViewProvider
import com.mapxus.map.mapxusmap.api.map.MapxusMap
import com.mapxus.map.mapxusmap.api.map.MapxusMap.OnIndoorPoiClickListener
import com.mapxus.map.mapxusmap.api.map.MapxusMapZoomMode
import com.mapxus.map.mapxusmap.api.map.model.BuildingBorderStyle
import com.mapxus.map.mapxusmap.api.map.model.LatLng
import com.mapxus.map.mapxusmap.api.map.model.MapxusMapOptions
import com.mapxus.map.mapxusmap.api.map.model.MapxusPointAnnotationOptions
import com.mapxus.map.mapxusmap.impl.MapLibreMapViewProvider
import com.mapxus.positioning.api.positioning.MapxusFloor
import com.mapxus.positioning.api.positioning.MapxusLocation
import com.mapxus.positioning.sample_app.R
import com.mapxus.positioning.sample_app.utils.getName
import com.mapxus.positioning.sample_app.utils.logD
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.utils.BitmapUtils

private const val TAG = "MapxusMap"

@Composable
fun MapxusMap(
    modifier: Modifier = Modifier,
    isSettingCustomLocation: () -> Boolean,
    mapOptions: MapLibreMapOptions? = null,
    mapxusMapOptions: MapxusMapOptions = MapxusMapOptions(),
    onSetCustomLocation: (MapxusLocation) -> Unit = {},
    onGetMap: (MapLibreMap) -> Unit = {},
    onGetMapxusMap: (MapxusMap) -> Unit = {},
    onFloorChanged: (String) -> Unit = {},
) {

    val context = LocalContext.current
    val images = remember {
        prepareImages(context)
    }
    val mapView = remember { MapView(context, mapOptions) }
    val mapViewProvider = remember {
        MapLibreMapViewProvider(
            context,
            mapView,
            mapxusMapOptions
        )
    }

    var mapxusMap by remember { mutableStateOf<MapxusMap?>(null) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }

    MapClickEvent(
        isSettingCustomLocation,
        mapxusMap,
        onSetCustomLocation,
    )

    MapLifecycle(mapView)

    mapxusMap?.let {
        MapxusLifeCycle(it, mapViewProvider)
    }

    AndroidView(
        factory = {
            mapView
        },
        modifier = modifier
    )

    LaunchedEffect(Unit) {
        mapView.getMapAsync {
            it.getStyle { style ->
                style.addImages(images, true)
            }
            map = it
            onGetMap(it)
        }

        mapViewProvider.getMapxusMapAsync {
            mapxusMap = it
            it.isMaskNonSelectedSite(true)
            mapViewProvider.setBuildingAutoSwitch(false)
            it.mapxusUiSettings.setSelectedBuildingBorderStyle(
                BuildingBorderStyle(
                    lineOpacity = Expression.literal(0f)
                )
            )
            it.addOnFloorChangedListener { venue, indoorBuilding, floor ->
                onFloorChanged(floor?.id ?: "")
            }
            onGetMapxusMap(it)
        }
    }
}

@Composable
private fun MapClickEvent(
    isSettingCustomLocation: () -> Boolean,
    mapxusMap: MapxusMap?,
    onSetCustomLocation: (MapxusLocation) -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(mapxusMap) {
        if (mapxusMap != null) {

            mapxusMap.addOnMapClickedListener(
                generateMapxusMapClickListener(
                    context,
                    mapxusMap,
                    isSettingCustomLocation,
                    onSetCustomLocation,
                )
            )
            mapxusMap.addOnIndoorPoiClickListener(
                generateMapxusIndoorPoiClickListener(
                    context,
                    mapxusMap,
                    isSettingCustomLocation,
                    onSetCustomLocation,
                )
            )
        }
    }
}

private fun generateMapxusIndoorPoiClickListener(
    context: Context,
    mapxusMap: MapxusMap,
    isSettingCustomLocation: () -> Boolean,
    onSetCustomLocation: (MapxusLocation) -> Unit = {},
): OnIndoorPoiClickListener {
    return OnIndoorPoiClickListener {
        if (isSettingCustomLocation()) {
            clickOnMap(
                mapxusMap = mapxusMap,
                venueId = mapxusMap.buildings[it.buildingId]?.venueId,
                buildingId = it.buildingId,
                floorName = it.floorName ?: "NULL",
                floorId = it.floor,
                ordinal = mapxusMap.buildings[it.buildingId]?.floors?.find { floor -> floor.id == it.floor }?.ordinal
                    ?: 0,
                latLng = LatLng(it.latitude, it.longitude),
                context = context,
                onSetCustomLocation = onSetCustomLocation
            )
        }
    }
}

private fun generateMapxusMapClickListener(
    context: Context,
    mapxusMap: MapxusMap,
    isSettingCustomLocation: () -> Boolean,
    onSetCustomLocation: (MapxusLocation) -> Unit = {},
): MapxusMap.OnMapClickedListener {
    return MapxusMap.OnMapClickedListener { latLng, mapxusSite ->
        if (isSettingCustomLocation()) {
            mapxusSite.indoorBuilding?.let {
                mapxusMap.selectBuildingById(
                    it.buildingId,
                    MapxusMapZoomMode.ZoomDisable,
                    null
                )
            }
            clickOnMap(
                mapxusMap = mapxusMap,
                venueId = mapxusSite.venue?.id,
                buildingId = mapxusSite.indoorBuilding?.buildingId,
                floorName = mapxusSite.floor?.code ?: "",
                floorId = mapxusSite.floor?.id ?: "",
                ordinal = mapxusSite.floor?.ordinal ?: 0,
                latLng = latLng,
                context = context,
                onSetCustomLocation = onSetCustomLocation
            )
        }

    }
}

private fun clickOnMap(
    mapxusMap: MapxusMap,
    venueId: String?,
    buildingId: String?,
    floorName: String,
    floorId: String?,
    ordinal: Int,
    latLng: LatLng,
    context: Context,
    onSetCustomLocation: (MapxusLocation) -> Unit = {},
) {
    mapxusMap.removeMapxusPointAnnotations()

    val message =
        if (venueId == null) "" else ", Is this an indoor point or an outdoor point ?"
    val positiveText = if (venueId == null) "Ok" else "Outdoor"
    val negativeText = if (venueId == null) "Cancel" else "Indoor"

    val dialog = MaterialAlertDialogBuilder(context).setTitle("Custom Location").setMessage(
        "You have tap at coordinate ${latLng.latitude} , ${latLng.longitude} , $floorName ,${
            mapxusMap.buildings[buildingId]?.buildingNameMap?.getName()
        } $message"
    ).setPositiveButton(positiveText) { _, _ ->
        //do nothing
        onSetCustomLocation(
            setUpCustomLocation(
                mapxusMap, null, null, null, latLng
            )
        )
    }.setCancelable(false)

    if (venueId != null) {
        dialog.setNegativeButton(negativeText) { _, _ ->
            val mapxusFloor = MapxusFloor(
                floorId!!,
                ordinal,
                floorName,
                if (buildingId == null) MapxusFloor.Type.SHARED_FLOOR else MapxusFloor.Type.FLOOR
            )
            onSetCustomLocation(
                setUpCustomLocation(
                    mapxusMap, venueId, buildingId, mapxusFloor, latLng
                )
            )

        }
    }

    dialog.create().show()

}

private fun setUpCustomLocation(
    mapxusMap: MapxusMap,
    venueId: String?,
    buildingId: String?,
    mapxusFloor: MapxusFloor?,
    latLng: LatLng
): MapxusLocation {
    mapxusMap.addMapxusPointAnnotation(
        MapxusPointAnnotationOptions()
            .setFloorId(mapxusFloor?.id)
            .setIcon(
                if (mapxusFloor == null) R.drawable.blue_marker else com.mapxus.map.mapxusmap.R.drawable.red_marker
            ).setIconSize(
                if (mapxusFloor == null) 1.5f else 1f
            ).setPosition(latLng)
    )

    return MapxusLocation(
        venueId,
        buildingId,
        mapxusFloor,
        com.mapxus.positioning.api.positioning.LatLng.fromLatLng(
            latLng.latitude,
            latLng.longitude
        ),
        4f,
        System.currentTimeMillis()
    )
}

@Composable
private fun MapLifecycle(mapView: MapView) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val previousState = remember { mutableStateOf(Lifecycle.Event.ON_CREATE) }

    DisposableEffect(context, lifecycle, mapView) {
        val mapLifecycleObserver = mapView.lifecycleObserver(previousState)
        val callbacks = mapView.componentCallbacks()

        lifecycle.addObserver(mapLifecycleObserver)
        context.registerComponentCallbacks(callbacks)
        onDispose {
            lifecycle.removeObserver(mapLifecycleObserver)
            context.unregisterComponentCallbacks(callbacks)
        }
    }
    DisposableEffect(mapView) {
        onDispose {
            Log.i(TAG, "MapView onDispose")
            mapView.onDestroy()
            mapView.removeAllViews()
        }
    }
}


@Composable
private fun MapxusLifeCycle(mapxusMap: MapxusMap, mapViewProvider: MapViewProvider) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(context, lifecycle, mapxusMap) {
        val mapLifecycleObserver = mapxusMap.lifecycleObserver()
        lifecycle.addObserver(mapLifecycleObserver)
        onDispose {
            lifecycle.removeObserver(mapLifecycleObserver)
        }
    }

    DisposableEffect(mapViewProvider) {
        onDispose {
            Log.i(TAG, "MapViewProvider onDestroy")
            mapViewProvider.onDestroy()
        }
    }

}

private fun MapxusMap.lifecycleObserver(): LifecycleEventObserver =
    LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                Log.i(TAG, "MapxusMap onResume")
                this.onResume()
            }

            Lifecycle.Event.ON_PAUSE -> {
                Log.i(TAG, "MapxusMap onPause")
                this.onPause()
            }

            else -> {
                //ignore
            }
        }
    }

private fun MapView.lifecycleObserver(previousState: MutableState<Lifecycle.Event>): LifecycleEventObserver =
    LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                Log.i(TAG, "MapView onCreate")
                if (previousState.value != Lifecycle.Event.ON_STOP) {
                    this.onCreate(Bundle())
                }
            }

            Lifecycle.Event.ON_START -> {
                Log.i(TAG, "MapView onStart")
                this.onStart()
            }

            Lifecycle.Event.ON_RESUME -> {
                Log.i(TAG, "MapView onResume")
                this.onResume()
            }

            Lifecycle.Event.ON_PAUSE -> {
                Log.i(TAG, "MapView onPause")
                this.onPause()
            }

            Lifecycle.Event.ON_STOP -> {
                Log.i(TAG, "MapView onStop")
                this.onStop()
            }

            Lifecycle.Event.ON_DESTROY -> {
                //handled in onDispose
            }

            else -> {
                //ignore
            }
        }
        previousState.value = event
    }

private fun prepareImages(context: Context) = hashMapOf<String, Bitmap>().apply {
    val bearingIcon = BitmapUtils.getBitmapFromDrawable(
        AppCompatResources.getDrawable(
            context, R.drawable.bearing_icon
        )
    )
    put(POSITION_ARROW_ICON_ID, bearingIcon!!)
}

private fun MapView.componentCallbacks(): ComponentCallbacks =
    object : ComponentCallbacks {
        override fun onConfigurationChanged(config: Configuration) {
            //ignore
        }

        override fun onLowMemory() {
            Log.i(TAG, "MapView onLowMemory")
            this@componentCallbacks.onLowMemory()
        }
    }

fun headingMap(bearing: Double, map: MapLibreMap) {
    map.moveCamera(CameraUpdateFactory.bearingTo(bearing))
}

fun followUserLocation(
    mapxusmap: MapxusMap,
    map: MapLibreMap,
    isAlwaysFollow: Boolean,
    mapxusLocation: MapxusLocation
) {
    val currentMapFloor = mapxusmap.selectedFloor
    "followUserLocation  selectedVenueId:${mapxusmap.selectedVenueId} selectedBuildingId:${mapxusmap.selectedBuildingId} selectedFloor:$currentMapFloor isAlwaysFollow:$isAlwaysFollow mapxusLocation:$mapxusLocation ".logD(
    )
    when {
        isAlwaysFollow -> {
            followUserCenter(
                mapxusmap,
                map,
                mapxusLocation,
                currentMapFloor?.id != mapxusLocation.mapxusFloor?.id
            )
        }

        mapxusLocation.venueId == null -> followUserCenter(
            mapxusmap,
            map, mapxusLocation, false
        )

        mapxusLocation.buildingId != null && mapxusmap.selectedBuildingId != mapxusLocation.buildingId -> followUserCenter(
            mapxusmap,
            map,
            mapxusLocation,
            true
        )


        mapxusLocation.venueId != null && mapxusmap.selectedVenueId != mapxusLocation.venueId -> followUserCenter(
            mapxusmap,
            map,
            mapxusLocation,
            true
        )

        currentMapFloor?.id != mapxusLocation.mapxusFloor?.id -> {
            mapxusLocation.mapxusFloor?.let {
                followUserFloor(
                    mapxusmap,
                    it
                )
            }
        }
    }
}

private fun followUserFloor(
    mapxusmap: MapxusMap,
    mapxusFloor: MapxusFloor
) {
    val floorId = mapxusFloor.id
    if (mapxusFloor.type == MapxusFloor.Type.FLOOR) {
        mapxusmap.selectFloorById(
            floorId, MapxusMapZoomMode.ZoomDisable, null
        )
    } else {
        mapxusmap.selectSharedFloorById(
            floorId, MapxusMapZoomMode.ZoomDisable, null
        )
    }
}

private fun followUserCenter(
    mapxusmap: MapxusMap,
    map: MapLibreMap,
    mapxusLocation: MapxusLocation,
    isFollowUserFloor: Boolean
) {
    map.moveCamera(
        CameraUpdateFactory.newLatLngZoom(
            org.maplibre.android.geometry.LatLng(
                mapxusLocation.latitude, mapxusLocation.longitude
            ),
            map.cameraPosition.zoom.takeIf { it > 5 } ?: 18.0
        ), object : MapLibreMap.CancelableCallback {
            override fun onCancel() {
            }

            override fun onFinish() {
                if (isFollowUserFloor) {
                    mapxusLocation.mapxusFloor?.let {
                        followUserFloor(mapxusmap, it)
                    }
                }
            }
        }
    )
}