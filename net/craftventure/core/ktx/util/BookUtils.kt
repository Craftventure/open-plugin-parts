package net.craftventure.core.ktx.util

object BookUtils {
    val PAGE_WIDTH = 114
    val PAGE_LINES = 14

    fun textWidth(word: String): Int = GlyphSizes.calculateWidth(word)
}