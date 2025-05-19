package com.linkrunner.sdk.models.response

import com.google.gson.annotations.SerializedName

/**
 * Response model for initialization API
 */
data class InitResponse(
    @SerializedName("campaign_data") val campaignData: ClientCampaignData? = null
) : GeneralResponse()
