package net.craftventure.database.type

import org.jooq.impl.EnumConverter


enum class ServerTypeType {
    CRAFTVENTURE,
    LIMBO,
    OTHER;

    companion object {
        class Converter :
            EnumConverter<String, ServerTypeType>(String::class.java, ServerTypeType::class.java)
    }
}
