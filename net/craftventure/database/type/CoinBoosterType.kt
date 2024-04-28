package net.craftventure.database.type

import org.jooq.impl.EnumConverter


enum class CoinBoosterType(val prefix: String) {
    ADD("+"),
    MULTIPLY("x");

    companion object {
        class Converter : EnumConverter<String, CoinBoosterType>(String::class.java, CoinBoosterType::class.java)
    }
}
