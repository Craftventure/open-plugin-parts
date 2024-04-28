package net.craftventure.chat.bungee.util

import com.linkedin.urls.Url
import net.craftventure.chat.bungee.span.*
import net.craftventure.core.ktx.logging.logException
import net.craftventure.core.ktx.util.Logger
import net.craftventure.database.extension.aliasRegex
import net.craftventure.database.generated.cvdata.tables.pojos.Emoji

object AnnotatedChatUtils {
    const val DEBUG_PARSING = false
    val commandStart = "/".toRegex()
    val secondaryEmojis = """:(?<emoji>(\w+)):""".toRegex()
    val playerTagRegex =
        """(?<name>[a-zA-Z0-9_]{0,16})\b@""".toRegex()

    val unicodeCustomBlocks = """\p{Co}""".toRegex()
    val emojiRegex =
        """(?:[\u2700-\u27bf]|(?:[\ud83c\udde6-\ud83c\uddff]){2}|[\ud800\udc00-\uDBFF\uDFFF]|[\u2600-\u26FF])[\ufe0e\ufe0f]?(?:[\u0300-\u036f\ufe20-\ufe23\u20d0-\u20f0]|[\ud83c\udffb-\ud83c\udfff])?(?:\u200d(?:[^\ud800-\udfff]|(?:[\ud83c\udde6-\ud83c\uddff]){2}|[\ud800\udc00-\uDBFF\uDFFF]|[\u2600-\u26FF])[\ufe0e\ufe0f]?(?:[\u0300-\u036f\ufe20-\ufe23\u20d0-\u20f0]|[\ud83c\udffb-\ud83c\udfff])?)*|[\u0023-\u0039]\ufe0f?\u20e3|\u3299|\u3297|\u303d|\u3030|\u24c2|[\ud83c\udd70-\ud83c\udd71]|[\ud83c\udd7e-\ud83c\udd7f]|\ud83c\udd8e|[\ud83c\udd91-\ud83c\udd9a]|[\ud83c\udde6-\ud83c\uddff]|[\ud83c\ude01-\ud83c\ude02]|\ud83c\ude1a|\ud83c\ude2f|[\ud83c\ude32-\ud83c\ude3a]|[\ud83c\ude50-\ud83c\ude51]|\u203c|\u2049|[\u25aa-\u25ab]|\u25b6|\u25c0|[\u25fb-\u25fe]|\u00a9|\u00ae|\u2122|\u2139|\ud83c\udc04|[\u2600-\u26FF]|\u2b05|\u2b06|\u2b07|\u2b1b|\u2b1c|\u2b50|\u2b55|\u231a|\u231b|\u2328|\u23cf|[\u23e9-\u23f3]|[\u23f8-\u23fa]|\ud83c\udccf|\u2934|\u2935|[\u2190-\u21ff]""".toRegex()

    var emojis = mapOf<String, String>()

    fun AnnotatedChat.filterCurseWords(words: List<Regex>) {
        words.forEach {
            val occurences = it.findAll(this.text)
            occurences.forEach {
//                logcat { "Result ${it.value} ${it.groups.map { it?.range }}" }
                this.setSpan(CurseWordChatSpan(it.range.start, it.range.endInclusive))
            }
        }
    }

    fun AnnotatedChat.handleEmojis(permissionCheck: (String) -> Boolean, emojis: List<Emoji>) {
        emojis.forEach { emoji ->
            emoji.aliasRegex.forEach { aliasRegex ->
                val occurences = aliasRegex.findAll(this.text)
                occurences.forEach { occurence ->
                    this.setSpan(EmojiChatSpan(occurence.range.start, occurence.range.endInclusive, emoji))
                }
            }
        }
        val occurences = emojiRegex.findAll(this.text)
        occurences.forEach { occurence ->
//            logcat { "Found match ${occurence.value} at index ${occurence.range.start}-${occurence.range.endInclusive}" }
            this.setSpan(UnicodeEmojiChatSpan(occurence.range.start, occurence.range.endInclusive))
        }
    }

    fun AnnotatedChat.handleTwemojis() {
        secondaryEmojis.findAll(this.text).forEach { occurence ->
            try {
                if (occurence.groups.get(0)!!.value in emojis) {
                    this.setSpan(TwemojiSpan(occurence.range.start, occurence.range.endInclusive))
                }
            } catch (e: Exception) {
                logException(e)
            }
        }
    }

    fun AnnotatedChat.handleUrls(messageUrls: List<Url>) {
        messageUrls.forEach { url ->
            val originalUrl = url.originalUrl
            if (DEBUG_PARSING)
                Logger.debug("Matching $originalUrl")
            Regex.escape(originalUrl).toRegex().findAll(this.text).forEach {
                if (DEBUG_PARSING)
                    Logger.debug("Matched URL $originalUrl")
                this.setSpan(UrlChatSpan(it.range.start, it.range.endInclusive))
            }
        }
    }
}