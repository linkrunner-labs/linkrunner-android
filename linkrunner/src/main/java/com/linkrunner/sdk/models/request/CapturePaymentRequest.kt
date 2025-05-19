package com.linkrunner.sdk.models.request

import com.google.gson.annotations.SerializedName

/**
 * Payment capture request model
 */
data class CapturePaymentRequest(
    @SerializedName("payment_id") val paymentId: String? = null,
    @SerializedName("user_id") val userId: String,
    @SerializedName("amount") val amount: Double
)
