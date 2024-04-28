package net.craftventure.database.extension

import net.craftventure.database.generated.cvdata.tables.pojos.PlayerLocale
import java.util.*

fun PlayerLocale.getLocale(fallback: Locale? = Locale.getDefault()): Locale? = locale?.let {
    Locale.forLanguageTag(it.replace("_", "-"))
} ?: fallback

fun PlayerLocale.setLocale(locale: Locale) {
    this.locale = locale.let { Locale(it.language, it.country) }.toLanguageTag().replace("-", "_")
}