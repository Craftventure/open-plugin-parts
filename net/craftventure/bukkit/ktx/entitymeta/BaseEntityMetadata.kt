package net.craftventure.bukkit.ktx.entitymeta

import org.bukkit.entity.Entity

abstract class BaseEntityMetadata : BaseMetadata() {
    override fun isValid(target: Any): Boolean = if (target is Entity) target.isValid else true
}