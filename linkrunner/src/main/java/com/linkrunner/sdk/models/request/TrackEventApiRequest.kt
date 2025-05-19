package com.linkrunner.sdk.models.request

/**
 * API request model for tracking events
 */
internal data class TrackEventApiRequest(
    val token: String,
    val event_name: String,
    val event_data: Map<String, Any>?,
    val platform: String,
    val device_data: Map<String, Any>,
    val install_instance_id: String
)
