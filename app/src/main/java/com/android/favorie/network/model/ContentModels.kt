package com.android.favorie.network.model

// GET /contents/movies/search 응답
data class MovieSearchResult(
    val tmdbId: Int,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val releaseYear: Int?,
    val genre: String?
)

// GET /contents/movies/search 래퍼
data class MovieSearchResponse(
    val results: List<MovieSearchResult>,
    val totalResults: Int
)

// GET /contents/movies/{tmdbId} 응답
data class MovieDetail(
    val contentId: Long?,
    val tmdbId: String,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val director: String?,
    val genre: String?,
    val releaseYear: Int?
)

// GET /contents/books/search 응답
data class BookSearchResult(
    val bookId: String,
    val title: String,
    val author: String?,
    val imageUrl: String?,
    val genre: String?,
    val pubDate: String?
)

// GET /contents/books/search 래퍼
data class BookSearchResponse(
    val results: List<BookSearchResult>,
    val totalResults: Int
)

// GET /contents/books/{bookId} 응답
data class BookDetail(
    val contentId: Long?,
    val bookId: String,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val author: String?,
    val genre: String?,
    val pubDate: String?
)

// GET /contents/musics/search 결과 항목
data class MusicSearchResult(
    val spotifyId: String,
    val title: String,
    val artist: String?,
    val imageUrl: String?,
    val album: String?
)

// GET /contents/musics/search 래퍼
data class MusicSearchResponse(
    val results: List<MusicSearchResult>,
    val totalResults: Int
)

// GET /contents/musics/{spotifyId} 응답
data class MusicDetail(
    val contentId: Long?,
    val spotifyId: String,
    val title: String,
    val imageUrl: String?,
    val artist: String?,
    val album: String?
)