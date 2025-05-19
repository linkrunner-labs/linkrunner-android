package com.linkrunner.sdk.models.response

import com.google.gson.annotations.SerializedName

/**
 * Model class for client campaign data
 */
data class ClientCampaignData(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("ad_network") val adNetwork: String? = null,
    @SerializedName("group_name") val groupName: String? = null,
    @SerializedName("asset_group_name") val assetGroupName: String? = null,
    @SerializedName("asset_name") val assetName: String? = null
)
