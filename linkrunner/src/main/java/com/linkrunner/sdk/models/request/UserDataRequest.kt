package com.linkrunner.sdk.models.request

import com.google.gson.annotations.SerializedName

/**
 * User data model for API requests
 */
data class UserDataRequest(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("mixpanel_distinct_id") val mixpanelDistinctId: String? = null,
    @SerializedName("amplitude_device_id") val amplitudeDeviceId: String? = null,
    @SerializedName("posthog_distinct_id") val posthogDistinctId: String? = null
)

/**
 * Payment capture request model
 */
data class CapturePaymentRequest(
    @SerializedName("payment_id") val paymentId: String? = null,
    @SerializedName("user_id") val userId: String,
    @SerializedName("amount") val amount: Double
)

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

/**
 * Event tracking request model
 */
data class TrackEventRequest(
    @SerializedName("event_name") val eventName: String,
    @SerializedName("event_data") val eventData: Map<String, Any>? = null
)
