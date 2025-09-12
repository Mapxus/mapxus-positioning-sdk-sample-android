package com.mapxus.positioning.sample_app.page.positioning.mapview

import com.mapxus.map.mapxusmap.positioning.IndoorLocationProvider

private const val TAG = "MapxusPositioningProvider"

/**
 * Mapxus positioning provider
 *
 * core sdk 控制显示蓝点
 *
 * @constructor Create empty Mapxus positioning provider
 */
class MapxusPositioningProvider() : IndoorLocationProvider() {

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
}
