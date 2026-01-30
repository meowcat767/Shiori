package model

data class Bookmark(
    val mangaId: String,
    val mangaTitle: String,
    val chapterId: String,
    val chapterTitle: String?,
    val page: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)