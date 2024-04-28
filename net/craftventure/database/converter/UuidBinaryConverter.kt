package net.craftventure.database.converter

import net.craftventure.core.ktx.extension.toBinary
import net.craftventure.core.ktx.extension.toUuid
import org.jooq.Converter
import java.util.*

class UuidBinaryConverter : Converter<ByteArray, UUID> {
    override fun from(databaseObject: ByteArray?): UUID? {
        if (databaseObject == null) return null
        require(databaseObject.size == 16)
        return databaseObject.toUuid()
    }

    override fun to(userObject: UUID?): ByteArray? = userObject?.toBinary()

    override fun fromType(): Class<ByteArray> = ByteArray::class.java
    override fun toType(): Class<UUID> = UUID::class.java
}