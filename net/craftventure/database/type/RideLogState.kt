package net.craftventure.database.type

import org.jooq.impl.EnumConverter


enum class RideLogState {
    COMPLETED,
    LEFT,
    LEFT_BEFORE_START,
    ENTER;

    companion object {
        class Converter : EnumConverter<String, RideLogState>(String::class.java, RideLogState::class.java)
    }
}
