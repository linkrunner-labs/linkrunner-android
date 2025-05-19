package com.linkrunner.sdk.models.request

/**
 * Request model for setting user data
 */
internal data class SetUserDataRequest(
    val token: String,
    val user_data: Map<String, Any?>,  // User data as a map
    val platform: String,
    val device_data: Map<String, Any>,
    val install_instance_id: String
)
