package com.mapxus.positioning.sample_app.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mapxus.positioning.sample_app.utils.AppSettingDataStoreKeys.APP_SETTING

/**
 * Created by Edison on 2023/11/29.
 * Describe:
 */
object AppSettingDataStoreKeys {
    const val APP_SETTING = "app_setting"
    val POSITIONING_MODE = stringPreferencesKey("positioning_mode")
    val IS_FOLLOW_MAP = booleanPreferencesKey("is_follow_map")
}

val Context.appSettingDataStore: DataStore<Preferences> by preferencesDataStore(
    name = APP_SETTING,
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, APP_SETTING))
    }
)
