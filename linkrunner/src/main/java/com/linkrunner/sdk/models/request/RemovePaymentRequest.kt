package com.linkrunner.sdk.models.request

import com.google.gson.annotations.SerializedName

/**
 * Payment removal request model
 */
data class RemovePaymentRequest(
    @SerializedName("payment_id") val paymentId: String? = null,
    @SerializedName("user_id") val userId: String? = null
) {
    init {
        require(paymentId != null || userId != null) {
            "Either paymentId or userId must be provided"
        }
    }
}
