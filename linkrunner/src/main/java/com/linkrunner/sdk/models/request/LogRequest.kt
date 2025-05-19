package com.linkrunner.sdk.models.request

/**
 * Request model for logging errors or warnings
 */
internal data class LogRequest(
    val token: String,
    val platform: String,
    val level: String,  // 'error', 'warning', etc.
    val message: String,
    val device_data: Map<String, Any>,
    val install_instance_id: String,
    val meta: Map<String, Any>? = null
)
