package net.craftventure.bukkit.ktx.extension

import net.craftventure.bukkit.ktx.util.EntityTable
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity


val EntityType.nmsTypeId: Int?
    get() = EntityTable.getTypeId(this)

val EntityType.isLivingEntity: Boolean
    get() = LivingEntity::class.java.isAssignableFrom(this.entityClass)

fun Entity.ejectAndRemove() {
    eject()
    remove()
}

fun <T : Entity> T.takeIfValid() = takeIf { it.isValid }