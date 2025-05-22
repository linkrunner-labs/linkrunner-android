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
    @SerializedName("posthog_distinct_id") val posthogDistinctId: String? = null,
    @SerializedName("clevertap_id") val clevertapId: String? = null,
    @SerializedName("user_created_at") val userCreatedAt: String? = null,
    @SerializedName("is_first_time_user") val isFirstTimeUser: Boolean? = null,
)
