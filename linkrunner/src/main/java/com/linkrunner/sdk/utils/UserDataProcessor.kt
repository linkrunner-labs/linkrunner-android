package com.linkrunner.sdk.utils

import com.linkrunner.sdk.models.request.UserDataRequest

/**
 * Helper class to process user data for API requests
 * Handles hashing PII data when enabled
 */
internal class UserDataProcessor {
    
    companion object {
        /**
         * Process user data and apply hashing if enabled
         * @param userData The original user data
         * @param hashPII Whether to hash PII data
         * @return Map containing the processed user data
         */
        fun processUserData(userData: UserDataRequest, hashPII: Boolean): Map<String, Any?> {
            return if (hashPII) {
                mapOf<String, Any?>(
                    "id" to userData.id,
                    "name" to userData.name?.let { SHA256.hash(it) },
                    "email" to userData.email?.let { SHA256.hash(it) },
                    "phone" to userData.phone?.let { SHA256.hash(it) },
                    "mixpanel_distinct_id" to userData.mixpanelDistinctId,
                    "amplitude_device_id" to userData.amplitudeDeviceId,
                    "posthog_distinct_id" to userData.posthogDistinctId
                )
            } else {
                mapOf<String, Any?>(
                    "id" to userData.id,
                    "name" to userData.name,
                    "email" to userData.email,
                    "phone" to userData.phone,
                    "mixpanel_distinct_id" to userData.mixpanelDistinctId,
                    "amplitude_device_id" to userData.amplitudeDeviceId,
                    "posthog_distinct_id" to userData.posthogDistinctId
                )
            }
        }
    }
}
