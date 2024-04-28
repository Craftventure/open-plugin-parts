package net.craftventure.bukkit.ktx.manager

import net.craftventure.bukkit.ktx.manager.MessageBarMeta.Companion.get
import net.craftventure.bukkit.ktx.plugin.PluginProvider.getInstance
import net.craftventure.chat.bungee.util.FontUtils.width
import net.craftventure.chat.bungee.util.FontUtils.wrapInUnderlay
import net.craftventure.chat.core.util.SpaceHelper
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import kotlin.math.max

object MessageBarManager {
    const val HOTBAR_WIDTH = 178
    private var hasInitialised = false

    @JvmStatic
    var vcGetter: ((Player) -> Long)? = null

    fun generateVcDisplay(player: Player): TextComponent? {
        val vcGetter = vcGetter ?: return null
        if (player.remainingAir != player.maximumAir) return null
        if (player.spectatorTarget != null) return null
        if (player.gameMode == GameMode.CREATIVE && player.inventory.itemInMainHand.type == Material.DEBUG_STICK) return null
        return generateVcDisplay(vcGetter(player))
    }

    private fun generateVcDisplay(amount: Long): TextComponent {
        val characters = amount.toString()
        // 6 chars per digit + 1 space + 8 for VC icon
        val totalCharactersWidth = width(characters) + 9 /*+ 1*/ // emoji=8 + suffix=1
        // Hotbar width = 180 "px"
        val offset = max(HOTBAR_WIDTH - totalCharactersWidth, 1)

//        Logger.debug("Offset $offset t=${(offset / 2)}")

        val component = Component.text()
            .append(Component.text(SpaceHelper.width(offset)))
            .append(Component.text("${characters.map { '\uE061' + (it - '0') }.joinToString("")}\uE02A"))
            .build()
//        Logger.debug(GsonComponentSerializer.gson().serialize(component))
        return component
    }

    @JvmStatic
    fun init() {
        if (!hasInitialised) {
            hasInitialised = true
            Bukkit.getScheduler().scheduleSyncRepeatingTask(getInstance(), {
                for (player in Bukkit.getOnlinePlayers()) {
                    updateMessagesForPlayer(player)
                }
            }, 20, 20)
        }
    }

    fun require(player: Player) {
        trigger(player)
    }

    fun trigger(player: Player) {
        updateMessagesForPlayer(player)
    }

    private fun updateMessagesForPlayer(player: Player) {
        val metaData = get(player)
        if (metaData != null) {
            metaData.removeInvalidMessages()
            val messageList = metaData.getMessageList()
            val firstMessage = messageList.firstOrNull()

            if (firstMessage != null) {
                handleMessage(firstMessage, player)
            } else if (!displayVc(player)) {
//                logcat { "Remove instantly" }
                player.sendActionBar(Component.empty())
            }
        }
    }

    private fun shouldDisplayCoinsTo(player: Player): Boolean {
        return player.gameMode != GameMode.SPECTATOR
    }

    private fun displayVc(player: Player): Boolean {
        if (!shouldDisplayCoinsTo(player)) return false
//        if (true) return
        val component = generateVcDisplay(player) ?: return false
        player.sendActionBar(component)
        return true
    }

    private fun handleMessage(message: Message?, player: Player) {
//        if (true) return
        val vcDisplay = generateVcDisplay(player)
        if (vcDisplay == null || !shouldDisplayCoinsTo(player)) {
            if (message != null)
                player.sendActionBar(wrapInUnderlay(message.text))
            return
        }
        val displayText = message?.text
        if (displayText != null) {
            val wrappedText = wrapInUnderlay(displayText)
            val width = width(displayText) + 13 // 13 for underlay: start=6 + end=6 + suffix=1 = 13
            val widthCoins = HOTBAR_WIDTH

            if (widthCoins > width) {
                val difference = widthCoins - width
                val halfDifference = difference / 2

//                Logger.debug("widthCoins=$widthCoins width=$width difference=$difference halfDifference=$halfDifference")

                val newMessage = vcDisplay
                    .append(Component.text(SpaceHelper.width(-widthCoins + halfDifference, noSplit = false)))
                    .append(wrappedText)
                    .append(Component.text(SpaceHelper.width(halfDifference, noSplit = false)))
                player.sendActionBar(newMessage)
            } else {
                val difference = width - widthCoins
                val halfDifference = difference / 2

//                Logger.debug("widthCoins=$widthCoins width=$width difference=$difference halfDifference=$halfDifference")

                val newMessage = wrappedText
                    .append(Component.text(SpaceHelper.width(-width + halfDifference, noSplit = false)))
                    .append(vcDisplay)
                    .append(Component.text(SpaceHelper.width(halfDifference, noSplit = false)))
                player.sendActionBar(newMessage)
            }
        } else {
            player.sendActionBar(vcDisplay)
        }
    }

    @JvmStatic
    fun remove(player: Player, id: String) {
        val meta = get(player) ?: return
        if (meta.removeById(id)) {
            updateMessagesForPlayer(player)
        }
    }

    @JvmStatic
    fun remove(player: Player, type: Type) {
        val meta = get(player) ?: return
        if (meta.removeByType(type)) {
            updateMessagesForPlayer(player)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun display(player: Player, message: Message, replace: Boolean = false) {
        val metaData = get(player) ?: return
        if (metaData.addMessage(message, replace)) {
            trigger(player)
        }
    }

    @JvmStatic
    @JvmOverloads
    @Deprecated(
        "Use message one instead", replaceWith = ReplaceWith(
            "display(\n" +
                    "            player,\n" +
                    "            Message(\n" +
                    "                id = id,\n" +
                    "                text = text,\n" +
                    "                type = type,\n" +
                    "                untilMillis = untilMillis,\n" +
                    "            ),\n" +
                    "            replace = replace,\n" +
                    "        )"
        )
    )
    fun display(
        player: Player,
        text: Component,
        type: Type,
        untilMillis: Long,
        id: String,
        replace: Boolean = true,
    ) {
        display(
            player,
            Message(
                id = id,
                text = text,
                type = type,
                untilMillis = untilMillis,
            ),
            replace = replace,
        )
    }

    enum class Type(val value: Int) {
        ERROR(Int.MAX_VALUE),
        SHUTDOWN(10099),
        WARNING(10000),
        VEHICLE_EXIT_BLOCKED(1005),
        KART_EXIT(1004),
        SHOP_PRESS_F_BUY_MESSAGE(1003),
        QUEUE(1002),
        RIDE(1001),
        NOTICE(1000),
        CASINO(102),
        MINIGAME(85),
        SPEEDOMETER(80),
        AUDIOSERVER_AREA(75),
    }

    class Message(
        val id: String,
        val text: Component,
        val type: Type,
        val untilMillis: Long,
    ) {
        val since: Long = System.currentTimeMillis()
        fun isValid() = untilMillis > System.currentTimeMillis()

        companion object {
            val messageComparator: Comparator<Message> =
                compareByDescending<Message> { it.type.value }.thenBy { it.untilMillis }
        }
    }
}