package net.craftventure.bukkit.ktx.extension

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.block.data.Openable
import org.bukkit.block.data.type.Piston
import org.bukkit.block.data.type.Switch

fun Block.isOpenable() = blockData is Openable

fun Block.open(open: Boolean) {
    val blockData = this.blockData
    if (blockData is Openable) {
        blockData.isOpen = open
        this.blockData = blockData
    }
}

fun Block.toggleOpenable() {
    val blockData = this.blockData
    if (blockData is Openable) {
        blockData.isOpen = !blockData.isOpen
        this.blockData = blockData
    }
}

fun Block.powerAsLever(open: Boolean): Boolean {
    val blockData = this.blockData
    if (blockData is Switch) {
        blockData.isPowered = open
        this.blockData = blockData
        return true
    }
    return false
}

fun Block.openAsPiston(open: Boolean): Boolean {
    val blockData = this.blockData
    if (blockData is Piston) {
        blockData.isExtended = open
        this.blockData = blockData
        return true
    }
    return false
}

fun Block.power(open: Boolean): Boolean {
    if (powerAsLever(open))
        return true
    if (openAsPiston(open))
        return true
    return false
}

fun Block.changeType(material: Material) {
    if (this.type != material) {
        this.type = material
    }
}

fun Block.spawnParticlesAround(
    particle: Particle = Particle.END_ROD,
    amount: Int = 1,
    offsetX: Double = 0.0,
    offsetY: Double = 0.0,
    offsetZ: Double = 0.0,
    speed: Double = 0.0,
    data: Any? = null,
) {
    for (x in 0..3) {
        for (y in 0..3) {
            for (z in 0..3) {
                world.spawnParticle(
                    particle,
                    this.x + x * 0.33,
                    this.y + y * 0.33,
                    this.z + z * 0.33,
                    amount,
                    offsetX,
                    offsetY,
                    offsetZ,
                    speed,
                    data,
                )
            }
        }
    }
}

fun Block.updateState(force: Boolean = false, applyPhysics: Boolean = true, action: BlockState.() -> Unit): Block {
    state.apply(action).apply { update(force, applyPhysics) }
    return this
}