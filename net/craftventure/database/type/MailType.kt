package net.craftventure.database.type

import org.jooq.impl.EnumConverter

enum class MailType {
    PLAYER_SENT,
    AUTO,
    ANNOUNCEMENT,
    BOUGHT_PACKAGE;

    companion object {
        class Converter : EnumConverter<String, MailType>(String::class.java, MailType::class.java)
    }
}