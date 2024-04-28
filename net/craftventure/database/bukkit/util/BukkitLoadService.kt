package net.craftventure.database.bukkit.util

import net.craftventure.annotationkit.GenerateService
import net.craftventure.chat.bungee.util.rebuildCvMiniMessage
import net.craftventure.core.ktx.service.LoadService
import net.craftventure.database.type.BankAccountType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

@GenerateService
class BukkitLoadService : LoadService {
    override fun init() {
        rebuildCvMiniMessage {
            BankAccountType.values().forEach { bankAccountType ->
                resolver(
                    TagResolver.resolver(
                        "bank_account_type_${bankAccountType.internalName}",
                        Tag.inserting(Component.text(bankAccountType.emoji).color(NamedTextColor.WHITE))
                    )
                )
            }
        }
    }
}