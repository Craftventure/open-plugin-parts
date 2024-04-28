package net.craftventure.database.type

import org.jooq.impl.EnumConverter


enum class BalloonParticleType {
    DEFAULT,
    HALLOWEEN_2016,
    SINGLE_TOP_MID_FLAME;

    companion object {
        class Converter :
            EnumConverter<String, BalloonParticleType>(String::class.java, BalloonParticleType::class.java)
    }
}
