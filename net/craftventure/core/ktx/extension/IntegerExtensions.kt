package net.craftventure.core.ktx.extension


fun Int.asOrdinal(): String {
    val suffixes = arrayOf("th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th")
    return when (this % 100) {
        11, 12, 13 -> "th"
        else -> suffixes[this % 10]
    }
}

fun Long.toRangedInteger(): Long {
    if (this >= Long.MAX_VALUE)
        return Long.MAX_VALUE - 1
    if (this <= Long.MIN_VALUE)
        return Long.MIN_VALUE + 1
    return this
}

fun Int.asOrdinalAppended(): String {
    return this.toString() + this.asOrdinal()
}