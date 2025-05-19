package com.linkrunner.sdk.models.response

import com.google.gson.annotations.SerializedName

/**
 * Response model for trigger API
 */
data class TriggerResponse(
    @SerializedName("trigger") val trigger: Boolean? = null
) : GeneralResponse()
