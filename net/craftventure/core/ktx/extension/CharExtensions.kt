package net.craftventure.core.ktx.extension

fun Char.escapeUnicode() = "\\u${String.format("%04X", this.code)}"