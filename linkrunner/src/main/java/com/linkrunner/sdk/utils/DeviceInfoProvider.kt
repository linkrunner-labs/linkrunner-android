package com.linkrunner.sdk.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.linkrunner.sdk.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*

/**
 * Utility class to provide device information
 */
internal class DeviceInfoProvider(private val context: Context) {
    
    private val packageManager: PackageManager = context.packageManager
    private val packageName: String = context.packageName
    
    /**
     * Get device information as a map
     */
    @SuppressLint("HardwareIds")
    suspend fun getDeviceInfo(): Map<String, Any> = withContext(Dispatchers.IO) {
        val deviceInfo = mutableMapOf<String, Any>()
        
        try {
            // App info
            val packageInfo: PackageInfo = packageManager.getPackageInfo(packageName, 0)
            
            deviceInfo["application_name"] = getApplicationName()
            deviceInfo["app_version"] = packageInfo.versionName ?: ""
            deviceInfo["build_number"] = packageInfo.longVersionCode.toString()
            deviceInfo["bundle_id"] = packageName
            
            // Device info
            deviceInfo["device_id"] = getDeviceId()
            deviceInfo["device_name"] = "${Build.MANUFACTURER} ${Build.MODEL}"
            deviceInfo["device_model"] = Build.MODEL
            deviceInfo["manufacturer"] = Build.MANUFACTURER
            deviceInfo["brand"] = Build.BRAND
            deviceInfo["android_version"] = Build.VERSION.RELEASE ?: ""
            deviceInfo["api_level"] = Build.VERSION.SDK_INT
            deviceInfo["platform"] = "ANDROID"
            
            // Network info
            deviceInfo["connectivity"] = getNetworkType()
            
            // Advertising ID (GAID)
            getAdvertisingId()?.let { gaid ->
                deviceInfo["gaid"] = gaid
            }
            
            // Install referrer would be set by the app using this SDK
            
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
        }
        
        return@withContext deviceInfo
    }
    
    private fun getApplicationName(): String {
        return try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            ""
        }
    }
    
    @SuppressLint("HardwareIds")
    private fun getDeviceId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: UUID.randomUUID().toString()
    }
    
    private fun getNetworkType(): String {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo
            
            when (networkInfo?.type) {
                android.net.ConnectivityManager.TYPE_WIFI -> "Wi-Fi"
                android.net.ConnectivityManager.TYPE_MOBILE -> "Mobile Network"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private suspend fun getAdvertisingId(): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val advertisingIdInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
            if (advertisingIdInfo.isLimitAdTrackingEnabled) {
                null
            } else {
                advertisingIdInfo.id
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            null
        }
    }
}
