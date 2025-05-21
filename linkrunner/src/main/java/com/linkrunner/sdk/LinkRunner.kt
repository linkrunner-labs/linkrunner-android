package com.linkrunner.sdk

import android.content.Context
import android.net.Uri
import androidx.annotation.Keep
import com.linkrunner.sdk.models.request.*
import com.linkrunner.sdk.models.response.InitResponse
import com.linkrunner.sdk.models.response.TriggerResponse
import com.linkrunner.sdk.models.request.CapturePaymentRequest
import com.linkrunner.sdk.models.request.RemovePaymentRequest
import com.linkrunner.sdk.network.ApiClient
import com.linkrunner.sdk.utils.DeviceInfoLogger
import com.linkrunner.sdk.utils.DeviceInfoProvider
import com.linkrunner.sdk.utils.PreferenceManager
import com.linkrunner.sdk.utils.SHA256
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.util.*
import android.util.Log

/**
 * Main entry point for the LinkRunner SDK.
 * This class provides methods to initialize and interact with the LinkRunner service.
 */
@Keep
class LinkRunner private constructor() {
    
    // Token is now accessed via getter/setter with SharedPreferences persistence
    private var _token: String? = null
    var token: String?
        get() {
            if (_token == null) {
                _token = applicationContext?.let {
                    val preferenceManager = PreferenceManager(it)
                    preferenceManager.getString(KEY_TOKEN)
                }
            }
            return _token
        }
        set(value) {
            _token = value
            applicationContext?.let {
                val preferenceManager = PreferenceManager(it)
                if (value != null) {
                    preferenceManager.saveString(KEY_TOKEN, value)
                }
            }
        }
    
    private val packageVersion = "1.1.3"
    
    // Configuration option for PII hashing
    private var hashPII: Boolean = false
    
    private val baseUrl = "https://api.linkrunner.io"
    private var applicationContext: Context? = null
    
    companion object {
        @Volatile
        private var instance: LinkRunner? = null
        private const val KEY_INSTALL_ID = "install_instance_id"
        private const val KEY_DEEPLINK_URL = "deeplink_url"
        private const val KEY_HASH_PII = "hash_pii_enabled"
        private const val KEY_TOKEN = "linkrunner_token"
        
        /**
         * Get the singleton instance of LinkRunner.
         */
        @JvmStatic
        fun getInstance(): LinkRunner {
            return instance ?: synchronized(this) {
                instance ?: LinkRunner().also { instance = it }
            }
        }
        
        private fun generateInstallId(): String = UUID.randomUUID().toString()
    }
    
    /**
     * Initialize dependencies for the SDK
     * @param context Application context
     * @param token Your LinkRunner API token
     * @param link Optional deeplink URL
     * @param source Optional source parameter
     * @return Result containing the initialization response or an exception
     */
    suspend fun init(context: Context, token: String, link: String? = null, source: String? = null): Result<InitResponse> {
        this.applicationContext = context.applicationContext
        
        // Set the token - this will also persist it to SharedPreferences
        this.token = token
        
        // Load saved hashing preference
        val preferenceManager = PreferenceManager(context.applicationContext)
        this.hashPII = preferenceManager.getBoolean(KEY_HASH_PII)
        
        // Initialize dependencies
        initializeDependencies(context.applicationContext)
        
        return initApi(link, source)
    }
    
    private fun initializeDependencies(context: Context) {
        stopKoin()
        startKoin {
            androidContext(context)
            modules(appModule)
        }
    }
    
    private val appModule = module {
        single { ApiClient(get()) }
    }
    
    private fun getApiClient(): ApiClient {
        return org.koin.java.KoinJavaComponent.get(ApiClient::class.java)
    }
    
    private fun getOrCreateInstallId(): String {
        val preferenceManager = PreferenceManager(applicationContext ?: throw IllegalStateException("Context not set"))
        return preferenceManager.getString(KEY_INSTALL_ID).takeIf { it.isNotEmpty() } ?: run {
            val newId = generateInstallId()
            preferenceManager.saveString(KEY_INSTALL_ID, newId)
            newId
        }
    }
    
    private fun saveDeeplinkUrl(url: String) {
        val preferenceManager = PreferenceManager(applicationContext ?: throw IllegalStateException("Context not set"))
        preferenceManager.saveString(KEY_DEEPLINK_URL, url)
    }
    
    private fun loadDeeplinkUrl(): String? {
        val preferenceManager = PreferenceManager(applicationContext ?: throw IllegalStateException("Context not set"))
        return preferenceManager.getString(KEY_DEEPLINK_URL).takeIf { it.isNotEmpty() }
    }
    
    private suspend fun initApi(link: String? = null, source: String? = null): Result<InitResponse> {
        return try {
            val deviceInfoProvider = DeviceInfoProvider(applicationContext ?: throw IllegalStateException("Context not set"))
            val deviceInfo = deviceInfoProvider.getDeviceInfo()
            
            val installId = getOrCreateInstallId()
            
            // Get the app version from device info
            val appVersion = deviceInfo["app_version"] as? String ?: ""
            
            // Create a proper request object instead of a raw map
            val initRequest = InitRequest(
                token = token ?: "",
                package_version = packageVersion,
                app_version = appVersion,
                device_data = deviceInfo,
                platform = "ANDROID",
                install_instance_id = installId,
                link = link,
                source = source
            )
            
            val response = getApiClient().apiService.initialize(initRequest)
            
            ApiClient.handleResponse(response)
                .onSuccess { initResponse ->
                    initResponse.deeplink?.let { url ->
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
        if (token == null) {
            return Result.failure(Exception("Linkrunner token not initialized"))
        }
        
        return try {
            val deviceInfoProvider = DeviceInfoProvider(applicationContext ?: throw IllegalStateException("Context not set"))
            val deviceInfo = deviceInfoProvider.getDeviceInfo()
            val installId = getOrCreateInstallId()
            // Create a TriggerRequest object matching Flutter SDK structure
            // Convert UserDataRequest to a Map to match Flutter's approach
            // Hash sensitive fields (name, email, phone) with SHA-256 only if hashing is enabled
            val userDataMap = if (isPIIHashingEnabled()) {
    mapOf<String, Any?>(
        "id" to userData.id,
        "name" to userData.name?.let { hashWithSHA256(it) },
        "email" to userData.email?.let { hashWithSHA256(it) },
        "phone" to userData.phone?.let { hashWithSHA256(it) },
        "mixpanel_distinct_id" to userData.mixpanelDistinctId,
        "amplitude_device_id" to userData.amplitudeDeviceId,
        "posthog_distinct_id" to userData.posthogDistinctId,
        "user_created_at" to userData.userCreatedAt,
        "is_first_time_user" to userData.isFirstTimeUser
    )
} else {
    mapOf<String, Any?>(
        "id" to userData.id,
        "name" to userData.name,
        "email" to userData.email,
        "phone" to userData.phone,
        "mixpanel_distinct_id" to userData.mixpanelDistinctId,
        "amplitude_device_id" to userData.amplitudeDeviceId,
        "posthog_distinct_id" to userData.posthogDistinctId,
        "user_created_at" to userData.userCreatedAt,
        "is_first_time_user" to userData.isFirstTimeUser
    )
}
            
            // Create data map with device info and additional data
            val dataMap = mutableMapOf<String, Any>(
                "device_data" to deviceInfo
            )
            
            // Add any additional data if provided
            additionalData?.let { dataMap.putAll(it) }
            
            val triggerRequest = TriggerRequest(
                token = token!!,
                user_data = userDataMap,
                platform = "ANDROID",
                data = dataMap,
                install_instance_id = installId,
                event = "SIGNUP"
            )
            
            val response = getApiClient().apiService.trigger(triggerRequest)
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
        if (token == null) {
            return Result.failure(Exception("Linkrunner token not initialized"))
        }
        
        return try {
            val deviceInfoProvider = DeviceInfoProvider(applicationContext ?: throw IllegalStateException("Context not set"))
            val deviceInfo = deviceInfoProvider.getDeviceInfo()
            val installId = getOrCreateInstallId()
            
            // Create data map with device info
            val dataMap = mapOf<String, Any>(
                "device_data" to deviceInfo
            )
            
            // Create a proper CapturePaymentApiRequest object
            val capturePaymentRequest = CapturePaymentApiRequest(
                token = token ?: "",
                platform = "ANDROID",
                data = dataMap,
                payment_id = paymentData.paymentId ?: "",  // API requires this field
                user_id = paymentData.userId,
                amount = paymentData.amount,
                install_instance_id = installId
            )
            
            val response = getApiClient().apiService.capturePayment(capturePaymentRequest)
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
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
        if (token == null) {
            return Result.failure(Exception("Linkrunner token not initialized"))
        }
        
        return try {
            val deviceInfoProvider = DeviceInfoProvider(applicationContext ?: throw IllegalStateException("Context not set"))
            val deviceInfo = deviceInfoProvider.getDeviceInfo()
            val installId = getOrCreateInstallId()
            
            // Create data map with device info
            val dataMap = mapOf<String, Any>(
                "device_data" to deviceInfo
            )
            
            // Create a proper RemovePaymentApiRequest object
            val removePaymentRequest = RemovePaymentApiRequest(
                token = token ?: "",
                platform = "ANDROID",
                data = dataMap,
                payment_id = removePayment.paymentId ?: "",  // API requires this field
                user_id = removePayment.userId ?: "",       // API requires this field
                install_instance_id = installId
            )
            
            val response = getApiClient().apiService.removePayment(removePaymentRequest)
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
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
        if (token == null) {
            return Result.failure(Exception("Linkrunner token not initialized"))
        }
        
        if (eventName.isEmpty()) {
            return Result.failure(Exception("Event name is required"))
        }
        
        return try {
            val deviceInfoProvider = DeviceInfoProvider(applicationContext ?: throw IllegalStateException("Context not set"))
            val deviceInfo = deviceInfoProvider.getDeviceInfo()
            val installId = getOrCreateInstallId()
            
            // Prepare eventData for API call
            val processedEventData = eventData?.let { data ->
                // If eventData is not null, convert it to a Map<String, Any>
                data.entries.associate { entry ->
                    // Convert each entry value to non-null by using empty string for null values
                    entry.key to (entry.value ?: "")
                }
            }
            
            // Create the TrackEventApiRequest object
            val trackEventRequest = TrackEventApiRequest(
                token = token ?: "",
                event_name = eventName,
                event_data = processedEventData,
                platform = "ANDROID",
                device_data = deviceInfo,
                install_instance_id = installId
            )
            
            val response = getApiClient().apiService.trackEvent(trackEventRequest)
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
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
        if (token == null) {
            return Result.failure(Exception("Linkrunner token not initialized"))
        }
        
        return try {
            val deviceInfoProvider = DeviceInfoProvider(applicationContext ?: throw IllegalStateException("Context not set"))
            val deviceInfo = deviceInfoProvider.getDeviceInfo()
            val installId = getOrCreateInstallId()
                
            
            // Convert UserDataRequest to a Map to match Flutter's approach
            // Hash sensitive fields (name, email, phone) with SHA-256 only if hashing is enabled
            val userDataMap = if (isPIIHashingEnabled()) {
                mapOf<String, Any?>(
                    "id" to userData.id,
                    "name" to userData.name?.let { hashWithSHA256(it) },
                    "email" to userData.email?.let { hashWithSHA256(it) },
                    "phone" to userData.phone?.let { hashWithSHA256(it) },
                    "mixpanel_distinct_id" to userData.mixpanelDistinctId,
                    "amplitude_device_id" to userData.amplitudeDeviceId,
                    "posthog_distinct_id" to userData.posthogDistinctId
                )
            } else {
                mapOf<String, Any?>(
                    "id" to userData.id,
                    "name" to userData.name,
                    "email" to userData.email,
                    "phone" to userData.phone,
                    "mixpanel_distinct_id" to userData.mixpanelDistinctId,
                    "amplitude_device_id" to userData.amplitudeDeviceId,
                    "posthog_distinct_id" to userData.posthogDistinctId
                )
            }
            
            // Create a proper SetUserDataRequest object
            val setUserDataRequest = SetUserDataRequest(
                token = token ?: "",
                user_data = userDataMap,
                platform = "ANDROID",
                device_data = deviceInfo,
                install_instance_id = installId
            )
            
            val response = getApiClient().apiService.setUserData(setUserDataRequest)
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
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
        if (token == null) {
            return Result.failure(Exception("Linkrunner token not initialized"))
        }
        
        val storedDeeplink = loadDeeplinkUrl() ?: return Result.failure(Exception("No deeplink URL found"))
        
        return try {
            // Try to open the deeplink
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            intent.data = android.net.Uri.parse(storedDeeplink)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            applicationContext?.startActivity(intent) ?: throw IllegalStateException("Context not set")
            
            // Notify the server that the deeplink was triggered
            val deviceInfoProvider = DeviceInfoProvider(applicationContext ?: throw IllegalStateException("Context not set"))
            val deviceInfo = deviceInfoProvider.getDeviceInfo()
            val installId = getOrCreateInstallId()
            
            // Create a proper DeeplinkTriggeredRequest object
            val deeplinkTriggeredRequest = DeeplinkTriggeredRequest(
                token = token ?: "",
                device_data = deviceInfo,
                install_instance_id = installId,
                platform = "ANDROID"
            )
            
            val response = getApiClient().apiService.deeplinkTriggered(deeplinkTriggeredRequest)
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception("Failed to notify deeplink triggered: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Clean up resources when the SDK is no longer needed
     */
    /**
     * Enable or disable PII hashing
     * @param enabled Whether PII hashing should be enabled
     */
    fun enablePIIHashing(enabled: Boolean = true) {
        this.hashPII = enabled
        val preferenceManager = PreferenceManager(applicationContext ?: throw IllegalStateException("Context not set"))
        preferenceManager.saveBoolean(KEY_HASH_PII, enabled)
    }
    
    /**
     * Check if PII hashing is currently enabled
     * @return true if PII hashing is enabled, false otherwise
     */
    fun isPIIHashingEnabled(): Boolean {
        applicationContext?.let {
            val preferenceManager = PreferenceManager(it)
            this.hashPII = preferenceManager.getBoolean(KEY_HASH_PII)
        }
        return this.hashPII
    }
    
    /**
     * Hash a string using SHA-256 algorithm
     * @param input The string to hash
     * @return Hashed string in hexadecimal format
     */
    fun hashWithSHA256(input: String): String {
        return SHA256.hash(input)
    }
    
    /**
     * Clean up resources when the SDK is no longer needed
     */
    fun destroy() {
        stopKoin()
        instance = null
    }
}
