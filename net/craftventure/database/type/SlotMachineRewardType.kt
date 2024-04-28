package net.craftventure.database.type

import org.jooq.impl.EnumConverter


enum class SlotMachineRewardType(val description: String) {
    X0_5("0.5x"),
    X1("1x"),
    X2("2x"),
    X3("3x"),
    X4("4x"),
    X5("5x"),
    X6("6x"),
    X8("8x"),
    X10("10x"),
    X15("15x"),
    X20("20x"),
    X25("25x"),
    X50("50x");

    companion object {
        class Converter :
            EnumConverter<String, SlotMachineRewardType>(String::class.java, SlotMachineRewardType::class.java)
    }
}
