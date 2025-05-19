package com.linkrunner.sdk.models.response

import com.google.gson.annotations.SerializedName

/**
 * General response class that serves as a base for other responses
 */
open class GeneralResponse(
    @SerializedName("ip_location_data") val ipLocationData: IPLocationData? = null,
    @SerializedName("deeplink") val deeplink: String? = null,
    @SerializedName("root_domain") val rootDomain: Boolean? = null
)
