package com.linkrunner.sdk.utils

import android.util.Log

/**
 * Utility class for logging device information
 */
object DeviceInfoLogger {
    private const val TAG = "LinkRunner-DeviceInfo"
    
    /**
     * Log device information in a readable format
     */
    fun logDeviceInfo(deviceInfo: Map<String, Any>) {
        Log.d(TAG, "========= DEVICE INFO COLLECTED =========")
        
        // Log each property on a separate line for readability
        deviceInfo.entries.forEach { (key, value) ->
            Log.d(TAG, "$key: $value")
        }
        
        // Check for specific properties of interest
        Log.d(TAG, "Device ID: ${deviceInfo["device_id"]}")
        Log.d(TAG, "GAID present: ${deviceInfo.containsKey("gaid")}")
        if (deviceInfo.containsKey("gaid")) {
            Log.d(TAG, "GAID value: ${deviceInfo["gaid"]}")
        }
        
        Log.d(TAG, "=========================================")
    }
}
