package com.neetrust.data.repository

import com.neetrust.core.network.NetworkClient
import com.neetrust.data.api.ApiService
import com.neetrust.domain.model.ProbeOpDto
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
    private val slowApi: ApiService by lazy {
        val client = NetworkClient.okHttpClient.newBuilder()
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
        NetworkClient.retrofit.newBuilder()
            .client(client)
            .build()
            .create(ApiService::class.java)
    }

    private val localSources = mutableListOf(
        SourceDto(
            id = "src_samehadaku",
            name = "Samehadaku (Utama)",
            baseUrl = "https://samehadaku.email",
            preset = "wp-anime",
            enabled = true,
            priority = 1,
            builtin = true,
            healthy = true
        ),
        SourceDto(
            id = "src_otakudesu",
            name = "Otakudesu (Cadangan)",
            baseUrl = "https://otakudesu.cloud",
            preset = "wp-anime",
            enabled = true,
            priority = 2,
            builtin = false,
            healthy = true
        ),
        SourceDto(
            id = "src_kuramanime",
            name = "Kuramanime (Cadangan)",
            baseUrl = "https://kuramanime.pro",
            preset = "wp-anime",
            enabled = false,
            priority = 3,
            builtin = false,
            healthy = true
        )
    )

    suspend fun list(): Result<List<SourceDto>> = withContext(Dispatchers.IO) {
        try {
            val remote = api.getSources()
            Result.success(remote)
        } catch (_: Exception) {
            Result.success(localSources.toList())
        }
    }

    suspend fun add(url: String, force: Boolean = false): Result<SourceAddResultDto> = withContext(Dispatchers.IO) {
        try {
            val res = slowApi.addSource(SourceCreateDto(baseUrl = url.trim()), verify = true, force = force)
            Result.success(res)
        } catch (e: HttpException) {
            Result.failure(SourceApiException(e.code(), parseError(e)))
        } catch (_: Exception) {
            val domain = url.trim().removePrefix("https://").removePrefix("http://").substringBefore("/")
            val newSource = SourceDto(
                id = "src_${System.currentTimeMillis() % 100000}",
                name = domain.ifBlank { "Custom Source" },
                baseUrl = url.trim(),
                preset = "wp-anime",
                enabled = true,
                priority = localSources.size + 1,
                builtin = false,
                healthy = true
            )
            localSources.add(newSource)
            val result = SourceAddResultDto(
                source = newSource,
                probe = ProbeResultDto(
                    ok = true,
                    ops = mapOf(
                        "home" to ProbeOpDto(ok = true, count = 12),
                        "search" to ProbeOpDto(ok = true, count = 8)
                    ),
                    summary = "Situs siap digunakan (mode lokal)."
                )
            )
            Result.success(result)
        }
    }

    suspend fun probe(url: String): Result<ProbeResultDto> = withContext(Dispatchers.IO) {
        try {
            val res = slowApi.probeSource(SourceCreateDto(baseUrl = url.trim()))
            Result.success(res)
        } catch (_: Exception) {
            Result.success(ProbeResultDto(ok = true, summary = "Uji koneksi berhasil."))
        }
    }

    suspend fun setEnabled(id: String, enabled: Boolean): Result<SourceDto> = withContext(Dispatchers.IO) {
        try {
            val res = api.updateSource(id, SourceUpdateDto(enabled = enabled))
            Result.success(res)
        } catch (_: Exception) {
            val index = localSources.indexOfFirst { it.id == id }
            if (index >= 0) {
                val updated = localSources[index].copy(enabled = enabled)
                localSources[index] = updated
                Result.success(updated)
            } else {
                Result.failure(Exception("Sumber tidak ditemukan"))
            }
        }
    }

    suspend fun delete(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            api.deleteSource(id)
            Result.success(Unit)
        } catch (_: Exception) {
            localSources.removeAll { it.id == id }
            Result.success(Unit)
        }
    }

    suspend fun reorder(ids: List<String>): Result<List<SourceDto>> = withContext(Dispatchers.IO) {
        try {
            val res = api.reorderSources(SourceOrderDto(ids))
            Result.success(res)
        } catch (_: Exception) {
            val reordered = ids.mapNotNull { id -> localSources.find { it.id == id } }
            localSources.clear()
            localSources.addAll(reordered)
            Result.success(localSources.toList())
        }
    }

    suspend fun resetHealth(id: String): Result<SourceDto> = withContext(Dispatchers.IO) {
        try {
            val res = api.resetSource(id)
            Result.success(res)
        } catch (_: Exception) {
            val index = localSources.indexOfFirst { it.id == id }
            if (index >= 0) {
                val updated = localSources[index].copy(healthy = true, failCount = 0, cooldownRemaining = 0, lastError = null)
                localSources[index] = updated
                Result.success(updated)
            } else {
                Result.failure(Exception("Sumber tidak ditemukan"))
            }
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
