package net.craftventure.bukkit.ktx.extension

import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.SynchedEntityData



//fun <T> SynchedEntityData.setMetadata(field: EntityMetadata.Interactor<T>, value: T) {
//    setMetadata(field.accessor, value, field.defaultValue)
//}

fun <T> SynchedEntityData.setMetadata(field: EntityDataAccessor<T>, value: T, defaultValue: T) {
    try {
        set(field, value)
    } catch (e: Exception) {
        define(field, defaultValue)
        set(field, value)
    }
}