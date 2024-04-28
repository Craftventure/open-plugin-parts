package net.craftventure.core.metadata

import com.comphenix.packetwrapper.WrapperPlayServerEntityMetadata
import com.comphenix.protocol.wrappers.WrappedDataValue
import kotlinx.coroutines.Job
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.bukkit.ktx.extension.isConnected
import net.craftventure.bukkit.ktx.extension.packAllReflection
import net.craftventure.bukkit.ktx.extension.sendPacket
import net.craftventure.bukkit.ktx.manager.FeatureManager
import net.craftventure.core.database.metadata.OwnableItemMetadata
import net.craftventure.core.extension.getMatrix
import net.craftventure.core.extension.getSafeEyeMatrix
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.npc.EntityMetadata
import net.craftventure.core.npc.NpcEntity
import net.craftventure.database.type.EquippedItemSlot
import net.kyori.adventure.text.Component
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import org.bukkit.entity.Player


class EquippedItemsMeta(
    val player: Player
) : TickingBaseMetadata() {
    var equippedItems: Map<EquippedItemSlot, EquipmentManager.EquippedItemData> = emptyMap()
    var appliedEquippedItems: EquipmentManager.AppliedEquippedItems? = null
        set(value) {
            field = value
            updateEffects()
        }

    private var costumeEffect: EffectData? = null
    private var helmetEffect: EffectData? = null
    private var chestplateEffect: EffectData? = null
    private var leggingsEffect: EffectData? = null
    private var bootsEffect: EffectData? = null

    private var job: Job? = null

    override fun debugComponent() =
        Component.text("job=${job != null}/${job?.isActive} equippedItems=${equippedItems.entries.joinToString { "${it.key}=${it.value.id}" }}")

    fun replaceUpdateJob(job: Job) {
        this.job?.cancel()
        this.job = job
    }

    override fun isValid(target: Any): Boolean = super.isValid(target) || player.isConnected()

    private fun updateEffects() {
        val costume = appliedEquippedItems?.costumeItem
        if (costume != null) {
            setCostumeEffect(appliedEquippedItems?.costumeItem)
        } else {
            setHelmetEffect(appliedEquippedItems?.helmetItem)
            setChestplateEffect(appliedEquippedItems?.chestplateItem)
            setLeggingsEffect(appliedEquippedItems?.leggingsItem)
            setBootsEffect(appliedEquippedItems?.bootsItem)
        }
    }

    override fun update() {
        if (player.isDead) return
        if (FeatureManager.isFeatureEnabled(FeatureManager.Feature.CLOTHING_PARTICLES)) {
            if (costumeEffect != null || helmetEffect != null || chestplateEffect != null || leggingsEffect != null || bootsEffect != null) {
                val playerMatrix = player.getMatrix()
                val headMatrix = player.getSafeEyeMatrix() ?: return

                costumeEffect?.let { applyEffects(it, playerMatrix, headMatrix, player) }
                helmetEffect?.let { applyEffects(it, playerMatrix, headMatrix, player) }
                chestplateEffect?.let { applyEffects(it, playerMatrix, headMatrix, player) }
                leggingsEffect?.let { applyEffects(it, playerMatrix, headMatrix, player) }
                bootsEffect?.let { applyEffects(it, playerMatrix, headMatrix, player) }
            }
        }
    }

    private fun applyEffects(data: EffectData, playerMatrix: Matrix4x4, headMatrix: Matrix4x4, player: Player) {
        data.effects?.let { wearMeta ->
            wearMeta.effects.forEach { it.apply(player, playerMatrix, headMatrix, data.equippedItemData, this) }
        }
    }

    private fun clearEffects() {
        costumeEffect = null
        helmetEffect = null
        chestplateEffect = null
        leggingsEffect = null
        bootsEffect = null
    }

    private fun setCostumeEffect(effect: EquipmentManager.EquippedItemData?) {
        costumeEffect = effect?.let(::EffectData)
        helmetEffect = null
        chestplateEffect = null
        leggingsEffect = null
        bootsEffect = null
    }

    private fun setHelmetEffect(effect: EquipmentManager.EquippedItemData?) {
        helmetEffect = effect?.let(::EffectData)
        costumeEffect = null
    }

    private fun setChestplateEffect(effect: EquipmentManager.EquippedItemData?) {
        chestplateEffect = effect?.let(::EffectData)
        costumeEffect = null
    }

    private fun setLeggingsEffect(effect: EquipmentManager.EquippedItemData?) {
        leggingsEffect = effect?.let(::EffectData)
        costumeEffect = null
    }

    private fun setBootsEffect(effect: EquipmentManager.EquippedItemData?) {
        bootsEffect = effect?.let(::EffectData)
        costumeEffect = null
    }

//    private fun getEffect(name: String?): PlayerEffect? {
//        return when (name) {
//            "costume_5yearcv" -> Costume5YearCvEffect(name)
//            "magicarp" -> MagicarpEffect(name)
//            "hat_addict_5" -> HatAddict5Effect(name)
//            "well_wisher" -> WellWisherEffect(name)
//            else -> null
//        }
//    }

    fun applySpawnPacketsTo(vararg other: Player) {
        val entityData = NpcEntity.generateSynchedEntityData(player.location)
//        entityData.setMetadata()
        val packet = ClientboundSetEntityDataPacket(player.entityId, entityData.packAllReflection())
        other.forEach { it.sendPacket(packet) }

        val wrapperPlayServerEntityMetadata = WrapperPlayServerEntityMetadata()
        wrapperPlayServerEntityMetadata.entityID = player.entityId
        wrapperPlayServerEntityMetadata.metadata = listOf(
            WrappedDataValue(
                EntityMetadata.Player.shoulderLeft.absoluteIndex!!,
                EntityMetadata.Player.shoulderLeft.wrappedSerializer,
                appliedEquippedItems?.shoulderPetLeft?.ownableItemMeta?.parsedCompoundTag ?: CompoundTag()
            ),
            WrappedDataValue(
                EntityMetadata.Player.shoulderRight.absoluteIndex!!,
                EntityMetadata.Player.shoulderRight.wrappedSerializer,
                appliedEquippedItems?.shoulderPetRight?.ownableItemMeta?.parsedCompoundTag ?: CompoundTag()
            )
        )
        other.forEach { wrapperPlayServerEntityMetadata.sendPacket(it) }
    }

    private data class EffectData(
        val equippedItemData: EquipmentManager.EquippedItemData,
        val effects: OwnableItemMetadata.ItemWearMeta? = equippedItemData.ownableItemMeta?.itemWear,
    )

    companion object {
        fun getOrCreate(player: Player) = player.getOrCreateMetadata { EquippedItemsMeta(player) }
        fun get(player: Player) = player.getMetadata<EquippedItemsMeta>()

        fun Player.equippedItemsMeta() = get(this)
        fun Player.requireEquippedItemsMeta() = getOrCreate(this)
    }
}
