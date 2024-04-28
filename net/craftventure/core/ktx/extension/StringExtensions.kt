package net.craftventure.core.ktx.extension

import net.craftventure.core.ktx.util.Logger
import java.text.Normalizer
import java.util.*

fun <T : CharSequence> T.takeIfNotBlank() = this.takeIf { it.isNotBlank() }
inline fun String.broadcastAsDebugTimings() = Logger.info(this)
fun String?.asUuid(): UUID? {
    if (this != null) {
        try {
            return UUID.fromString(this)
        } catch (e: Exception) {
        }
    }
    return null
}

private val REGEX_UNACCENT = "\\p{InCombiningDiacriticalMarks}+".toRegex()

fun <T : CharSequence> T.deAccent(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    return REGEX_UNACCENT.replace(temp, "")
}

fun Char.deAccent(): Char {
    val temp = Normalizer.normalize(this.toString(), Normalizer.Form.NFD)
    return REGEX_UNACCENT.replace(temp, "").first()
}