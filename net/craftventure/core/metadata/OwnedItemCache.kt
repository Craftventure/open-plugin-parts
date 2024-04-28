package net.craftventure.core.metadata

import net.craftventure.annotationkit.GenerateService
import net.craftventure.bukkit.ktx.entitymeta.BasePlayerMetadata
import net.craftventure.bukkit.ktx.entitymeta.PlayerMetaFactory
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.core.async.executeAsync
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.tables.pojos.PlayerOwnedItem
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

class OwnedItemCache(
    val player: Player
) : BasePlayerMetadata(player) {
    var ownedItems = listOf<PlayerOwnedItem>()
        private set
    var ownedItemIds = setOf<String>()
        private set

    init {
        update()
    }

    override fun debugComponent() =
        Component.text("ownedItemIds=${ownedItemIds.joinToString()} ownedItems=${ownedItems.joinToString { it.id.toString() }}")

    fun update(onFinish: (() -> Unit)? = null) {
        executeAsync {
            ownedItems = MainRepositoryProvider.playerOwnedItemRepository.get(player.uniqueId).toList().apply {
                ownedItemIds = this.map { it.ownedItemId!! }.toSet()
            }
            onFinish?.invoke()
//            Logger.debug("Items for ${player.name} are: ${ownedItems.joinToString(", ")}")
        }
    }

    @GenerateService
    class Generator : PlayerMetaFactory() {
        override fun create(player: Player) = player.getOrCreateMetadata { OwnedItemCache(player) }
    }
}