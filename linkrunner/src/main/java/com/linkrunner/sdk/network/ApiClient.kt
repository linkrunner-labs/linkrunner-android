package com.linkrunner.sdk.network

import android.content.Context
import com.google.gson.GsonBuilder
import com.linkrunner.sdk.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API client for handling network requests
 */
@Singleton
internal class ApiClient @Inject constructor(private val context: Context) {
    private val baseUrl = "https://api.linkrunner.io/"
    
    private val okHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
        
        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }
        
        builder.build()
    }
    
    private val gson = GsonBuilder()
        .setLenient()
        .create()
    
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
    
    companion object {
        /**
         * Handle API response and return a Result type
         */
        fun <T> handleResponse(response: retrofit2.Response<T>): Result<T> {
            return try {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Result.success(body)
                    } else {
                        Result.failure(Exception("Response body is null"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Result.failure(Exception("API Error: ${response.code()} - $errorBody"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        
        /**
         * Extension function to handle successful responses
         */
        fun <T> Result<T>.onSuccess(block: (T) -> Unit): Result<T> {
            if (isSuccess) {
                block(getOrNull()!!)
            }
            return this
        }
        
        /**
         * Extension function to handle failed responses
         */
        fun <T> Result<T>.onFailure(block: (Throwable) -> Unit): Result<T> {
            exceptionOrNull()?.let { block(it) }
            return this
        }
    }
}
