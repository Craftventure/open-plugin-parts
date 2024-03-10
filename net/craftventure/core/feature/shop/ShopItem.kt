package net.craftventure.core.feature.shop

import net.craftventure.bukkit.ktx.extension.isDisconnected
import net.craftventure.core.feature.shop.dto.ShopItemDto
import net.craftventure.core.npc.EntityBitFlags
import net.craftventure.core.npc.EntityMetadata
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.npc.tracker.NpcEntityTracker
import net.craftventure.core.utils.OwnableItemUtils
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.itemStack
import net.craftventure.database.generated.cvdata.tables.pojos.ItemStackData
import net.craftventure.database.generated.cvdata.tables.pojos.OwnableItem
import net.craftventure.minusFlag
import net.craftventure.withFlag
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.util.BoundingBox

class ShopItem(
    val shop: ShopPresenter,
    val data: ShopItemDto
) {
    val ownableItem: OwnableItem? =
        MainRepositoryProvider.ownableItemRepository.cachedItems.firstOrNull { it.id == data.id }
    internal var item: ItemStackData? = MainRepositoryProvider.itemStackDataRepository
        .cachedItems.firstOrNull { it.id == ownableItem?.guiItemStackDataId }
    private val itemstack = ownableItem?.let { ownableItem ->
        item?.let { item ->
            OwnableItemUtils.toItem(
                ownableItem,
                item,
                null,
                false,
            )/*.apply {
                logcat { "Has item for item ${data.id}: ${this != null}/${this?.amount}" }
            }*/
        }
    }
    private val boundingBoxes = data.boundingBoxProducers.map { it.create() }
    private val boundingBoxesAtCurrentLocation
        get() = boundingBoxes.map {
            val entityLocation = entity.getLocation()
            BoundingBox(
                entityLocation.x + it.minX,
                entityLocation.y + it.minY,
                entityLocation.z + it.minZ,
                entityLocation.x + it.maxX,
                entityLocation.y + it.maxY,
                entityLocation.z + it.maxZ
            )
        }
    val entity = NpcEntity("shopItem", data.type, data.location)

    private var areaPlayers = hashSetOf<Player>()

    init {
        entity.apply {
            noGravity(true)
//            Logger.debug("Shopitem ${ownableItem!!.id} ${item?.itemStack}")
            if (entityType == EntityType.ARMOR_STAND) {
                setSlot(data.equipmentSlot, itemstack ?: item?.itemStack)
                invisible(true)
                head(getLocation().pitch, 0f, 0f)
            }
            if (entityType == EntityType.DROPPED_ITEM) {
                itemstack(itemstack ?: item?.itemStack)
                velocity(0.0, 0.0, 0.0)
            }
            data.metadata.forEach { it.applyTo(entity) }
//            if (this is Item) {
//                this.pickupDelay = Int.MAX_VALUE
//                this.setCanMobPickup(false)
//                this.setItemStack(item?.itemStack)
//            }
//
//            if (this is LivingEntity) {
//                this.equipment?.helmet = item?.itemStack
//            }
        }
    }

    fun update() {
        areaPlayers.removeAll { it.isDisconnected() }
    }

    fun remove(player: Player): Boolean {
        if (player !in areaPlayers) return false
        areaPlayers.remove(player)
        setGlowing(player, false)
        return true
    }

    private fun setGlowing(player: Player, glowing: Boolean) {
        val currentValue = entity.getMetadata(EntityMetadata.Entity.sharedFlags) ?: 0
        val newValue =
            if (glowing) currentValue.withFlag(EntityBitFlags.EntityState.GLOWING) else currentValue.minusFlag(
                EntityBitFlags.EntityState.GLOWING
            )
        entity.setPlayerSpecificMetadata(
            EntityMetadata.Entity.sharedFlags,
            newValue,
            listOf(player)
        )
    }

    fun add(player: Player): Boolean {
        if (player in areaPlayers) return false
        areaPlayers.add(player)
        setGlowing(player, true)
        return true
    }

    fun matches(player: Player): Boolean {
        val boundingBoxes = this.boundingBoxesAtCurrentLocation
        boundingBoxes.forEach { boundingBox ->
            val result = boundingBox.rayTrace(player.eyeLocation.toVector(), player.location.direction.normalize(), 5.0)
            if (result != null) return true
        }
        return false
    }

    fun spawn(tracker: NpcEntityTracker) {
        tracker.addEntity(entity)
//        NameTagManager.addNpc(entity.uuid.toString())
    }

    fun despawn(tracker: NpcEntityTracker) {
        tracker.removeEntity(entity)
//        NameTagManager.removeNpc(entity.uuid.toString())
    }
}