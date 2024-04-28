package net.craftventure.database.type

import org.jooq.impl.EnumConverter


enum class OperatorKind {
    NONE,
    ANYONE,
    VIP,
    CREW,
    OWNER;

    companion object {
        class Converter : EnumConverter<String, OperatorKind>(String::class.java, OperatorKind::class.java)
    }
}
