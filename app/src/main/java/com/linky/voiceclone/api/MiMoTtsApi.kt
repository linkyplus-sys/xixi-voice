package com.linky.voiceclone.api

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class SynthResult(val audioBytes: ByteArray, val format: String)

class MiMoTtsApi(
    private var apiKey: String = "",
    private var baseUrl: String = "https://token-plan-cn.xiaomimimo.com/v1"
) {
    private val client = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(2, 5, TimeUnit.MINUTES))
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    fun updateConfig(key: String, url: String) {
        apiKey = key
        baseUrl = url.trimEnd('/')
    }

    suspend fun synthesizeClone(
        sampleFile: File, text: String, format: String = "wav"
    ): SynthResult = withContext(Dispatchers.IO) {
        val ext = sampleFile.extension.lowercase()
        val mime = when (ext) {
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            else -> throw Exception("不支持的样本格式 .$ext，仅支持 WAV 和 MP3")
        }
        val b64 = Base64.encodeToString(sampleFile.readBytes(), Base64.NO_WRAP)
        val messages = JSONArray().put(
            JSONObject().put("role", "assistant").put("content", text)
        )
        val body = JSONObject()
            .put("model", "mimo-v2.5-tts-voiceclone")
            .put("messages", messages)
            .put("audio", JSONObject().put("format", format).put("voice", "data:$mime;base64,$b64"))
        callApi(body)
    }

    suspend fun synthesizeDesign(
        voiceDesc: String, text: String, format: String = "wav"
    ): SynthResult = withContext(Dispatchers.IO) {
        val messages = JSONArray()
            .put(JSONObject().put("role", "user").put("content", voiceDesc))
            .put(JSONObject().put("role", "assistant").put("content", text))
        val body = JSONObject()
            .put("model", "mimo-v2.5-tts-voicedesign")
            .put("messages", messages)
            .put("audio", JSONObject().put("format", format))
        callApi(body)
    }

    private fun callApi(body: JSONObject): SynthResult {
        val req = Request.Builder()
            .url("$baseUrl/chat/completions")
            .addHeader("api-key", apiKey)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) {
            throw Exception("API ${resp.code}: ${resp.body?.string() ?: ""}")
        }
        val json = JSONObject(resp.body!!.string())
        val audioData = json.getJSONArray("choices")
            .getJSONObject(0).getJSONObject("message")
            .getJSONObject("audio").getString("data")
        return SynthResult(Base64.decode(audioData, Base64.DEFAULT), body.getJSONObject("audio").optString("format", "wav"))
    }
}
