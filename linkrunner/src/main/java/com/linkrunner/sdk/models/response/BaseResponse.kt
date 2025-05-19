package com.linkrunner.sdk.models.response

import com.google.gson.annotations.SerializedName

/**
 * Base response model for API responses
 */
open class BaseResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: Any? = null
)
