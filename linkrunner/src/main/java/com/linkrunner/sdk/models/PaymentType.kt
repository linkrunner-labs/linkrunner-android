package com.linkrunner.sdk.models

import com.google.gson.annotations.SerializedName

/**
 * Enum representing different types of payments
 */
enum class PaymentType(val value: String) {
    @SerializedName("FIRST_PAYMENT")
    FIRST_PAYMENT("FIRST_PAYMENT"),
    
    @SerializedName("WALLET_TOPUP")
    WALLET_TOPUP("WALLET_TOPUP"),
    
    @SerializedName("FUNDS_WITHDRAWAL")
    FUNDS_WITHDRAWAL("FUNDS_WITHDRAWAL"),
    
    @SerializedName("SUBSCRIPTION_CREATED")
    SUBSCRIPTION_CREATED("SUBSCRIPTION_CREATED"),
    
    @SerializedName("SUBSCRIPTION_RENEWED")
    SUBSCRIPTION_RENEWED("SUBSCRIPTION_RENEWED"),
    
    @SerializedName("ONE_TIME")
    ONE_TIME("ONE_TIME"),
    
    @SerializedName("RECURRING")
    RECURRING("RECURRING"),
    
    @SerializedName("DEFAULT")
    DEFAULT("DEFAULT")
}
