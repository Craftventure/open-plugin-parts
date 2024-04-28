package net.craftventure.bukkit.ktx.extension

import net.minecraft.network.syncher.SynchedEntityData

private val packAllMethod by lazy {
    SynchedEntityData::class.java.getDeclaredMethod("packAll").apply {
        this.isAccessible = true
    }
}

fun SynchedEntityData.packAllReflection(): List<SynchedEntityData.DataValue<*>> {
    return packAllMethod.invoke(this) as List<SynchedEntityData.DataValue<*>>? ?: emptyList()
}