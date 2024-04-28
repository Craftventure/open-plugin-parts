package net.craftventure.core.npc.actor.action

import com.google.gson.annotations.Expose
import com.squareup.moshi.JsonClass
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.utils.ItemStackUtils
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemStack

@JsonClass(generateAdapter = true)
class ActionEquipment(
    @field:Expose val slot: Int,
    @field:Expose val itemData: String?
) : ActorAction() {
    @Transient
    private var cachedItem: ItemStack? = null
        get() {
            if (field == null && itemData != null) field = ItemStackUtils.fromString(itemData)
            return field
        }

    override fun executeAction(npcEntity: NpcEntity?) { //        Logger.info("Parsing " + itemData);
        val itemStack = cachedItem
        //        if (itemStack != null)
        set(itemStack, npcEntity)
    }

    private operator fun set(itemStack: ItemStack?, npcEntity: NpcEntity?) {
        if (npcEntity?.entityType == EntityType.DROPPED_ITEM) {
            npcEntity.itemstack(itemStack)
        }
        when (slot) {
            SlotType.HELMET -> npcEntity!!.helmet(itemStack)
            SlotType.MAIN_HAND -> npcEntity!!.held(itemStack)
            SlotType.OFF_HAND -> npcEntity!!.heldOffHand(itemStack)
            SlotType.BOOTS -> npcEntity!!.boots(itemStack)
            SlotType.LEGS -> npcEntity!!.leggings(itemStack)
            SlotType.CHEST -> npcEntity!!.chestplate(itemStack)
        }
    }

    override val actionTypeId: Int
        get() = Type.EQUIPMENT

    interface SlotType {
        companion object {
            const val MAIN_HAND = 1
            const val OFF_HAND = 2
            const val BOOTS = 3
            const val LEGS = 4
            const val CHEST = 5
            const val HELMET = 6
        }
    }

}