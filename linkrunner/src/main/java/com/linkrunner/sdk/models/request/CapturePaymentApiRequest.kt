package com.linkrunner.sdk.models.request

/**
 * Request model for capturing payment API call
 */
internal data class CapturePaymentApiRequest(
    val token: String,
    val platform: String,
    val data: Map<String, Any>,
    val payment_id: String,
    val user_id: String,
    val amount: Double,
    val type: String,
    val status: String,
    val install_instance_id: String
)
