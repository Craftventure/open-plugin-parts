package net.craftventure.core.feature.balloon.extensions

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.util.EntityConstants
import net.craftventure.core.database.ItemStackLoader
import net.craftventure.core.feature.balloon.holders.BalloonHolder
import net.craftventure.core.feature.balloon.types.ExtensibleBalloon
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.npc.json.EntityInteractorJson
import net.craftventure.core.npc.tracker.NpcEntityTracker
import net.craftventure.core.utils.ItemStackUtils
import net.craftventure.temporary.getOwnableItemMetadata
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.util.Vector

class EntityExtension(private val data: Json) : ExtensibleBalloon.Extension() {
    private var entity: NpcEntity? = null

    private fun requireEntity(balloon: ExtensibleBalloon): NpcEntity {
        if (entity != null) return entity!!
        val entity = NpcEntity(location = balloon.balloonLocation!!, entityType = data.entityType)
        this.entity = entity
        entity.noGravity(true)
        entity.invisible(data.invisible)
        data.metadata.forEach { it.applyTo(entity) }
        data.helmet?.let {
            if (it == "self") {
                val item = ItemStackUtils.fromString(balloon.id) ?: return@let
                balloon.ownableItem?.getOwnableItemMetadata()?.let {
                    ItemStackLoader.apply(item, it)
                }
                entity.helmet(item)
                return@let
            }
            val item = ItemStackUtils.fromString(it) ?: return@let
            balloon.ownableItem?.getOwnableItemMetadata()?.let {
                ItemStackLoader.apply(item, it)
            }
            entity.helmet(item)
        }
        return entity
    }

    private fun getLocation(balloon: ExtensibleBalloon): Location =
        balloon.balloonLocation!!.clone()
            .add(data.offset)
            .add(
                0.0,
                if (data.compensateForArmorstand && data.entityType == EntityType.ARMOR_STAND) -EntityConstants.ArmorStandHeadOffset else 0.0,
                0.0
            )

    override fun spawn(balloon: ExtensibleBalloon, balloonHolder: BalloonHolder, tracker: NpcEntityTracker) {
        val entity = requireEntity(balloon)
        tracker.addEntity(entity)
    }

    override fun update(balloon: ExtensibleBalloon) {
        super.update(balloon)

        val entity = requireEntity(balloon)
        val location = getLocation(balloon)
        entity.move(location)
    }

    override fun despawn(balloon: ExtensibleBalloon, withEffects: Boolean, tracker: NpcEntityTracker) {
        if (entity != null) {
            tracker.removeEntity(entity!!)
            entity = null
        }
    }

    @JsonClass(generateAdapter = true)
    class Json(
        val entityType: EntityType = EntityType.ARMOR_STAND,
        val compensateForArmorstand: Boolean = true,
        val metadata: List<EntityInteractorJson<Any>> = emptyList(),
        val offset: Vector = Vector(),
        val helmet: String? = null,
        val invisible: Boolean = true,
    ) : ExtensibleBalloon.Extension.Json() {
        override fun toExtension(): ExtensibleBalloon.Extension = EntityExtension(this)

        companion object {
            const val type = "entity"
        }
    }
}