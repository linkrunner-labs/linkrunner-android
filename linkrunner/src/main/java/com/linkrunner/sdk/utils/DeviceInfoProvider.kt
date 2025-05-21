package com.linkrunner.sdk.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.view.WindowManager
import android.webkit.WebSettings
import android.webkit.WebView
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.linkrunner.sdk.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import kotlin.coroutines.resume

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
            deviceInfo["build_number"] = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toString()
            }
            deviceInfo["bundle_id"] = packageName
            deviceInfo["version"] = packageInfo.versionName ?: ""

            // Device info
            deviceInfo["device_id"] = getDeviceId()
            deviceInfo["device_name"] = "${Build.MANUFACTURER} ${Build.MODEL}"
            deviceInfo["manufacturer"] = Build.MANUFACTURER
            deviceInfo["brand"] = Build.BRAND
            deviceInfo["system_version"] = Build.VERSION.RELEASE ?: ""
            deviceInfo["version"] = packageInfo.versionName ?: ""
            deviceInfo["connectivity"] = getNetworkType()
            deviceInfo["user_agent"] = getUserAgent()
            deviceInfo["gaid"] = getAdvertisingId() ?: ""
            // idfa and idfv are iOS only, set as empty string for Android
            deviceInfo["idfa"] = ""
            deviceInfo["idfv"] = ""

            // Carrier info as array
            val carrierName = getCarrierName()
            deviceInfo["carrier"] = if (carrierName.isNotEmpty()) listOf(carrierName) else listOf<String>()

            // IP Address
            getIpAddress()?.let { ipAddress ->
                deviceInfo["device_ip"] = ipAddress
            }

            // Play Store details (install referrer)
            try {
                val installReferrerInfo = getInstallReferrerInfo()
                installReferrerInfo?.let { referrerInfo ->
                    // Set install_ref for parity
                    deviceInfo["install_ref"] = referrerInfo.installReferrer ?: ""
                    // install_ref_url
                    val installReferrerStr = referrerInfo.installReferrer ?: ""
                    deviceInfo["install_ref_url"] = try {
                        java.net.URL(installReferrerStr).toString()
                    } catch (e: Exception) {
                        ""
                    }
                    // install_ref_hashCode (with capital C for backend parity)
                    deviceInfo["install_ref_hashCode"] = installReferrerStr.hashCode()
                    // install_ref_install_version (snake_case for backend)
                    deviceInfo["install_ref_install_version"] = referrerInfo.installVersion ?: ""
                    // install_ref_installBeginTimestampSeconds
                    // install_ref_referrerClickTimestampSeconds
                    deviceInfo["install_ref_installBeginTimestampSeconds"] = referrerInfo.installBeginTimestampSeconds
                    deviceInfo["install_ref_referrerClickTimestampSeconds"] = referrerInfo.referrerClickTimestampSeconds
                    // installBeginTimestampServerSeconds (top-level, as in backend)
                    deviceInfo["installBeginTimestampServerSeconds"] = referrerInfo.installBeginTimestampServerSeconds
                    // referrerClickTimestampServerSeconds (top-level, as in backend)
                    deviceInfo["referrerClickTimestampServerSeconds"] = referrerInfo.referrerClickTimestampServerSeconds
                    // Optionally, keep googlePlayInstantParam if needed by backend
                    deviceInfo["install_ref_googlePlayInstantParam"] = referrerInfo.googlePlayInstantParam
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace()
                }
            }
           
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
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return "Not Connected"
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "Unknown"
                
                return when {
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Network"
                    capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                    else -> "Unknown"
                }
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                @Suppress("DEPRECATION")
                when (networkInfo?.type) {
                    android.net.ConnectivityManager.TYPE_WIFI -> "Wi-Fi"
                    android.net.ConnectivityManager.TYPE_MOBILE -> "Mobile Network"
                    else -> "Unknown"
                }
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
    
    /**
     * Get Play Store install referrer information
     * Returns install referrer details from Google Play Store
     */
    private suspend fun getInstallReferrerInfo(): ReferrerDetails? = suspendCancellableCoroutine { continuation ->
        try {
            val referrerClient = InstallReferrerClient.newBuilder(context).build()
            
            referrerClient.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    when (responseCode) {
                        InstallReferrerClient.InstallReferrerResponse.OK -> {
                            try {
                                val response = referrerClient.installReferrer
                                continuation.resume(response)
                            } catch (e: Exception) {
                                if (BuildConfig.DEBUG) {
                                    e.printStackTrace()
                                }
                                continuation.resume(null)
                            } finally {
                                try {
                                    referrerClient.endConnection()
                                } catch (e: Exception) {
                                    // Ignore connection end errors
                                }
                            }
                        }
                        else -> {
                            try {
                                referrerClient.endConnection()
                            } catch (e: Exception) {
                                // Ignore connection end errors
                            }
                            continuation.resume(null)
                        }
                    }
                }
                
                override fun onInstallReferrerServiceDisconnected() {
                    continuation.resume(null)
                }
            })
            
            continuation.invokeOnCancellation {
                try {
                    referrerClient.endConnection()
                } catch (e: Exception) {
                    // Ignore connection end errors
                }
            }
            
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            continuation.resume(null)
        }
    }
    
    /**
     * Get display information of the device
     */
    private fun getDisplayInfo(): String {
        return try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(metrics)
            
            "${metrics.widthPixels}x${metrics.heightPixels} (${metrics.densityDpi} dpi)"
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Get device type - phone, tablet, etc.
     */
    private fun getDeviceType(): String {
        return try {
            val metrics = context.resources.displayMetrics
            val widthInches = metrics.widthPixels / metrics.xdpi
            val heightInches = metrics.heightPixels / metrics.ydpi
            val diagonalInches = Math.sqrt((widthInches * widthInches + heightInches * heightInches).toDouble())
            
            // Tablet is typically a device with screen larger than 7 inches diagonally
            if (diagonalInches >= 7.0) "Tablet" else "Phone"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    /**
     * Get device user agent
     */
    private fun getUserAgent(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                WebSettings.getDefaultUserAgent(context)
            } else {
                @Suppress("DEPRECATION")
                WebView(context).settings.userAgentString ?: ""
            }
        } catch (e: Exception) {
            // Fallback user agent if WebView is not available
            "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL} Build/${Build.ID})"
        }
    }
    
    /**
     * Get mobile carrier name
     */
    private fun getCarrierName(): String {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            telephonyManager.networkOperatorName ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Get device IP address
     */
    private fun getIpAddress(): String? {
        return try {
            // Check Wi-Fi first
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE)
            if (wifiManager != null) {
                val wifiInfo = Class.forName("android.net.wifi.WifiManager").getMethod("getConnectionInfo").invoke(wifiManager)
                if (wifiInfo != null) {
                    val ipAddress = Class.forName("android.net.wifi.WifiInfo").getMethod("getIpAddress").invoke(wifiInfo) as Int
                    if (ipAddress != 0) {
                        return String.format(
                            Locale.US,
                            "%d.%d.%d.%d",
                            ipAddress and 0xff,
                            ipAddress shr 8 and 0xff,
                            ipAddress shr 16 and 0xff,
                            ipAddress shr 24 and 0xff
                        )
                    }
                }
            }
            
            // Otherwise try to get IP from network interfaces
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is InetAddress) {
                        return address.hostAddress
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
