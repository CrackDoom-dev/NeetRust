package com.neetrust.data.repository

import com.neetrust.data.api.ApiService
import com.neetrust.domain.model.AnimeDto
import com.neetrust.domain.model.AnimeListResponseDto
import com.neetrust.domain.model.ScheduleDayDto
import com.neetrust.domain.model.ScheduleItemDto
import com.neetrust.domain.model.ScheduleResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DiscoverRepository(
    private val api: ApiService
) {
    private val wajikClient = com.neetrust.data.api.WajikAnimeClient()
    private val jikanClient = com.neetrust.data.api.JikanAnimeClient()

    suspend fun schedule(): Result<ScheduleResponseDto> = withContext(Dispatchers.IO) {
        jikanClient.getSchedule().onSuccess { return@withContext Result.success(it) }
        try {
            Result.success(api.getSchedule())
        } catch (_: Exception) {
            Result.success(getFallbackSchedule())
        }
    }

    suspend fun ongoing(page: Int = 1): Result<AnimeListResponseDto> = withContext(Dispatchers.IO) {
        wajikClient.getOngoing(page).onSuccess { return@withContext Result.success(it) }
        jikanClient.getOngoing(page).onSuccess { return@withContext Result.success(it) }
        try {
            Result.success(api.getOngoing(page))
        } catch (_: Exception) {
            Result.success(getFallbackOngoing())
        }
    }

    suspend fun movies(page: Int = 1): Result<AnimeListResponseDto> = withContext(Dispatchers.IO) {
        jikanClient.getMovies(page).onSuccess { return@withContext Result.success(it) }
        try {
            Result.success(api.getMovies(page))
        } catch (_: Exception) {
            Result.success(getFallbackMovies())
        }
    }

    private fun getFallbackSchedule(): ScheduleResponseDto {
        val days = listOf(
            ScheduleDayDto(
                day = "Senin",
                items = listOf(
                    ScheduleItemDto(
                        title = "Tsukimichi: Moonlit Fantasy S2",
                        slug = "tsukimichi-moonlit-fantasy-s2",
                        poster = "https://cdn.myanimelist.net/images/anime/1630/140078l.jpg",
                        time = "22:00 WIB",
                        episode = "10",
                        type = "TV"
                    ),
                    ScheduleItemDto(
                        title = "Kingdom Season 5",
                        slug = "kingdom-season-5",
                        poster = "https://cdn.myanimelist.net/images/anime/1208/140081l.jpg",
                        time = "23:30 WIB",
                        episode = "09",
                        type = "TV"
                    )
                )
            ),
            ScheduleDayDto(
                day = "Selasa",
                items = listOf(
                    ScheduleItemDto(
                        title = "Black Clover",
                        slug = "black-clover",
                        poster = "https://cdn.myanimelist.net/images/anime/2/88336l.jpg",
                        time = "18:00 WIB",
                        episode = "170",
                        type = "TV"
                    ),
                    ScheduleItemDto(
                        title = "Shaman King: Flowers",
                        slug = "shaman-king-flowers",
                        poster = "https://cdn.myanimelist.net/images/anime/1094/139972l.jpg",
                        time = "21:00 WIB",
                        episode = "11",
                        type = "TV"
                    )
                )
            ),
            ScheduleDayDto(
                day = "Rabu",
                items = listOf(
                    ScheduleItemDto(
                        title = "Doctor Elise",
                        slug = "doctor-elise",
                        poster = "https://cdn.myanimelist.net/images/anime/1765/139975l.jpg",
                        time = "20:30 WIB",
                        episode = "12",
                        type = "TV"
                    )
                )
            ),
            ScheduleDayDto(
                day = "Kamis",
                items = listOf(
                    ScheduleItemDto(
                        title = "Dungeon Meshi (Delicious in Dungeon)",
                        slug = "dungeon-meshi",
                        poster = "https://cdn.myanimelist.net/images/anime/1487/138656l.jpg",
                        time = "21:30 WIB",
                        episode = "14",
                        type = "TV"
                    ),
                    ScheduleItemDto(
                        title = "Mato Seihei no Slave",
                        slug = "mato-seihei-no-slave",
                        poster = "https://cdn.myanimelist.net/images/anime/1207/138865l.jpg",
                        time = "22:00 WIB",
                        episode = "12",
                        type = "TV"
                    )
                )
            ),
            ScheduleDayDto(
                day = "Jumat",
                items = listOf(
                    ScheduleItemDto(
                        title = "Sousou no Frieren",
                        slug = "sousou-no-frieren",
                        poster = "https://cdn.myanimelist.net/images/anime/1015/138075l.jpg",
                        time = "22:00 WIB",
                        episode = "28",
                        type = "TV"
                    ),
                    ScheduleItemDto(
                        title = "Mashle: Magic and Muscles S2",
                        slug = "mashle-s2",
                        poster = "https://cdn.myanimelist.net/images/anime/1922/140026l.jpg",
                        time = "23:00 WIB",
                        episode = "11",
                        type = "TV"
                    )
                )
            ),
            ScheduleDayDto(
                day = "Sabtu",
                items = listOf(
                    ScheduleItemDto(
                        title = "Solo Leveling: Arise",
                        slug = "solo-leveling",
                        poster = "https://cdn.myanimelist.net/images/anime/1672/140381l.jpg",
                        time = "23:30 WIB",
                        episode = "12",
                        type = "TV"
                    ),
                    ScheduleItemDto(
                        title = "Kusuriya no Hitorigoto",
                        slug = "kusuriya-no-hitorigoto",
                        poster = "https://cdn.myanimelist.net/images/anime/1708/138033l.jpg",
                        time = "23:05 WIB",
                        episode = "24",
                        type = "TV"
                    )
                )
            ),
            ScheduleDayDto(
                day = "Minggu",
                items = listOf(
                    ScheduleItemDto(
                        title = "One Piece",
                        slug = "one-piece",
                        poster = "https://cdn.myanimelist.net/images/anime/1244/138851l.jpg",
                        time = "09:30 WIB",
                        episode = "1100",
                        type = "TV"
                    ),
                    ScheduleItemDto(
                        title = "Shangri-La Frontier",
                        slug = "shangri-la-frontier",
                        poster = "https://cdn.myanimelist.net/images/anime/1278/138070l.jpg",
                        time = "16:30 WIB",
                        episode = "25",
                        type = "TV"
                    )
                )
            )
        )
        return ScheduleResponseDto(days = days, source = "Samehadaku")
    }

    private fun getFallbackOngoing(): AnimeListResponseDto {
        val items = listOf(
            AnimeDto(
                title = "Solo Leveling",
                slug = "solo-leveling",
                poster = "https://cdn.myanimelist.net/images/anime/1672/140381l.jpg",
                type = "TV",
                status = "Ongoing",
                score = "8.75",
                latestEpisode = "12"
            ),
            AnimeDto(
                title = "One Piece",
                slug = "one-piece",
                poster = "https://cdn.myanimelist.net/images/anime/1244/138851l.jpg",
                type = "TV",
                status = "Ongoing",
                score = "8.89",
                latestEpisode = "1100"
            ),
            AnimeDto(
                title = "Sousou no Frieren",
                slug = "sousou-no-frieren",
                poster = "https://cdn.myanimelist.net/images/anime/1015/138075l.jpg",
                type = "TV",
                status = "Ongoing",
                score = "9.14",
                latestEpisode = "28"
            ),
            AnimeDto(
                title = "Dungeon Meshi",
                slug = "dungeon-meshi",
                poster = "https://cdn.myanimelist.net/images/anime/1487/138656l.jpg",
                type = "TV",
                status = "Ongoing",
                score = "8.40",
                latestEpisode = "18"
            ),
            AnimeDto(
                title = "Mashle: Magic and Muscles S2",
                slug = "mashle-s2",
                poster = "https://cdn.myanimelist.net/images/anime/1922/140026l.jpg",
                type = "TV",
                status = "Ongoing",
                score = "8.12",
                latestEpisode = "10"
            ),
            AnimeDto(
                title = "Wind Breaker",
                slug = "wind-breaker",
                poster = "https://cdn.myanimelist.net/images/anime/1301/141974l.jpg",
                type = "TV",
                status = "Ongoing",
                score = "8.25",
                latestEpisode = "8"
            )
        )
        return AnimeListResponseDto(items = items, source = "Samehadaku")
    }

    private fun getFallbackMovies(): AnimeListResponseDto {
        val items = listOf(
            AnimeDto(
                title = "Demon Slayer: Mugen Train",
                slug = "kimetsu-no-yaiba-mugen-ressha-hen",
                poster = "https://cdn.myanimelist.net/images/anime/1404/109375l.jpg",
                type = "Movie",
                status = "Tamat",
                score = "8.92",
                latestEpisode = "Movie"
            ),
            AnimeDto(
                title = "Jujutsu Kaisen 0",
                slug = "jujutsu-kaisen-0",
                poster = "https://cdn.myanimelist.net/images/anime/1121/119044l.jpg",
                type = "Movie",
                status = "Tamat",
                score = "8.80",
                latestEpisode = "Movie"
            ),
            AnimeDto(
                title = "Suzume no Tojimari",
                slug = "suzume-no-tojimari",
                poster = "https://cdn.myanimelist.net/images/anime/1710/124103l.jpg",
                type = "Movie",
                status = "Tamat",
                score = "8.70",
                latestEpisode = "Movie"
            ),
            AnimeDto(
                title = "Kimi no Na wa (Your Name)",
                slug = "kimi-no-na-wa",
                poster = "https://cdn.myanimelist.net/images/anime/1935/127974l.jpg",
                type = "Movie",
                status = "Tamat",
                score = "9.05",
                latestEpisode = "Movie"
            )
        )
        return AnimeListResponseDto(items = items, source = "Samehadaku")
    }
}
