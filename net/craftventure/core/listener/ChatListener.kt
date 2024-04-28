package net.craftventure.core.listener

import io.papermc.paper.event.player.AsyncChatEvent
import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.chat.bungee.extension.append
import net.craftventure.chat.bungee.extension.containingUrls
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.AnnotatedChat
import net.craftventure.chat.bungee.util.AnnotatedChatUtils
import net.craftventure.chat.bungee.util.AnnotatedChatUtils.filterCurseWords
import net.craftventure.chat.bungee.util.AnnotatedChatUtils.handleEmojis
import net.craftventure.chat.bungee.util.AnnotatedChatUtils.handleTwemojis
import net.craftventure.chat.bungee.util.AnnotatedChatUtils.handleUrls
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.ChatHelpers
import net.craftventure.chat.core.extension.capitalizeForChat
import net.craftventure.chat.core.extension.capitalizeSentences
import net.craftventure.chat.core.extension.cursewordFilters
import net.craftventure.chat.core.extension.fixCommonTypos
import net.craftventure.core.chat.SpanHelpers.handlePlayerTags
import net.craftventure.core.chat.SpanHelpers.handleRideTags
import net.craftventure.core.extension.canUseChat
import net.craftventure.core.extension.canUseCommand
import net.craftventure.core.extension.smokeTrees
import net.craftventure.core.ktx.util.Logger
import net.craftventure.database.MainRepositoryProvider
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.query.QueryOptions
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent


class ChatListener : Listener {
    protected val DEBUG_PARSING = false
    protected val DEBUG_COMPILING = false

    private val chatFilter =
        """(?!\p{Graph}|\p{Digit}|\p{Punct}|\p{IsLatin}|\p{IsGreek}|\p{InArabic}|\p{IsCyrillic}| )""".toRegex()

    fun isMessageAllowedBy(message: String, player: Player): Boolean {
//        Logger.console("Checking %s", message)
        if (!player.isCrew()) {
//            val textonly = message.replace("[^A-Za-z ]+".toRegex(), "").toLowerCase().trim()
//            val textonlyStripped = StringUtils.stripAccents(textonly)

            if (!message.startsWith("/") && !chatEnabled) {
                player.sendMessage(Translation.CHAT_DISABLED.getTranslation(player)!!)
                return false
            }/* else if (message.containsDisallowedDomains()) {
                player.sendMessage(Translation.CHAT_ADVERTISEMENT.getTranslation(player)!!)
                report(message, player, Type.ADVERTISEMENT)
                return false
            } else if (message.containsSlang() || textonly.containsSlang() || textonlyStripped.containsSlang()) {
                player.sendMessage(Translation.CHAT_CURSEWORD.getTranslation(player)!!)
                report(message, player, Type.CURSE)
                return false
            }*/
        }
        return true
    }

    fun handleBaseEvent(cancellable: Cancellable, message: String, player: Player) {
        if (!isMessageAllowedBy(message, player)) {
            cancellable.isCancelled = true
        } else if (message.startsWith("/") && !player.canUseCommand(true)) {
            cancellable.isCancelled = true
        } else if (!message.startsWith("/") && !player.canUseChat(true)) {
            cancellable.isCancelled = true
        }

//        if(message.contains("CraftVenture")) {
//
//        }

        if (message.contains("smoke trees", true)) {
            player.smokeTrees()
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerCommandPreprocess(event: PlayerCommandPreprocessEvent) {
        var message = event.message
        val player = event.player

        handleBaseEvent(event, message, player)
        if (event.isCancelled) return

        if (!player.isCrew()) {
            message = message.capitalizeForChat()
        }

//        event.message = message.formatForChat()
        event.message = message
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    fun onAsyncPlayerChat(event: AsyncChatEvent) {
        val player = event.player

        handleBaseEvent(
            event, PlainTextComponentSerializer.plainText().serialize(event.originalMessage())
                .replace(AnnotatedChatUtils.unicodeCustomBlocks, ""), player
        )
        if (event.isCancelled) return

        val parsedResult = handleChatMessage(player, event.originalMessage())
        event.message(parsedResult.fullMessage)
        event.renderer { source, sourceDisplayName, message, audience -> message }

        Audience.audience(event.viewers()).sendMessage(event.player, parsedResult.fullMessage)
        event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    fun onChatEnabledCheck(event: AsyncChatEvent) {
        if (!event.player.isCrew() && !chatEnabled) {
            event.isCancelled = true
            event.player.sendMessage(Translation.CHAT_DISABLED.getTranslation(event.player)!!)
        }
    }

//    @EventHandler
//    fun onAsyncPlayerChatPreviewEvent(event: AsyncChatDecorateEvent) {
//        val newMessage = handleChatMessage(event.player(), event.originalMessage())
//        event.result(newMessage.component)
//    }

    private fun handleChatMessage(player: Player?, eventMessage: Component): ParsedChatResult {
        var message = PlainTextComponentSerializer.plainText().serialize(eventMessage)
            .replace(AnnotatedChatUtils.unicodeCustomBlocks, "")
        if (message.length > 1 && message.startsWith(".")) message = message.substring(1)

        message = if (player != null && !player.isCrew()) {
            message.capitalizeForChat()
        } else {
            message.capitalizeSentences()
        }
        message = message.fixCommonTypos()


        var nameComponent: Component? = null
        var messageComponent: Component
        val api = LuckPermsProvider.get()
        val user =
            if (player != null) api.userManager.getUser(player.uniqueId) ?: api.userManager.loadUser(player.uniqueId)
                .join() else null
        if (user != null && player != null) {
            val contextManager = api.contextManager
            val contexts = contextManager.getContext(user).orElse(contextManager.staticContext)

            val prefix =
                user.cachedData.getMetaData(QueryOptions.contextual(contexts)).prefix?.let {
                    PlainTextComponentSerializer.plainText().deserialize(it)
                } ?: Component.text("", NamedTextColor.WHITE)
            val suffix =
                user.cachedData.getMetaData(QueryOptions.contextual(contexts)).suffix?.let {
                    PlainTextComponentSerializer.plainText().deserialize(it)
                } ?: Component.text("", CVTextColor.subtle)
            val chatColorMetaValue =
                user.cachedData.getMetaData(QueryOptions.contextual(contexts)).getMetaValue("chatcolor")
            val chatColor = chatColorMetaValue?.let { TextColor.fromHexString(it) } ?: CVTextColor.subtle

            val rankColorMetaValue =
                user.cachedData.getMetaData(QueryOptions.contextual(contexts)).getMetaValue("rankcolor")
            val rankColor = rankColorMetaValue?.let { TextColor.fromHexString(it) } ?: NamedTextColor.WHITE

            nameComponent = Component.text("", rankColor).append(prefix).append(player.name).append(suffix)
            messageComponent = Component.text("", chatColor)
        } else if (player != null) {
            nameComponent = Component.text(player.name, NamedTextColor.WHITE)
            messageComponent = Component.text("", NamedTextColor.WHITE)
        } else {
            nameComponent = null
            messageComponent = Component.empty()
        }

        val url = "https://translate.google.com/#view=home&op=translate&sl=auto&tl=en".toHttpUrl()
            .newBuilder()
            .addQueryParameter("text", message)
            .build()

        nameComponent = nameComponent
            ?.hoverEvent(Component.text("Click to open translation URL", CVTextColor.CHAT_HOVER))
            ?.clickEvent(ClickEvent.openUrl(url.toString()))

        if (player != null && !player.isCrew())
            message = ChatHelpers.replaceDisallowedUrls(message)

        val messageUrls = message.containingUrls()

        // TODO: Caps test must be < 30%
//        val upperCase = message.count { it.isUpperCase() }
//        val lowerCase = message.count { it.isLowerCase() }
        val annotatedChat = AnnotatedChat(message)

        annotatedChat.filterCurseWords(cursewordFilters)

//        if (player.hasPermission("craftventure.chat.color")) annotatedChat.handleColors(chatColor)
        if (player == null || player.hasPermission("craftventure.chat.span.url")) annotatedChat.handleUrls(messageUrls)
        if (player == null || player.hasPermission("craftventure.chat.span.tagplayer")) annotatedChat.handlePlayerTags()
        if (player == null || player.hasPermission("craftventure.chat.span.tagride")) annotatedChat.handleRideTags()
        if (player == null || player.hasPermission("craftventure.chat.span.emoji")) annotatedChat.handleEmojis(
            { player!!.hasPermission(it) },
            MainRepositoryProvider.emojiRepository.cachedItems
        )
        annotatedChat.handleTwemojis()

        if (DEBUG_PARSING)
            Logger.debug("$message with spans [${annotatedChat.spans.joinToString(", ")}] and parts")

        val annotatedMessageComponent = annotatedChat.appendTo(Component.text(""))
        return ParsedChatResult(
            messageComponent.append(annotatedMessageComponent),
            nameComponent,
        )

    }

    data class ParsedChatResult(
        val component: Component,
        val nameComponent: Component?,
    ) {
        val fullMessage = if (nameComponent != null) {
            Component.empty() + nameComponent + component
        } else
            component

    }

    companion object {
        var chatEnabled: Boolean = true
    }
}