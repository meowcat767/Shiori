package reading

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.nio.file.Files
import java.nio.file.Path

/**
 * Stores reading progress for manga chapters.
 * Progress is keyed by mangaId + chapterId combination.
 */
class ReadingProgressStore(
    private val file: Path
) {
    private val mapper = ObjectMapper()
        .registerModule(KotlinModule())

    private var progressMap: MutableMap<String, ReadingProgress> = load()

    /**
     * Save reading progress for a specific manga and chapter
     */
    fun saveProgress(mangaId: String, chapterId: String, pageIndex: Int) {
        val key = generateKey(mangaId, chapterId)
        progressMap[key] = ReadingProgress(
            mangaId = mangaId,
            chapterId = chapterId,
            pageIndex = pageIndex,
            lastReadAt = System.currentTimeMillis()
        )
        save()
    }

    /**
     * Get saved page index for a manga and chapter
     */
    fun getPageIndex(mangaId: String, chapterId: String): Int {
        val key = generateKey(mangaId, chapterId)
        return progressMap[key]?.pageIndex ?: 0
    }

    /**
     * Check if progress exists for a manga and chapter
     */
    fun hasProgress(mangaId: String, chapterId: String): Boolean {
        val key = generateKey(mangaId, chapterId)
        return progressMap.containsKey(key)
    }

    /**
     * Clear progress for a specific manga (all chapters)
     */
    fun clearMangaProgress(mangaId: String) {
        progressMap.keys.removeIf { it.startsWith(mangaId) }
        save()
    }

    /**
     * Clear all reading progress
     */
    fun clearAll() {
        progressMap.clear()
        save()
    }

    private fun generateKey(mangaId: String, chapterId: String): String {
        return "$mangaId:$chapterId"
    }

    @Suppress("UNCHECKED_CAST")
    private fun load(): MutableMap<String, ReadingProgress> {
        if (!Files.exists(file)) return mutableMapOf()

        return try {
            val list: List<ReadingProgress> = mapper.readValue(
                file.toFile(),
                mapper.typeFactory.constructCollectionType(
                    List::class.java,
                    ReadingProgress::class.java
                )
            )
            list.associateTo(mutableMapOf()) { generateKey(it.mangaId, it.chapterId) to it }
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    private fun save() {
        Files.createDirectories(file.parent)
        mapper.writerWithDefaultPrettyPrinter()
            .writeValue(file.toFile(), progressMap.values.toList())
    }
}

