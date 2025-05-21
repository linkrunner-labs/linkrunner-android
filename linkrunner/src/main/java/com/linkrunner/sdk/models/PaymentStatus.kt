package com.linkrunner.sdk.models

import com.google.gson.annotations.SerializedName

/**
 * Enum representing different payment statuses
 */
enum class PaymentStatus(val value: String) {
    @SerializedName("PAYMENT_INITIATED")
    PAYMENT_INITIATED("PAYMENT_INITIATED"),
    
    @SerializedName("PAYMENT_COMPLETED")
    PAYMENT_COMPLETED("PAYMENT_COMPLETED"),
    
    @SerializedName("PAYMENT_FAILED")
    PAYMENT_FAILED("PAYMENT_FAILED"),
    
    @SerializedName("PAYMENT_CANCELLED")
    PAYMENT_CANCELLED("PAYMENT_CANCELLED")
}
