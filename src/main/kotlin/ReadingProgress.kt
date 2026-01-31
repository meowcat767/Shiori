package reading

/**
 * Represents reading progress for a manga chapter.
 * Stores the last read page for each manga/chapter combination.
 */
data class ReadingProgress(
    val mangaId: String,
    val chapterId: String,
    val pageIndex: Int = 0,
    val lastReadAt: Long = System.currentTimeMillis()
)

