package net.craftventure.chat.core.extension

import com.linkedin.urls.Url


fun Url.isAllowed(): Boolean {
    return when {
        this.host.contains("youtube.com", ignoreCase = true) -> true
        this.host.contains("youtube-nocookie.com", ignoreCase = true) -> true
        this.host.contains("youtu.be", ignoreCase = true) -> true
        this.host.contains("craftventure.net", ignoreCase = true) -> true
        this.host.contains("spotify.com", ignoreCase = true) -> true
        this.host.contains("reddit.com", ignoreCase = true) -> true
        this.host.contains("twitter.com", ignoreCase = true) -> true
        this.host.contains("gyazo.com", ignoreCase = true) -> true
        this.host.contains("prnt.sc", ignoreCase = true) -> true
        this.host.contains("optifine.net", ignoreCase = true) -> true
        this.host.contains("gitlab.com", ignoreCase = true) -> true
        this.host.contains("github.com", ignoreCase = true) -> true
        else -> false
    }
}
