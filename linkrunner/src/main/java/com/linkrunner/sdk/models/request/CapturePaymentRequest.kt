package com.linkrunner.sdk.models.request

import com.google.gson.annotations.SerializedName
import com.linkrunner.sdk.models.PaymentStatus
import com.linkrunner.sdk.models.PaymentType

/**
 * Payment capture request model
 */
data class CapturePaymentRequest(
    @SerializedName("payment_id") val paymentId: String? = null,
    @SerializedName("user_id") val userId: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("type") val type: PaymentType = PaymentType.DEFAULT,
    @SerializedName("status") val status: PaymentStatus = PaymentStatus.PAYMENT_COMPLETED
)
