package com.linkrunner.sdk.models.request

/**
 * Request model for updating push notification token
 */
internal data class UpdatePushTokenRequest(
    val token: String,
    val platform: String,
    val push_token: String,
    val device_data: Map<String, Any>,
    val install_instance_id: String
)
