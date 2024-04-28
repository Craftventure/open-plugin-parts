package net.craftventure.database.type

import org.jooq.impl.EnumConverter


enum class ServerPublicityType {
    PUBLIC,
    DEVELOPMENT;

    companion object {
        class Converter :
            EnumConverter<String, ServerPublicityType>(String::class.java, ServerPublicityType::class.java)
    }
}
