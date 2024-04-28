package net.craftventure.database.type

import org.jooq.impl.EnumConverter

enum class EquippedItemSlot constructor(
    val displayName: String,
    val supportedItemTypes: Array<ItemType>,
    val allowInEquipCommand: Boolean = true,
) {
    HELMET(
        displayName = "Helmet",
        supportedItemTypes = arrayOf(ItemType.HELMET)
    ),
    HAIRSTYLE(
        displayName = "Hairstyle",
        supportedItemTypes = arrayOf(ItemType.HAIRSTYLE),
        allowInEquipCommand = false,
    ),
    CHESTPLATE(
        displayName = "Chestplate",
        supportedItemTypes = arrayOf(ItemType.CHESTPLATE)
    ),
    LEGGINGS(
        displayName = "Leggings",
        supportedItemTypes = arrayOf(ItemType.LEGGINGS)
    ),
    BOOTS(
        displayName = "Boots",
        supportedItemTypes = arrayOf(ItemType.BOOTS)
    ),
    BALLOON(
        displayName = "Balloon",
        supportedItemTypes = arrayOf(ItemType.BALLOON)
    ),
    COSTUME(
        displayName = "Costume",
        supportedItemTypes = arrayOf(ItemType.COSTUME)
    ),
    HANDHELD(
        displayName = "Handheld",
        supportedItemTypes = arrayOf(ItemType.WEAPON)
    ),
    CONSUMPTION(
        displayName = "Consumption",
        supportedItemTypes = arrayOf(ItemType.FOOD, ItemType.DRINK)
    ),
    TITLE(
        displayName = "Title",
        supportedItemTypes = arrayOf(ItemType.TITLE)
    ),
    LASER_GAME_A(
        displayName = "Lasergame (A)",
        supportedItemTypes = arrayOf(ItemType.LASER_GAME_GUN, ItemType.LASER_GAME_TURRET)
    ),
    LASER_GAME_B(
        displayName = "Lasergame (B)",
        supportedItemTypes = arrayOf(ItemType.LASER_GAME_GUN, ItemType.LASER_GAME_TURRET)
    ),
    SHOULDER_PET_LEFT(
        displayName = "Shoulder Pet (left)",
        supportedItemTypes = arrayOf(ItemType.SHOULDER_PET)
    ),
    SHOULDER_PET_RIGHT(
        displayName = "Shoulder Pet (right)",
        supportedItemTypes = arrayOf(ItemType.SHOULDER_PET)
    ),
    INSTRUMENT(
        displayName = "Instrument",
        supportedItemTypes = arrayOf(ItemType.INSTRUMENT)
    );

    companion object {
        val clears = hashMapOf<EquippedItemSlot, Set<EquippedItemSlot>>().apply {
            put(HELMET, setOf(COSTUME))
            put(CHESTPLATE, setOf(COSTUME))
            put(LEGGINGS, setOf(COSTUME))
            put(BOOTS, setOf(COSTUME))
        }

        fun find(name: String) = values().let { values ->
            values.firstOrNull {
                it.name.equals(name, ignoreCase = true) ||
                        it.displayName.equals(name, ignoreCase = true)
            }
        }

        fun find(itemType: ItemType) = values()
            .filter { itemType in it.supportedItemTypes }

        class Converter : EnumConverter<String, EquippedItemSlot>(String::class.java, EquippedItemSlot::class.java)
    }
}