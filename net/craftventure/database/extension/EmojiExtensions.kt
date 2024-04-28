package net.craftventure.database.extension

import net.craftventure.database.generated.cvdata.tables.pojos.Emoji

val Emoji.actualAliases get() = aliases!!.split(" ")
val Emoji.regex get() = Regex.escape(emoji!!).toRegex()
val Emoji.aliasRegex get() = actualAliases.map { Regex.escape(it).toRegex(RegexOption.IGNORE_CASE) }