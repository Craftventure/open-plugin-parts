package net.craftventure.core.manager

import net.craftventure.bukkit.ktx.util.SoundUtils
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.async.executeAnyAsync
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeMain
import net.craftventure.core.inventory.impl.MailsMenu
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.book
import net.craftventure.database.bukkit.extensions.validate
import net.craftventure.database.generated.cvdata.tables.pojos.Mail
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import java.util.*

object MailManager {
    fun cleanup() {
        executeAsync {
            val mailCheck = MainRepositoryProvider.mailRepository.cleanup()
//            if (mailCheck != 0)
//                info("Expired $mailCheck mails", logToCrew = false)
        }
    }

    fun send(mail: Mail) = executeAsync { sendSync(mail) }

    fun sendSync(mail: Mail): Boolean {
        if (!mail.validate())
            return false

        val database = MainRepositoryProvider.mailRepository
        val created = database.create(mail)
        if (created) {
            mail.receiverUuid?.let { receiverUuid ->
                Bukkit.getPlayer(receiverUuid)?.let { player ->
                    player.playSound(player.location, SoundUtils.MAIL_RECEIVED, SoundCategory.AMBIENT, 1f, 1f)
                    player.sendMessage(
                        Component
                            .text("New mail received titled ", NamedTextColor.GREEN)
                            .append(Component.text("'${mail.title}'", NamedTextColor.DARK_GREEN))
                            .append(Component.text(". Click here to open it"))
                            .hoverEvent(Component.text("Click to read", CVTextColor.CHAT_HOVER).asHoverEvent())
                            .clickEvent(ClickEvent.runCommand("/mail read ${mail.id}"))
                    )
                }
            }
            return true
        }
        return false
    }

    fun notifyUnreads(uuid: UUID) {
        Bukkit.getPlayer(uuid)?.let { player ->
            executeAsync {
                val database = MainRepositoryProvider.mailRepository
                val mailsCount = database.getUnreadMailsCountFor(uuid)
                if (mailsCount > 0)
                    player.sendMessage(
                        Component
                            .text(
                                "You have $mailsCount unread mails, click here to view them or use /mails",
                                NamedTextColor.GREEN
                            )
                            .clickEvent(ClickEvent.runCommand("/mails"))
                    )
            }
        }
    }

    fun markAsReadSync(mailId: UUID): Boolean {
        return MainRepositoryProvider.mailRepository.markAsRead(mailId) == 1
    }

    fun markAsRead(mailId: UUID) {
        executeAsync {
            markAsReadSync(mailId)
        }
    }

    fun open(player: Player, mailId: UUID) {
        executeAsync {
            val mail = MainRepositoryProvider.mailRepository.find(mailId)
            if (mail != null)
                open(player, mail)
            else
                player.sendMessage(CVTextColor.serverNotice + "That mail doesn't seem to exist!")
        }
    }

    fun open(player: Player, mail: Mail) {
        executeMain {
            player.openBook(mail.book)
        }

        if (!mail.read!!)
            executeAnyAsync {
                markAsReadSync(mail.id!!)
            }
    }

    fun delete(player: Player, mailId: UUID) {
        executeAsync {
            val mail = MainRepositoryProvider.mailRepository.find(mailId)
            if (mail?.deletable == false) {
                return@executeAsync
            }
            val deleted = MainRepositoryProvider.mailRepository.deleteMail(player.uniqueId, mailId)
            if (deleted > 0) {
                player.sendMessage(CVTextColor.serverNotice + "The selected mail was deleted")
            } else {
                player.sendMessage(CVTextColor.serverError + "The selected mail was not deleted!")
            }
        }
    }

    fun openMenu(player: Player) {
        MailsMenu(player).openAsMenu(player)
    }
}