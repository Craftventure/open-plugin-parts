package net.craftventure.database.type

import org.jooq.impl.EnumConverter


enum class AchievementType {
    ROAMING,
    ROAMING_DISPLAY_IF_UNLOCKED,
    SECRET,
    SECRET_ROOM,
    RIDE;

    companion object {
        class Converter : EnumConverter<String, AchievementType>(String::class.java, AchievementType::class.java)
    }
}
