package net.craftventure.database.bukkit.extensions

import net.craftventure.bukkit.ktx.extension.*
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.displayNameBuilder
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.displayNameWithBuilder
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.ktx.util.BookUtils
import net.craftventure.database.generated.cvdata.tables.pojos.Mail
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta

fun Mail.validate() = id != null && receiverUuid != null && (title != null || message != null)

val Mail.item: ItemStack
    get() {
        return ItemStack(if (read!!) Material.BOOK else Material.WRITABLE_BOOK).apply {
            displayName(Component.text((if (!read!!) "Unread: " else "") + title, CVTextColor.serverNoticeAccent))
            lore(
                listOfNotNull(
                    Component.text("By", CVTextColor.subtle),
                    Component.text(
                        from ?: senderUuid?.toName() ?: "?",
                        CVTextColor.MENU_DEFAULT_LORE_ACCENT
                    ),
                    Component.empty(),
                    Component.text("Sent on", CVTextColor.subtle),
                    Component.text(
                        sendDate.toString(),
                        CVTextColor.MENU_DEFAULT_LORE_ACCENT
                    ),
                    if (expires != null)
                        Component.newline() + Component.newline() + Component.text("Autodeletes on", CVTextColor.subtle)
                            .append(Component.text(expires.toString(), CVTextColor.MENU_DEFAULT_LORE_ACCENT))
                    else
                        null
                )
            )

            if (!read!!) {
                addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1)
            }
            hideEnchants()
            hideAttributes()
            hideDestroys()
            hidePlacedOn()
            hidePotionEffects()
            hideUnbreakable()
        }
    }

val Mail.book: ItemStack
    get() {
        val writtenBook = ItemStack(Material.WRITTEN_BOOK)
        writtenBook.displayNameWithBuilder {
            text("Book")
            subtle(" (right click)")
        }

        val bookMeta = writtenBook.itemMeta as BookMeta
        bookMeta.title(Component.text("A nice book"))
        bookMeta.generation = BookMeta.Generation.ORIGINAL
        bookMeta.author(Component.text("Craftventure", CVTextColor.serverNotice))

        val messageLines = message
            ?.replace("\r\n", "\n")
            ?.replace("\r", "\n")
            ?.split(" ") ?: listOf("This mail has no message")
//        Logger.debug("Lines ${messageLines.joinToString(", ")}")

        val pageLines = mutableListOf<Component>()
        var currentLine = ""
        val spaceWidth = BookUtils.textWidth(" ")
        for (baseLine in messageLines) {
            val subLines = baseLine.split("\n")
//            Logger.debug("Line ${baseLine.replace("\n", "\\n")}")
            for ((index, line) in subLines.withIndex()) {
//                Logger.debug("  - Word $index $line")
                if (BookUtils.textWidth(currentLine) + BookUtils.textWidth(line) + spaceWidth /* SPACE */ >= BookUtils.PAGE_WIDTH) {
//                    Logger.debug("    - New page line [$currentLine]")
                    pageLines.add(
                        PlainTextComponentSerializer.plainText().deserialize("$currentLine\n")
                            .color(NamedTextColor.BLACK)
                    )
                    currentLine = line
                } else {
                    if (currentLine.isNotEmpty())
                        currentLine += " "
                    currentLine += line

                    if (index < subLines.size - 1) {
//                        Logger.debug("    - Start at new line [$currentLine]")
                        pageLines.add(
                            PlainTextComponentSerializer.plainText().deserialize("$currentLine\n")
                                .color(NamedTextColor.BLACK)
                        )
                        currentLine = ""
                    }
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            pageLines.add(
                PlainTextComponentSerializer.plainText().deserialize("$currentLine\n").color(NamedTextColor.BLACK)
            )
        }
        val pages = pageLines
            .chunked(BookUtils.PAGE_LINES) { it.toTypedArray() }
            .map { components ->
                var result = Component.empty()
                components.forEach { component ->
                    result = result.append(component)
                }
                result
            }
            .toMutableList()
//        Logger.debug(
//            "Pages ${
//                pages.joinToString(", ", prefix = "{", postfix = "}\n") { lines ->
//                    lines.joinToString(", ", prefix = "[", postfix = "]") { ChatColor.stripColor(it.toLegacyText()) }
//                }
//            }"
//        )

        var optionsBuilder = Component.text("Mail options", NamedTextColor.BLACK)

        if (deletable!!) {
            optionsBuilder = optionsBuilder.append(
                Component.newline() + Component.newline() + Component.text(
                    "> Delete this mail",
                    NamedTextColor.DARK_RED
                )
                    .hoverEvent(
                        Component.text("Click to delete the selected mail permanently", CVTextColor.CHAT_HOVER)
                            .asHoverEvent()
                    )
                    .clickEvent(ClickEvent.runCommand("/mail delete $id"))
            )
        } else if (!deletable!! && deletableAfter != null) {
            optionsBuilder = optionsBuilder.append(
                Component.newline() + Component.newline() + Component.text(
                    "This mail can be deleted after $deletableAfter",
                    NamedTextColor.DARK_RED
                )
            )
        } else {
            optionsBuilder = optionsBuilder.append(
                Component.newline() + Component.newline() + Component.text(
                    "This mail can't be deleted",
                    NamedTextColor.DARK_RED
                )
            )
        }
        optionsBuilder = optionsBuilder.append(
            Component.newline() + Component.newline() + Component.text("> Back to inbox", NamedTextColor.BLACK)
                .hoverEvent(
                    Component.text("Click to go back to your inbox", CVTextColor.CHAT_HOVER)
                        .asHoverEvent()
                )
                .clickEvent(ClickEvent.runCommand("/mails"))
        )

        pages.add(optionsBuilder)

        pages.forEach {
            logcat { "Page ${it.content()}" }
        }

        bookMeta.addPages(*pages.toTypedArray())

        writtenBook.itemMeta = bookMeta

        return writtenBook
    }