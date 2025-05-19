package com.linkrunner.sdk.models.request

/**
 * Request model for removing payment API call
 */
internal data class RemovePaymentApiRequest(
    val token: String,
    val platform: String,
    val data: Map<String, Any>,
    val payment_id: String,
    val user_id: String,
    val install_instance_id: String
)
