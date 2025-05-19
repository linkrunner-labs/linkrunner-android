package com.linkrunner.sdk.models.request

/**
 * Request model for deeplink triggered API call
 */
internal data class DeeplinkTriggeredRequest(
    val token: String,
    val device_data: Map<String, Any>,
    val install_instance_id: String,
    val platform: String
)
