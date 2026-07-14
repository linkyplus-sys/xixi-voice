package com.linky.voiceclone.api

import android.util.Base64
import com.linky.voiceclone.audio.AudioSampleImporter
import com.linky.voiceclone.audio.SupportedAudioFormat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class SynthResult(val audioBytes: ByteArray, val format: String)

sealed class TtsException(message: String) : Exception(message) {
    class Configuration(message: String) : TtsException(message)
    class InvalidSample(message: String) : TtsException(message)
    class Unauthorized : TtsException("API Key 无效或没有访问权限")
    class RateLimited : TtsException("请求过于频繁或额度不足，请稍后重试")
    class Network(message: String) : TtsException(message)
    class Service(message: String) : TtsException(message)
    class InvalidResponse : TtsException("MiMo 返回了无法解析的音频数据")
}

class MiMoTtsApi(
    private var apiKey: String = "",
    private var baseUrl: String = DEFAULT_BASE_URL,
) {
    private val client = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(2, 5, TimeUnit.MINUTES))
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun updateConfig(key: String, url: String) {
        apiKey = key.trim()
        baseUrl = url.trim().trimEnd('/')
    }

    suspend fun testConnection() {
        validateConfig()
    }

    suspend fun synthesizeClone(
        sampleFile: File,
        text: String,
        context: String = "",
        format: String = "wav",
    ): SynthResult {
        validateConfig()
        val sampleFormat = inspectSample(sampleFile)
        val bytes = withContext(Dispatchers.IO) { sampleFile.readBytes() }
        val encodedSize = AudioSampleImporter.base64EncodedSize(bytes.size.toLong())
        if (encodedSize > AudioSampleImporter.MAX_BASE64_BYTES) {
            throw TtsException.InvalidSample("音频 Base64 编码后超过 10MB")
        }

        val messages = JSONArray()
        if (context.isNotBlank()) {
            messages.put(JSONObject().put("role", "user").put("content", context.trim()))
        }
        messages.put(JSONObject().put("role", "assistant").put("content", text))

        val body = JSONObject()
            .put("model", "mimo-v2.5-tts-voiceclone")
            .put("messages", messages)
            .put(
                "audio",
                JSONObject()
                    .put("format", format)
                    .put(
                        "voice",
                        "data:${sampleFormat.mimeType};base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}",
                    ),
            )
        return callApi(body)
    }

    suspend fun synthesizeDesign(
        voiceDesc: String,
        text: String,
        format: String = "wav",
    ): SynthResult {
        validateConfig()
        val messages = JSONArray()
            .put(JSONObject().put("role", "user").put("content", voiceDesc.trim()))
            .put(JSONObject().put("role", "assistant").put("content", text))
        val body = JSONObject()
            .put("model", "mimo-v2.5-tts-voicedesign")
            .put("messages", messages)
            .put("audio", JSONObject().put("format", format))
        return callApi(body)
    }

    private fun validateConfig() {
        if (apiKey.isBlank()) throw TtsException.Configuration("请先在设置中填写 API Key")
        val url = baseUrl.toHttpUrlOrNull()
            ?: throw TtsException.Configuration("Base URL 格式不正确")
        if (!url.isHttps) throw TtsException.Configuration("Base URL 必须使用 HTTPS")
    }

    private suspend fun inspectSample(file: File): SupportedAudioFormat = withContext(Dispatchers.IO) {
        if (!file.isFile || file.length() == 0L) {
            throw TtsException.InvalidSample("音色样本不存在或为空")
        }
        val header = ByteArray(12)
        val read = file.inputStream().buffered().use { it.read(header) }
        AudioSampleImporter.detectFormat(header, read)
            ?: throw TtsException.InvalidSample("MiMo 音色克隆仅支持 WAV 和 MP3")
    }

    private suspend fun callApi(body: JSONObject): SynthResult = withContext(Dispatchers.IO) {
        var attempt = 0
        while (true) {
            val request = Request.Builder()
                .url("$baseUrl/chat/completions")
                .addHeader("api-key", apiKey)
                .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = try {
                executeAwait(request)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: IOException) {
                if (attempt < MAX_RETRIES) {
                    delay(retryDelay(attempt++))
                    continue
                }
                throw TtsException.Network("无法连接 MiMo，请检查网络后重试")
            }

            val code = response.code
            val retryAfterMs = response.header("Retry-After")
                ?.toLongOrNull()
                ?.coerceIn(1L, 30L)
                ?.times(1_000L)
            val payload = response.body?.string().orEmpty()
            response.close()

            if (code in 200..299) {
                return@withContext parseResult(payload, body)
            }
            if ((code == 429 || code >= 500) && attempt < MAX_RETRIES) {
                delay(retryAfterMs ?: retryDelay(attempt))
                attempt++
                continue
            }
            throw mapHttpError(code, payload)
        }
        @Suppress("UNREACHABLE_CODE")
        throw TtsException.Service("未知服务错误")
    }

    private suspend fun executeAwait(request: Request): Response =
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (continuation.isActive) continuation.resume(response)
                    else response.close()
                }
            })
        }

    private fun parseResult(payload: String, requestBody: JSONObject): SynthResult {
        return try {
            val audioData = JSONObject(payload)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getJSONObject("audio")
                .getString("data")
            val bytes = Base64.decode(audioData, Base64.DEFAULT)
            if (bytes.isEmpty()) throw TtsException.InvalidResponse()
            SynthResult(
                audioBytes = bytes,
                format = requestBody.getJSONObject("audio").optString("format", "wav"),
            )
        } catch (error: TtsException) {
            throw error
        } catch (_: Exception) {
            throw TtsException.InvalidResponse()
        }
    }

    private fun mapHttpError(code: Int, payload: String): TtsException = when (code) {
        400 -> TtsException.Service("请求参数或音频格式不符合 MiMo 要求${detailSuffix(payload)}")
        401, 403 -> TtsException.Unauthorized()
        404 -> TtsException.Service("MiMo 接口地址或模型不可用")
        408 -> TtsException.Network("请求超时，请重试")
        413 -> TtsException.InvalidSample("音频样本过大")
        429 -> TtsException.RateLimited()
        in 500..599 -> TtsException.Service("MiMo 服务暂时不可用（$code）")
        else -> TtsException.Service("MiMo 请求失败（$code）${detailSuffix(payload)}")
    }

    private fun detailSuffix(payload: String): String {
        if (payload.isBlank()) return ""
        val detail = runCatching {
            val json = JSONObject(payload)
            json.optJSONObject("error")?.optString("message")
                ?.ifBlank { null }
                ?: json.optString("message").ifBlank { null }
        }.getOrNull() ?: return ""
        return "：${detail.take(160)}"
    }

    private fun retryDelay(attempt: Int): Long = 800L * (attempt + 1L)

    companion object {
        const val DEFAULT_BASE_URL = "https://token-plan-cn.xiaomimimo.com/v1"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val MAX_RETRIES = 2
    }
}
