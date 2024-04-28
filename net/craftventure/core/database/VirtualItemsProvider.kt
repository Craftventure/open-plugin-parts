package net.craftventure.core.database

import net.craftventure.bukkit.ktx.extension.isVIP
import net.craftventure.bukkit.ktx.extension.isYouTuber
import net.craftventure.core.feature.minigame.MinigameManager
import net.craftventure.core.feature.minigame.autopia.Autopia
import net.craftventure.database.generated.cvdata.tables.pojos.PlayerOwnedItem
import net.craftventure.database.repository.PlayerOwnedItemRepository
import net.craftventure.database.wrap.WrappedPlayerOwnedItem
import net.craftventure.database.wrap.WrappedPlayerOwnedItem.Companion.wrap
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.time.LocalDateTime
import java.util.*

object VirtualItemsProvider : PlayerOwnedItemRepository.VirtualItemProvider {
    override fun provideVirtualItems(who: UUID): List<WrappedPlayerOwnedItem> {
        val player = Bukkit.getPlayer(who) ?: return emptyList()
        return provideVirtualItems(player)
    }

    val hairstyleUuid = UUID.fromString("2874597d-6d73-485d-b357-5fa761ff8c7e")

    val lasergame_gun_default_uuid = UUID.fromString("7e1de631-6c99-47da-87e4-155019ea52a5")
    val lasergame_gun_shotgun_uuid = UUID.fromString("f8e98877-f085-4f9a-a0dd-6db863d06b5b")
    val lasergame_gun_sniper_uuid = UUID.fromString("6da7ffba-1cf2-49cd-8039-1d473bc08d9c")
    val lasergame_gun_bazooka_uuid = UUID.fromString("128147ab-63a9-43e9-b1d5-a0a870ff9028")
    val lasergame_turret_default_uuid = UUID.fromString("8059fead-becd-4c22-b595-8d876a7426eb")

    fun provideVirtualItems(player: Player): List<WrappedPlayerOwnedItem> {
        val items = mutableListOf<WrappedPlayerOwnedItem>()

        if (false) {
            items += PlayerOwnedItem(
                hairstyleUuid,
                player.uniqueId,
                "",
                LocalDateTime.now(),
                0,
                null,
            ).wrap()
        }

        if (player.isYouTuber()) {
            items += PlayerOwnedItem(
                UUID.randomUUID(),
                player.uniqueId,
                "kart_golfcaddy",
                LocalDateTime.now(),
                0,
                null
            ).wrap()
            items += PlayerOwnedItem(
                UUID.randomUUID(),
                player.uniqueId,
                "title_recording",
                LocalDateTime.now(),
                0,
                null
            ).wrap()
        }

        if (player.isVIP()) {
            items += PlayerOwnedItem(
                UUID.randomUUID(),
                player.uniqueId,
                "kart_default",
                LocalDateTime.now(),
                0,
                null
            ).wrap()

            if (MinigameManager.all().none { it is Autopia })
                items += PlayerOwnedItem(
                    UUID.randomUUID(),
                    player.uniqueId,
                    "kart_autopia",
                    LocalDateTime.now(),
                    0,
                    null
                ).wrap()
//            items += PlayerOwnedItem(player.uniqueId, "kart_autopia", Date(), 0)
        }

//        items += PlayerOwnedItem(
//            lasergame_gun_default_uuid,
//            player.uniqueId,
//            "lasergame_gun_default",
//            LocalDateTime.now(),
//            0
//        ).wrap()
//        items += PlayerOwnedItem(
//            lasergame_gun_shotgun_uuid,
//            player.uniqueId,
//            "lasergame_gun_shotgun",
//            LocalDateTime.now(),
//            0
//        ).wrap()
//        items += PlayerOwnedItem(
//            lasergame_gun_sniper_uuid,
//            player.uniqueId,
//            "lasergame_gun_sniper",
//            LocalDateTime.now(),
//            0
//        ).wrap()
//        items += PlayerOwnedItem(
//            lasergame_gun_bazooka_uuid,
//            player.uniqueId,
//            "lasergame_gun_bazooka",
//            LocalDateTime.now(),
//            0
//        ).wrap()
//        items += PlayerOwnedItem(
//            lasergame_turret_default_uuid,
//            player.uniqueId,
//            "lasergame_turret_default",
//            LocalDateTime.now(),
//            0
//        ).wrap()

        items.forEach { it.isVirtual = true }
        return items
    }
}