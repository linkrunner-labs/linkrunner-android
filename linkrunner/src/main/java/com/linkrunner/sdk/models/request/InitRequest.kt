package com.linkrunner.sdk.models.request

/**
 * Request model for SDK initialization
 */
internal data class InitRequest(
    val token: String,
    val package_version: String,
    val device_data: Map<String, Any>,
    val platform: String,
    val install_instance_id: String,
    val link: String? = null,
    val source: String? = null
)
