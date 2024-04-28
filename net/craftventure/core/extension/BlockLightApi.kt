package net.craftventure.core.extension

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.Waterlogged
import org.bukkit.block.data.type.Light

fun Block.setLightLevel(level: Int) {
    val data = blockData
    if (data is Light && data.level == level) return
    val isWaterlogged = this is Waterlogged && this.isWaterlogged
    this.type = Material.LIGHT

    val newData = blockData
//    Logger.debug("Setting light $level for ${self is Light} with current=${self.lightLevel} ${self.javaClass.name}")
    if (newData is Waterlogged) {
        newData.isWaterlogged = isWaterlogged
    }
    if (newData is Light) {
        newData.level = level
    }
    blockData = newData
}

fun Block.resetLight() {
    if (this.type != Material.LIGHT) return

    if (this is Waterlogged && this.isWaterlogged) {
        this.type = Material.WATER
    } else {
        this.type = Material.AIR
    }
}