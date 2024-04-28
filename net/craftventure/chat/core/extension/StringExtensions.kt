package net.craftventure.chat.core.extension

import net.craftventure.core.ktx.extension.random
import java.util.*
import java.util.regex.Pattern

val slangWords2 = arrayOf(
    // Removed this list before Github deletes my repo
)
val cursewordFilters = slangWords2.map { it.toRegex(RegexOption.IGNORE_CASE) }

val grawlixChars = arrayOf('*')//'@', '#', '$', '%', '&', '!')
fun generateGrawlix(length: Int) = generateSequence {
    grawlixChars.random()
}.take(length).joinToString("")


// Can't have people typing CraftVenture of Craft Venture can we? If they don't learn, we force them
fun String.fixCommonTypos(): String {
    return this
        .replace("(?i)craft venture".toRegex(), "Craftventure")
        .replace("(?i)craftventure".toRegex(), "Craftventure")
        .replace("(?i)http".toRegex(), "http")
}

fun String.booleanValue(): Boolean =
    equals("true", ignoreCase = true) || this == "1" || equals("yes", ignoreCase = true)

//private val accentStrippingPattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")//$NON-NLS-1$
val sentencePattern = Pattern.compile("\\.\\s+\\w")
fun String.capitalizeSentences(): String {
    val m = sentencePattern.matcher(this)
    val buf = StringBuffer()
    while (m.find())
        m.appendReplacement(buf, m.group().toUpperCase())
    m.appendTail(buf)
    return buf.toString().substring(0, 1).toUpperCase() + buf.toString().substring(1)
}

fun String.capitalizeForChat(): String {
    val caps = (indices).count { Character.isUpperCase(get(it)) }

    if (caps > length * 0.3 + 1)
        return (substring(0, 1).toUpperCase() + substring(1).lowercase(Locale.getDefault())).capitalizeSentences()
    return this.capitalizeSentences()
}