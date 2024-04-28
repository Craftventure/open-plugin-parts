package net.craftventure.database.type

import org.jooq.impl.EnumConverter


enum class RideType private constructor(val stateName: String) {
    UNKNOWN("Unknown"),
    COASTER("Roller Coaster"),
    FLATRIDE("Flatride"),
    WALK_THROUGH("Walk through"),
    DARKRIDE("Darkride"),
    WATERRIDE("Waterride"),
    INTERACTIVE_DARKRIDE("Interactive dark ride"),
    INTERACTIVE_RIDE("Interactive ride"),
    KARTING("Karting"),
    TRANSPORT("Transport"),
    SUSPENDED_FLIGHT("Suspended flight"),
    ROWING("Rowing boats");

    companion object {
        class Converter : EnumConverter<String, RideType>(String::class.java, RideType::class.java)
    }
}
