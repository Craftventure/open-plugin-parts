package net.craftventure.database.type

import org.jooq.impl.EnumConverter


enum class TransactionType {
    SHOP,
    ACHIEVEMENT,
    CASINO_WIN,
    CASINO_SPEND,
    COMMAND,
    ACTIVE_ONLINE_REWARD,
    SCAVENGER_PACKET,
    DAILY_WINTER_CHEST,
    DAILY_SUMMER_CHEST,
    MINIGAME,
    EVENT,
    ENTITY_KILL,
    WISHING_WELL,
    WVW_REWARD,
    SHOOTER_RIDE_REWARD,
    BARBER;

    companion object {
        class Converter : EnumConverter<String, TransactionType>(String::class.java, TransactionType::class.java)
    }
}
