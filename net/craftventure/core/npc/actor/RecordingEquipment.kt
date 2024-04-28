package net.craftventure.core.npc.actor

import com.google.gson.annotations.Expose
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.extension.takeIfNotAir
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.npc.actor.action.ActionEquipment
import net.craftventure.core.utils.ItemStackUtils
import net.craftventure.extension.encodeAsBase64ToString
import org.bukkit.inventory.EntityEquipment
import org.bukkit.inventory.ItemStack

@JsonClass(generateAdapter = true)
class RecordingEquipment(
    @Json(name = "item")
    @field:Expose var item: String? = null,
    @Json(name = "main_hand_item")
    @field:Expose var mainHandItem: String? = null,
    @Json(name = "off_hand_item")
    @field:Expose var offHandItem: String? = null,
    @Json(name = "helmet_item")
    @field:Expose var helmetItem: String? = null,
    @Json(name = "chest_item")
    @field:Expose var chestItem: String? = null,
    @Json(name = "legs_item")
    @field:Expose var legsItem: String? = null,
    @Json(name = "boots_item")
    @field:Expose var bootsItem: String? = null
) {
    constructor(equipment: EntityEquipment) : this(
        equipment.itemInMainHand.takeIfNotAir()?.serializeAsBytes()?.encodeAsBase64ToString(),
        equipment.itemInOffHand.takeIfNotAir()?.serializeAsBytes()?.encodeAsBase64ToString(),
        equipment.helmet?.takeIfNotAir()?.serializeAsBytes()?.encodeAsBase64ToString(),
        equipment.chestplate?.takeIfNotAir()?.serializeAsBytes()?.encodeAsBase64ToString(),
        equipment.leggings?.takeIfNotAir()?.serializeAsBytes()?.encodeAsBase64ToString(),
        equipment.boots?.takeIfNotAir()?.serializeAsBytes()?.encodeAsBase64ToString(),
    )

    fun updateItems() {
        mainHandItem =
            ItemStackUtils.fromString(mainHandItem)?.takeIfNotAir()?.serializeAsBytes()?.encodeAsBase64ToString()
        offHandItem =
            ItemStackUtils.fromString(offHandItem)?.takeIfNotAir()?.serializeAsBytes()?.encodeAsBase64ToString()
        helmetItem =
            ItemStackUtils.fromString(helmetItem)?.takeIfNotAir()?.serializeAsBytes()?.encodeAsBase64ToString()
        chestItem =
            ItemStackUtils.fromString(chestItem)?.takeIfNotAir()?.serializeAsBytes()?.encodeAsBase64ToString()
        legsItem =
            ItemStackUtils.fromString(legsItem)?.takeIfNotAir()?.serializeAsBytes()?.encodeAsBase64ToString()
        bootsItem =
            ItemStackUtils.fromString(bootsItem)?.takeIfNotAir()?.serializeAsBytes()?.encodeAsBase64ToString()
    }

    fun equip(npcEntity: NpcEntity) {
        setItem(ActionEquipment.SlotType.MAIN_HAND, mainHandItem, npcEntity)
        setItem(ActionEquipment.SlotType.OFF_HAND, offHandItem, npcEntity)
        setItem(ActionEquipment.SlotType.HELMET, helmetItem, npcEntity)
        setItem(ActionEquipment.SlotType.CHEST, chestItem, npcEntity)
        setItem(ActionEquipment.SlotType.LEGS, legsItem, npcEntity)
        setItem(ActionEquipment.SlotType.BOOTS, bootsItem, npcEntity)
        npcEntity.itemstack(ItemStackUtils.fromString(item))
    }

    private fun setItem(slot: Int, item: String?, npcEntity: NpcEntity) {
        val itemStack = CommandMigrate.migrateDurabilityToCustomModelData(ItemStackUtils.fromString(item))
        itemStack?.let { set(slot, it, npcEntity) }
    }

    private operator fun set(slot: Int, itemStack: ItemStack, npcEntity: NpcEntity) {
        val fixedItemStack = CommandMigrate.migrateDurabilityToCustomModelData(itemStack)
        if (slot == ActionEquipment.SlotType.HELMET)
            npcEntity.helmet(fixedItemStack)
        else if (slot == ActionEquipment.SlotType.MAIN_HAND)
            npcEntity.held(fixedItemStack)
        else if (slot == ActionEquipment.SlotType.OFF_HAND)
            npcEntity.heldOffHand(fixedItemStack)
        else if (slot == ActionEquipment.SlotType.BOOTS)
            npcEntity.boots(fixedItemStack)
        else if (slot == ActionEquipment.SlotType.LEGS)
            npcEntity.leggings(fixedItemStack)
        else if (slot == ActionEquipment.SlotType.CHEST)
            npcEntity.chestplate(fixedItemStack)
    }
}