package com.mapxus.positioning.sample_app.page.positioning.mapview

import android.graphics.Color
import androidx.core.graphics.toColorInt
import com.mapxus.positioning.api.positioning.MapxusLocation
import com.mapxus.positioning.sample_app.utils.keepOne
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.color
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

/**
 * Created by Edison on 2022/11/24.
 * Describe:
 */

const val POSITION_ID = "position"
const val POSITION_ARROW_ID = "position_arrow"
const val POSITION_DIRECTION_ID = "position_direction"
const val POSITION_ACCURACY_ID = "position_accuracy_id"

const val POSITION_ARROW_ICON_ID = "position_arrow_icon"
const val PROPERTY_BEARING = "position_arrow_bearing"

const val PROPERTY_LOCATION_FLOOR_ID = "sample-property-location-floor-id"

const val PROPERTY_ACCURACY_RADIUS = "bts-property-accuracy-radius"

val ACCURACY_BLUE: Int = "#3E91F7".toColorInt()

class LayerProvider(
    private val style: Style
) {

    private val positionLayer by lazy {
        CircleLayer(POSITION_ID, POSITION_ID).withProperties(
            PropertyFactory.circleColor(Color.RED),
            PropertyFactory.circleRadius(4f),
        )
    }

    private val positionArrowLayer by lazy {
        SymbolLayer(POSITION_ARROW_ID, POSITION_ARROW_ID).withProperties(
            PropertyFactory.iconImage(POSITION_ARROW_ICON_ID),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true),
            PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
            PropertyFactory.iconRotate(get(PROPERTY_BEARING))
        )
    }

    private val accuracyLayer by lazy {
        CircleLayer(POSITION_ACCURACY_ID, POSITION_ACCURACY_ID)
            .withProperties(
                PropertyFactory.circleRadius(get(PROPERTY_ACCURACY_RADIUS)),
                PropertyFactory.circleColor(color(ACCURACY_BLUE)),
                PropertyFactory.circleOpacity(0.15f),
                PropertyFactory.circleStrokeColor(color(ACCURACY_BLUE)),
                PropertyFactory.circlePitchAlignment(Property.CIRCLE_PITCH_ALIGNMENT_MAP)
            )
    }

    private var isLayerAdded: Boolean = false
    fun generateDebugLayer() {
        if (!isLayerAdded) {
            isLayerAdded = true
            style.addLayer(positionLayer)
            style.addLayer(accuracyLayer)
            style.addLayer(positionArrowLayer)
        }
    }

    fun clearMap() {
        updateSource(FeatureCollection.fromFeatures(emptyList()), POSITION_ID)
        updateSource(FeatureCollection.fromFeatures(emptyList()), POSITION_ARROW_ID)
        updateSource(FeatureCollection.fromFeatures(emptyList()), POSITION_DIRECTION_ID)
        updateSource(FeatureCollection.fromFeatures(emptyList()), POSITION_ACCURACY_ID)

        style.removeLayer(positionLayer)
        style.removeLayer(positionArrowLayer)
        style.removeLayer(accuracyLayer)

        style.removeSource(POSITION_ID)
        style.removeSource(POSITION_ARROW_ID)
        style.removeSource(POSITION_DIRECTION_ID)
        style.removeSource(POSITION_ACCURACY_ID)
        isLayerAdded = false
    }

    fun updateSource(data: FeatureCollection, sourceId: String) {
        with(style.getSourceAs<GeoJsonSource>(sourceId)) {
            this?.setGeoJson(data) ?: style.addSource(GeoJsonSource(sourceId, data))
        }
    }

    fun updateAccuracyRadiusSource(
        lastCameraZoom: Double,
        isNewLocation: Boolean,
        mapxusLocation: MapxusLocation,
        map: MapLibreMap
    ): Double {
        val cameraZoom = map.cameraPosition.zoom.keepOne().toDouble()
        if (cameraZoom != lastCameraZoom || isNewLocation) {
            val feature =
                generateFeatureFormMapxusLocation(
                    mapxusLocation
                ).also {
                    it.addAccuracyRadius(
                        map,
                        mapxusLocation.latitude,
                        mapxusLocation.accuracy
                    )
                }

            updateSource(
                FeatureCollection.fromFeature(feature),
                POSITION_ACCURACY_ID
            )
        }
        return cameraZoom
    }

    companion object {

        fun generateFeatureFormMapxusLocation(mapxusLocation: MapxusLocation): Feature {
            return Feature.fromGeometry(
                Point.fromLngLat(
                    mapxusLocation.longitude,
                    mapxusLocation.latitude
                )
            )
                .also {
                    it.addIndoorInfo(mapxusLocation.mapxusFloor?.id ?: "")
                }
        }
    }
}

fun Feature.addAccuracyRadius(map: MapLibreMap, latitude: Double, accuracy: Float): Feature {
    val metersPerPixel =
        map.projection.getMetersPerPixelAtLatitude(latitude)
    val zoomLevelRadius = (accuracy / metersPerPixel).toFloat()

    addNumberProperty(
        PROPERTY_ACCURACY_RADIUS,
        zoomLevelRadius
    )
    return this
}

fun Feature.addIndoorInfo(floorId: String): Feature {
    addStringProperty(
        PROPERTY_LOCATION_FLOOR_ID, floorId
    )
    return this
}