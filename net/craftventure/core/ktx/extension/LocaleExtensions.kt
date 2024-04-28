package net.craftventure.core.ktx.extension

import java.util.*

fun String.parseAsLocale() = this.replace('_', '-').let {
    try {
        Locale.forLanguageTag(it)
    } catch (e: Exception) {
        null
    }
}