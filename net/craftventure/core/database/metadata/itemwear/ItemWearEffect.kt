package net.craftventure.core.database.metadata.itemwear

import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.metadata.EquippedItemsMeta
import org.bukkit.GameMode
import org.bukkit.entity.Player

sealed class ItemWearEffect {
    fun apply(
        player: Player,
        playerMatrix: Matrix4x4,
        headMatrix: Matrix4x4,
        data: EquipmentManager.EquippedItemData,
        meta: EquippedItemsMeta
    ) {
        if (shouldRun(player, playerMatrix, headMatrix, data, meta)) {
            applyActual(player, playerMatrix, headMatrix, data, meta)
        }
    }

    protected abstract fun applyActual(
        player: Player,
        playerMatrix: Matrix4x4,
        headMatrix: Matrix4x4,
        data: EquipmentManager.EquippedItemData,
        meta: EquippedItemsMeta
    )

    open fun shouldRun(
        player: Player,
        playerMatrix: Matrix4x4,
        headMatrix: Matrix4x4,
        data: EquipmentManager.EquippedItemData,
        meta: EquippedItemsMeta
    ): Boolean {
        return player.gameMode != GameMode.SPECTATOR
    }
}