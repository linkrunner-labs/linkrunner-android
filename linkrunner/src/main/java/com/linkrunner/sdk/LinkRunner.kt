package com.linkrunner.sdk

import android.content.Context
import android.net.Uri
import androidx.annotation.Keep
import com.linkrunner.sdk.models.request.CapturePaymentRequest
import com.linkrunner.sdk.models.request.RemovePaymentRequest
import com.linkrunner.sdk.models.request.UserDataRequest
import com.linkrunner.sdk.models.response.InitResponse
import com.linkrunner.sdk.models.response.TriggerResponse
import com.linkrunner.sdk.network.ApiClient
import com.linkrunner.sdk.utils.DeviceInfoProvider
import com.linkrunner.sdk.utils.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.util.*

/**
 * Main entry point for the LinkRunner SDK.
 * This class provides methods to initialize and interact with the LinkRunner service.
 */
@Keep
class LinkRunner private constructor() {

    private var isInitialized = false
    private var token: String? = null
    private val appContext: Context by lazy { applicationContext ?: throw IllegalStateException("Context not set") }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val preferenceManager by lazy { PreferenceManager(appContext) }
    private val deviceInfoProvider by lazy { DeviceInfoProvider(appContext) }
    
    private var applicationContext: Context? = null
    private var deeplinkUrl: String? = null
    
    companion object {
        @Volatile
        private var instance: LinkRunner? = null
        private const val PREF_NAME = "linkrunner_prefs"
        private const val KEY_INSTALL_ID = "install_instance_id"
        private const val KEY_DEEPLINK_URL = "deeplink_url"
        
        /**
         * Initialize the LinkRunner SDK.
         * @param context Application context
         * @param token Your LinkRunner API token
         * @return Singleton instance of LinkRunner
         */
        @JvmStatic
        @Synchronized
        fun initialize(context: Context, token: String): LinkRunner {
            return instance ?: synchronized(this) {
                instance ?: LinkRunner().apply {
                    this.token = token
                    this.applicationContext = context.applicationContext
                    initializeDependencies()
                    isInitialized = true
                }.also { instance = it }
            }
        }
        
        /**
         * Get the singleton instance of LinkRunner.
         * @throws IllegalStateException if initialize() has not been called
         */
        @JvmStatic
        fun getInstance(): LinkRunner {
            return instance ?: throw IllegalStateException(
                "LinkRunner must be initialized first. Call LinkRunner.initialize(context, token)"
            )
        }
        
        private fun generateInstallId(): String = UUID.randomUUID().toString()
    }
    
    private fun initializeDependencies() {
        startKoin {
            androidContext(appContext)
            modules(appModule)
        }
    }
    
    private val appModule = module {
        single { ApiClient(get()) }
    }
    
    private fun requireInitialized() {
        check(isInitialized) { "LinkRunner must be initialized first" }
    }
    
    private fun getApiClient(): ApiClient {
        return org.koin.java.KoinJavaComponent.get(ApiClient::class.java)
    }
    
    private fun getOrCreateInstallId(): String {
        return preferenceManager.getString(KEY_INSTALL_ID).takeIf { it.isNotEmpty() } ?: run {
            val newId = generateInstallId()
            preferenceManager.saveString(KEY_INSTALL_ID, newId)
            newId
        }
    }
    
    private fun saveDeeplinkUrl(url: String) {
        preferenceManager.saveString(KEY_DEEPLINK_URL, url)
    }
    
    private fun loadDeeplinkUrl(): String? {
        return preferenceManager.getString(KEY_DEEPLINK_URL).takeIf { it.isNotEmpty() }
    }
    
    /**
     * Initialize the SDK with optional deeplink handling
     * @param link Optional deeplink URL
     * @param source Optional source parameter
     * @return Result containing the initialization response or an exception
     */
    suspend fun init(link: String? = null, source: String? = null): Result<InitResponse> {
        requireInitialized()
        
        return try {
            val deviceInfo = deviceInfoProvider.getDeviceInfo()
            val installId = getOrCreateInstallId()
            
            val response = getApiClient().apiService.initialize(
                mapOf(
                    "token" to token,
                    "package_version" to "1.0.0",
                    "device_data" to deviceInfo,
                    "platform" to "ANDROID",
                    "link" to link,
                    "source" to source,
                    "install_instance_id" to installId
                )
            )
            
            ApiClient.handleResponse(response).onSuccess { initResponse ->
                initResponse.deeplink?.let { url ->
                    deeplinkUrl = url
                    saveDeeplinkUrl(url)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Track a user signup event
     * @param userData User data to be associated with the signup
     * @param additionalData Additional custom data to be sent with the event
     * @return Result containing the trigger response or an exception
     */
    suspend fun signup(
        userData: UserDataRequest,
        additionalData: Map<String, Any>? = null
    ): Result<TriggerResponse> {
        requireInitialized()
        
        return try {
            val deviceInfo = deviceInfoProvider.getDeviceInfo()
            val installId = getOrCreateInstallId()
            
            val requestData = mutableMapOf<String, Any>(
                "token" to token!!,
                "user_data" to userData,
                "platform" to "ANDROID",
                "data" to (additionalData?.toMutableMap() ?: mutableMapOf()).apply {
                    put("device_data", deviceInfo)
                },
                "install_instance_id" to installId
            )
            
            val response = getApiClient().apiService.trigger(requestData)
            ApiClient.handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Track a payment event
     * @param paymentData Payment data to be tracked
     */
    suspend fun capturePayment(paymentData: CapturePaymentRequest): Result<Unit> {
        requireInitialized()
        
        return try {
            val deviceInfo = deviceInfoProvider.getDeviceInfo()
            val installId = getOrCreateInstallId()
            
            val response = getApiClient().apiService.capturePayment(
                mapOf(
                    "token" to token!!,
                    "platform" to "ANDROID",
                    "data" to mapOf("device_data" to deviceInfo),
                    "payment_id" to paymentData.paymentId,
                    "user_id" to paymentData.userId,
                    "amount" to paymentData.amount,
                    "install_instance_id" to installId
                )
            )
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to capture payment: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Remove a previously captured payment
     * @param removePayment Payment data to be removed
     */
    suspend fun removePayment(removePayment: RemovePaymentRequest): Result<Unit> {
        requireInitialized()
        
        return try {
            val deviceInfo = deviceInfoProvider.getDeviceInfo()
            val installId = getOrCreateInstallId()
            
            val requestMap = mutableMapOf<String, Any?>(
                "token" to token!!,
                "platform" to "ANDROID",
                "data" to mapOf("device_data" to deviceInfo),
                "install_instance_id" to installId
            )
            
            removePayment.paymentId?.let { requestMap["payment_id"] = it }
            removePayment.userId?.let { requestMap["user_id"] = it }
            
            val response = getApiClient().apiService.removePayment(requestMap)
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to remove payment: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Track a custom event
     * @param eventName Name of the event to track
     * @param eventData Optional additional event data
     */
    suspend fun trackEvent(
        eventName: String,
        eventData: Map<String, Any>? = null
    ): Result<Unit> {
        requireInitialized()
        
        return try {
            val deviceInfo = deviceInfoProvider.getDeviceInfo()
            val installId = getOrCreateInstallId()
            
            val response = getApiClient().apiService.trackEvent(
                mapOf(
                    "token" to token!!,
                    "event_name" to eventName,
                    "event_data" to eventData,
                    "platform" to "ANDROID",
                    "device_data" to deviceInfo,
                    "install_instance_id" to installId
                )
            )
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to track event: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Set user data for the current session
     * @param userData User data to be associated with the session
     */
    suspend fun setUserData(userData: UserDataRequest): Result<Unit> {
        requireInitialized()
        
        return try {
            val deviceInfo = deviceInfoProvider.getDeviceInfo()
            val installId = getOrCreateInstallId()
            
            val response = getApiClient().apiService.setUserData(
                mapOf(
                    "token" to token!!,
                    "user_data" to userData,
                    "platform" to "ANDROID",
                    "device_data" to deviceInfo,
                    "install_instance_id" to installId
                )
            )
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to set user data: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Trigger a deeplink that was previously stored
     * This is used for deferred deep linking functionality
     */
    suspend fun triggerDeeplink(): Result<Unit> {
        requireInitialized()
        
        val storedDeeplink = loadDeeplinkUrl() ?: return Result.failure(Exception("No deeplink URL found"))
        
        return try {
            // Try to open the deeplink
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            intent.data = android.net.Uri.parse(storedDeeplink)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
            
            // Notify the server that the deeplink was triggered
            val deviceInfo = deviceInfoProvider.getDeviceInfo()
            val installId = getOrCreateInstallId()
            
            val response = getApiClient().apiService.deeplinkTriggered(
                mapOf(
                    "token" to token!!,
                    "device_data" to deviceInfo,
                    "install_instance_id" to installId,
                    "platform" to "ANDROID"
                )
            )
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to notify deeplink triggered: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Clean up resources when the SDK is no longer needed
     */
    fun destroy() {
        stopKoin()
        instance = null
    }
}
