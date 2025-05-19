package com.linkrunner.sdk.network

import com.linkrunner.sdk.models.response.BaseResponse
import com.linkrunner.sdk.models.response.InitResponse
import com.linkrunner.sdk.models.response.TriggerResponse
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit service interface for LinkRunner API
 */
internal interface ApiService {
    /**
     * Initialize the SDK with the server
     */
    @POST("api/client/init")
    suspend fun initialize(@Body body: Map<String, Any?>): Response<InitResponse>
    
    /**
     * Trigger an action on the server
     */
    @POST("api/client/trigger")
    suspend fun trigger(@Body body: Map<String, Any?>): Response<TriggerResponse>
    
    /**
     * Set user data for the current session
     */
    @POST("api/client/set-user-data")
    suspend fun setUserData(@Body body: Map<String, Any?>): Response<BaseResponse>
    
    /**
     * Capture a payment event
     */
    @POST("api/client/capture-payment")
    suspend fun capturePayment(@Body body: Map<String, Any?>): Response<BaseResponse>
    
    /**
     * Remove a previously captured payment
     */
    @POST("api/client/remove-captured-payment")
    suspend fun removePayment(@Body body: Map<String, Any?>): Response<BaseResponse>
    
    /**
     * Track a custom event
     */
    @POST("api/client/capture-event")
    suspend fun trackEvent(@Body body: Map<String, Any?>): Response<BaseResponse>
    
    /**
     * Notify that a deeplink was triggered
     */
    @POST("api/client/deeplink-triggered")
    suspend fun deeplinkTriggered(@Body body: Map<String, Any?>): Response<BaseResponse>
    
    /**
     * Get the current user's profile
     */
    @GET("api/client/getProfile")
    suspend fun getProfile(@Query("token") token: String): Response<BaseResponse>
    
    /**
     * Update user's push notification token
     */
    @POST("api/client/updatePushToken")
    suspend fun updatePushToken(@Body body: Map<String, Any?>): Response<BaseResponse>
    
    /**
     * Log an error or warning
     */
    @POST("api/client/log")
    suspend fun log(@Body body: Map<String, Any?>): Response<BaseResponse>
}
