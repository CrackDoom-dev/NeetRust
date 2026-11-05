package com.neetrust.data.repository

import com.neetrust.core.network.NetworkClient
import com.neetrust.data.api.ApiService
import com.neetrust.domain.model.ProbeResultDto
import com.neetrust.domain.model.SourceAddResultDto
import com.neetrust.domain.model.SourceCreateDto
import com.neetrust.domain.model.SourceDto
import com.neetrust.domain.model.SourceOrderDto
import com.neetrust.domain.model.SourceUpdateDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import java.util.concurrent.TimeUnit

class SourceApiException(val code: Int, message: String) : Exception(message)

class SourceRepository(
    private val api: ApiService
) {
    // Uji situs bisa memakan waktu lebih lama dari batas baca default (15 dtk).
    private val slowApi: ApiService by lazy {
        val client = NetworkClient.okHttpClient.newBuilder()
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
        NetworkClient.retrofit.newBuilder()
            .client(client)
            .build()
            .create(ApiService::class.java)
    }

    suspend fun list(): Result<List<SourceDto>> = call { api.getSources() }

    suspend fun add(url: String, force: Boolean = false): Result<SourceAddResultDto> =
        call { slowApi.addSource(SourceCreateDto(baseUrl = url.trim()), verify = true, force = force) }

    suspend fun probe(url: String): Result<ProbeResultDto> =
        call { slowApi.probeSource(SourceCreateDto(baseUrl = url.trim())) }

    suspend fun setEnabled(id: String, enabled: Boolean): Result<SourceDto> =
        call { api.updateSource(id, SourceUpdateDto(enabled = enabled)) }

    suspend fun delete(id: String): Result<Unit> = call {
        api.deleteSource(id)
        Unit
    }

    suspend fun reorder(ids: List<String>): Result<List<SourceDto>> =
        call { api.reorderSources(SourceOrderDto(ids)) }

    suspend fun resetHealth(id: String): Result<SourceDto> = call { api.resetSource(id) }

    private suspend fun <T> call(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (e: HttpException) {
            Result.failure(SourceApiException(e.code(), parseError(e)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseError(e: HttpException): String {
        val raw = try {
            e.response()?.errorBody()?.string()
        } catch (_: Exception) {
            null
        }
        if (raw.isNullOrBlank()) return "Server membalas HTTP ${e.code()}"
        return try {
            val json = JSONObject(raw)
            val msg = json.optString("detail").ifBlank { json.optString("error") }
            msg.ifBlank { "Server membalas HTTP ${e.code()}" }
        } catch (_: Exception) {
            "Server membalas HTTP ${e.code()}"
        }
    }
}
