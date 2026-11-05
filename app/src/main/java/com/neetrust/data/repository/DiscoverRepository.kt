package com.neetrust.data.repository

import com.neetrust.data.api.ApiService
import com.neetrust.domain.model.AnimeListResponseDto
import com.neetrust.domain.model.ScheduleResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DiscoverRepository(
    private val api: ApiService
) {
    suspend fun schedule(): Result<ScheduleResponseDto> = call { api.getSchedule() }

    suspend fun ongoing(page: Int = 1): Result<AnimeListResponseDto> = call { api.getOngoing(page) }

    suspend fun movies(page: Int = 1): Result<AnimeListResponseDto> = call { api.getMovies(page) }

    private suspend fun <T> call(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
