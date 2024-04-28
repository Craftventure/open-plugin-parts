package net.craftventure.database.type

import org.jooq.impl.EnumConverter

enum class MinigameScoreType {
    TOTAL,
    ROUND,
    KILLS,
    DEATHS;

    companion object {
        class Converter : EnumConverter<String, MinigameScoreType>(String::class.java, MinigameScoreType::class.java)
    }
}