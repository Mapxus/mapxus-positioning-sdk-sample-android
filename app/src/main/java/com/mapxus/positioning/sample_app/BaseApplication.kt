package com.mapxus.positioning.sample_app

import android.app.Application
import com.mapxus.map.auth.CognitoContext
import com.mapxus.map.mapxusmap.api.map.MapxusMapContext

class BaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MapxusMapContext.init(applicationContext)
        CognitoContext.initCognito(applicationContext)
    }
}

