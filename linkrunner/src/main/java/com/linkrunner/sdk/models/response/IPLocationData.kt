package com.linkrunner.sdk.models.response

import com.google.gson.annotations.SerializedName

/**
 * Model class for IP location data
 */
data class IPLocationData(
    @SerializedName("ip") val ip: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("countryLong") val countryLong: String? = null,
    @SerializedName("countryShort") val countryShort: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("region") val region: String? = null,
    @SerializedName("timeZone") val timeZone: String? = null,
    @SerializedName("zipCode") val zipCode: String? = null
)
