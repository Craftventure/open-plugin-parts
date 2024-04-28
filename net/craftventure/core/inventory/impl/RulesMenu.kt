package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.CvComponent
import net.craftventure.chat.bungee.util.FontCodes
import net.craftventure.core.async.executeAsync
import net.craftventure.core.inventory.InventoryMenu
import net.craftventure.database.MainRepositoryProvider
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class RulesMenu(
    player: Player
) : InventoryMenu(
    owner = player,
) {
    val rules = listOf(

        Rule(
            CVTextColor.MENU_DEFAULT_TITLE + "Be respectful",
            CVTextColor.MENU_DEFAULT_LORE + "Please be respectful to other people and don't curse in any kind of way"
        ),

        Rule(
            CVTextColor.MENU_DEFAULT_TITLE + "Don't advertise",
            CVTextColor.MENU_DEFAULT_LORE + "Don't post any kind of reference to another server in the (private) chat"
        ),

        Rule(
            CVTextColor.MENU_DEFAULT_TITLE + "Don't abuse bugs",
            CVTextColor.MENU_DEFAULT_LORE + "Don't abuse bugs/glitches and use cheats/hacks, but report them instead"
        ),

        Rule(
            CVTextColor.MENU_DEFAULT_TITLE + "No spam",
            CVTextColor.MENU_DEFAULT_LORE + "Not in global chat... not in private chat.. All (private) chats are (partly automatically) monitored/scanned!"
        ),

        Rule(
            CVTextColor.MENU_DEFAULT_TITLE + "Don't offend people",
            CVTextColor.MENU_DEFAULT_LORE + "Keep your chat civilized and don't offend people, harass or threaten them"
        ),

        Rule(
            CVTextColor.MENU_DEFAULT_TITLE + "No coinfarming",
            CVTextColor.MENU_DEFAULT_LORE + "When we see someone farming we reserve the right for a VentureCoin reset and other measures such as removing all your owned items"
        ),

        Rule(
            CVTextColor.MENU_DEFAULT_TITLE + "No trolling",
            CVTextColor.MENU_DEFAULT_LORE + "Just don't troll. Don't intentionally provoke people in the server into an emotional response"
        ),

        Rule(
            CVTextColor.MENU_DEFAULT_TITLE + "Don't scam",
            CVTextColor.MENU_DEFAULT_LORE + "Don't scam other people. This includes things like impersonating owners"
        ),

        Rule(
            CVTextColor.MENU_DEFAULT_TITLE + "Inappropriate content",
            CVTextColor.MENU_DEFAULT_LORE + "Keep the server children friendly. Don't post links to any kind of mature stuff or any other kind of inappropriate content"
        ),

        Rule(
            CVTextColor.MENU_DEFAULT_TITLE + "Use common sense",
            CVTextColor.MENU_DEFAULT_LORE + "If something feels wrong, it probably is wrong"
        ),

        Rule(
            CVTextColor.MENU_DEFAULT_TITLE + "Don't try to fraud",
            CVTextColor.MENU_DEFAULT_LORE + "We regularly get people claiming 'I've bought X' while they didn't. We reserve the right to (temp)ban any who attempt to fraud"
        ),

        Rule(
            CVTextColor.MENU_DEFAULT_TITLE + "Don't share personal information",
            CVTextColor.MENU_DEFAULT_LORE + "For your own safety, you might not want to share your personal information to others"
        ),

        Rule(
            CVTextColor.MENU_DEFAULT_TITLE + "Don't use macro's or any other automation",
            CVTextColor.MENU_DEFAULT_LORE + "The use of any of those might lead to resets in things like VentureCoins, ridecounters, items and others"
        ),

        Rule(
            CVTextColor.MENU_DEFAULT_TITLE + "Don't try to circumvent server systems",
            CVTextColor.MENU_DEFAULT_LORE + "It is not allowed to (or trying to) circumvent server systems such as for example the AFK-mode or anti-cheating systems"
        ),

        Rule(
            CVTextColor.MENU_DEFAULT_TITLE + "The owners are always right",
            CVTextColor.MENU_DEFAULT_LORE + "Sometimes our guidelines may not be specific enough to deal with your situation, in that case the owners are always right"
        ),

        Rule(
            CVTextColor.MENU_DEFAULT_TITLE + "Not following our guidelines?",
            CVTextColor.MENU_DEFAULT_LORE + "Not following our guidelines may lead to a warning, kick, ban or any other action"
        ),

        Rule(
            CVTextColor.MENU_DEFAULT_TITLE + "No excuse",
            CVTextColor.MENU_DEFAULT_LORE + "\"I didn't read the rules so I didn't know that\"" + Component.newline() + Component.newline() +
                    "is NO excuse!"
        )
    )

    init {
        rowsCount = 3
        underlay = CvComponent.resettingInventoryOverlay(
            FontCodes.Inventory.emptyRowUnderlay.row1,
        )

        titleComponent = centeredTitle("Da rules")

        executeAsync {
            MainRepositoryProvider.achievementProgressRepository.reward(player.uniqueId, "rules_reader")
        }
    }

    override fun onItemClicked(inventory: Inventory, position: Int, player: Player, action: InventoryAction) {
        if (handleNavigationButtonsClick(inventory, position, player, action)) return
    }

    override fun onLayout(inventory: Inventory) {
        addNavigationButtons(inventory)

        var index = 9

        for (rule in rules) {
            inventory.setItem(
                index++, ItemStack(Material.WRITABLE_BOOK)
                    .displayName(rule.title)
                    .apply {
                        lore(listOf(rule.description))
                    }
            )
        }
    }

    class Rule(
        val title: Component,
        val description: Component
    )
}