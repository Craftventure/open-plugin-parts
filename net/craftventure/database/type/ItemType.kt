package net.craftventure.database.type

import org.jooq.impl.EnumConverter


enum class ItemType constructor(
    val displayName: String,
    val displayNamePlural: String,
    val description: String,
    val equippable: Boolean = false,
    val consumeViaMenu: Boolean = false
) {
    HELMET(
        displayName = "Helmet",
        displayNamePlural = "Helmets/Hats",
        description = "Hats, can be worn",
        equippable = true
    ),
    CHESTPLATE(
        displayName = "Chestplate",
        displayNamePlural = "Chestplates",
        description = "Chestplate, can be worn", equippable = true
    ),
    LEGGINGS(
        displayName = "Leggings",
        displayNamePlural = "Leggings",
        description = "Leggings, can be worn",
        equippable = true
    ),
    BOOTS(
        displayName = "Boots",
        displayNamePlural = "Boots",
        description = "Boots, can be worn",
        equippable = true
    ),
    BALLOON(
        displayName = "Balloon",
        displayNamePlural = "Balloons",
        description = "Balloon, can be held",
        equippable = true
    ),
    COSTUME(
        displayName = "Costume",
        displayNamePlural = "Costumes",
        description = "Costume, can be worn. Will override all other clothing",
        equippable = true
    ),
    WEAPON(
        displayName = "Handheld",
        displayNamePlural = "Handhelds",
        description = "Handhelds, can be held in your hand",
        equippable = true
    ),
    FOOD(
        displayName = "Food",
        displayNamePlural = "Foods",
        description = "Foods, can be eaten",
        equippable = true
    ),
    DRINK(
        displayName = "Drink",
        displayNamePlural = "Drinks",
        description = "Drinks, can be drunk",
        equippable = true
    ),
    TITLE(
        displayName = "Title",
        displayNamePlural = "Titles",
        description = "Title, can be worn above your head",
        equippable = true
    ),
    COIN_BOOSTER(
        displayName = "Coin Booster",
        displayNamePlural = "Coin Boosters",
        description = "Coin Booster, can be enabled to temporarily increase your VentureCoin rate",
        consumeViaMenu = true
    ),
    SERVER_COIN_BOOSTER(
        displayName = "Server Coin Booster",
        displayNamePlural = "Server Coin Boosters",
        description = "Server Coin Booster, can be enabled to temporarily increase the VentureCoin rate for everyone on the server",
        consumeViaMenu = true
    ),
    KART(
        displayName = "Kart",
        displayNamePlural = "Karts",
        description = "Kart, can be used to drive around the park"
    ),
    KART_ENGINE(
        displayName = "Kart Engine",
        displayNamePlural = "Karts Engines",
        description = "Kart part, can be used to drive around the park"
    ),
    KART_STEER(
        displayName = "Kart Steer",
        displayNamePlural = "Karts Steers",
        description = "Kart part, can be used to drive around the park"
    ),
    KART_BRAKES(
        displayName = "Kart Brake",
        displayNamePlural = "Karts Brakes",
        description = "Kart part, can be used to drive around the park"
    ),
    KART_TIRES(
        displayName = "Kart Tire",
        displayNamePlural = "Karts Tires",
        description = "Kart part, can be used to drive around the park"
    ),
    KART_VISUAL(
        displayName = "Kart Visual",
        displayNamePlural = "Karts Visuals",
        description = "Kart part, can be used to drive around the park"
    ),
    LASER_GAME_GUN(
        displayName = "Gun",
        displayNamePlural = "Guns",
        description = "Guns that can be equipped for the LaserGame",
        equippable = true
    ),
    LASER_GAME_TURRET(
        displayName = "Turret",
        displayNamePlural = "Turrets",
        description = "Turrets that can be equipped for the LaserGame",
        equippable = true
    ),
    INSTRUMENT(
        displayName = "Instrument",
        displayNamePlural = "Instruments",
        description = "Instruments that can be equipped and played",
        equippable = true
    ),
    SHOULDER_PET(
        displayName = "Shoulder Pet",
        displayNamePlural = "Shoulder Pets",
        description = "Pets that can be placed on your shoulder",
        equippable = true,
    ),
    BACK_ITEM(
        displayName = "Back Item",
        displayNamePlural = "Back Items",
        description = "Items that can be put on your back",
        equippable = false,
    ),
    HAIRSTYLE(
        displayName = "Hairstyle",
        displayNamePlural = "Hairstyles",
        description = "Items that function as a hairstyle",
        equippable = false,
    ),
    MUSIC_SHEET(
        displayName = "Music Sheet",
        displayNamePlural = "Music Sheets",
        description = "Items that provide sheets for instruments",
        equippable = false,
    );

    fun nameForAmount(amount: Int) = when (amount) {
        1 -> displayName
        else -> displayNamePlural
    }

    companion object {
        class Converter : EnumConverter<String, ItemType>(String::class.java, ItemType::class.java)
    }
}
