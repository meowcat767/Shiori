package bookmark

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.nio.file.Files
import java.nio.file.Path
import model.Bookmark
import model.*;


class BookmarkStore(
    private val file: Path
) {
    private val mapper = ObjectMapper()
        .registerModule(KotlinModule())

    private var bookmarks: MutableList<Bookmark> = load()

    fun add(bookmark: Bookmark) {
        bookmarks.removeIf { it.mangaId == bookmark.mangaId }
        bookmarks.add(bookmark)
        save()
    }

    fun remove(mangaId: String) {
        bookmarks.removeIf { it.mangaId == mangaId }
        save()
    }

    fun find(mangaId: String): Bookmark? =
        bookmarks.find { it.mangaId == mangaId }

    private fun load(): MutableList<Bookmark> {
        if (!Files.exists(file)) return mutableListOf()
        return mapper.readValue(
            file.toFile(),
            mapper.typeFactory.constructCollectionType(
                MutableList::class.java,
                Bookmark::class.java
            )
        )
    }

    private fun save() {
        Files.createDirectories(file.parent)
        mapper.writerWithDefaultPrettyPrinter()
            .writeValue(file.toFile(), bookmarks)
    }
}