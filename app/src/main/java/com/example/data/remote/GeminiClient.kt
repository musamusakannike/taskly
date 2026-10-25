package com.example.data.remote

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OfflineException(message: String = "AI features require an active internet connection.") : Exception(message)

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateContent(prompt: String, systemInstruction: String? = null, responseMimeType: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("Gemini API key is not configured. Please set your GEMINI_API_KEY."))
        }

        val rootJson = JSONObject()
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()
        val partObj = JSONObject()
        partObj.put("text", prompt)
        partsArray.put(partObj)
        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        rootJson.put("contents", contentsArray)

        if (!systemInstruction.isNullOrBlank()) {
            val systemObj = JSONObject()
            val sysParts = JSONArray()
            sysParts.put(JSONObject().put("text", systemInstruction))
            systemObj.put("parts", sysParts)
            rootJson.put("systemInstruction", systemObj)
        }

        val genConfig = JSONObject()
        genConfig.put("temperature", 0.3)
        if (responseMimeType != null) {
            genConfig.put("responseMimeType", responseMimeType)
        }
        rootJson.put("generationConfig", genConfig)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = rootJson.toString().toRequestBody(mediaType)
        val url = "$BASE_URL?key=$apiKey"
        val request = Request.Builder().url(url).post(body).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    return@withContext Result.failure(Exception("Gemini error (${response.code}): $errorBody"))
                }
                val respString = response.body?.string() ?: ""
                val respJson = JSONObject(respString)
                val candidates = respJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        return@withContext Result.success(text)
                    }
                }
                Result.failure(Exception("Empty response from AI"))
            }
        } catch (e: java.io.IOException) {
            Result.failure(OfflineException())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
