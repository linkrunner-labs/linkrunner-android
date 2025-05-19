package com.linkrunner.sdk.models.request

/**
 * Request model for SDK trigger actions
 */
internal data class TriggerRequest(
    val token: String,
    val user_data: Map<String, Any?>,  // Pass the user data model directly as a map
    val platform: String,
    val data: Map<String, Any>,       // Contains device_data and additional custom data
    val install_instance_id: String,
    val event: String? = null,
    val link: String? = null,
    val value: String? = null,
    val source: String? = null
)
