package com.linkrunner.sdk.models.request

import com.google.gson.annotations.SerializedName

/**
 * Event tracking request model
 */
data class TrackEventRequest(
    @SerializedName("event_name") val eventName: String,
    @SerializedName("event_data") val eventData: Map<String, Any>? = null
)
