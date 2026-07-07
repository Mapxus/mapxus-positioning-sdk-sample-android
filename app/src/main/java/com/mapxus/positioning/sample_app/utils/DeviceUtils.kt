package com.mapxus.common.ui.lib.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings

/**
 * Created by Edison on 2022/12/9.
 * Describe:
 */
object DeviceUtils {
    /**
     * Whether wifi throttling is enabled
     * @param context
     * @return
     */
    @SuppressLint("MissingPermission")
    fun isWifiThrottlingEnable(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
                ?: return false).isScanThrottleEnabled
        } else {
            return try {
                Settings.Global.getInt(context.contentResolver, "wifi_scan_throttle_enabled") == 1
            } catch (ex: Settings.SettingNotFoundException) {
                ex.printStackTrace()
                true
            }
        }
    }
}