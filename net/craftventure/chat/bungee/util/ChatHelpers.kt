package net.craftventure.chat.bungee.util

import net.craftventure.chat.bungee.extension.containingUrls
import net.craftventure.chat.core.extension.isAllowed


object ChatHelpers {
    const val DEBUG_COMPILING = false

    fun replaceDisallowedUrls(message: String): String {
        var newMessage = message
        val messageUrls = newMessage.containingUrls()
        messageUrls.forEach { url ->
            if (!url.isAllowed()) {
//                Logger.debug("URL not allowed ${url.originalUrl}")
                newMessage = newMessage.replace(url.originalUrl, "***")//generateGrawlix(url.originalUrl.length))
            }
        }
        return newMessage
    }

    // item span: [item:pirate_hat]
}