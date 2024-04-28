package net.craftventure.bukkit.ktx.extension

import net.minecraft.world.phys.AABB


fun AABB.overlaps(other: AABB): Boolean {
    return this.intersects(other)
}

@Deprecated("Does this actually function properly?")
fun AABB.add(x: Double, y: Double, z: Double): AABB {
    return this.contract(x, y, z)
}

fun AABB.xMin() = this.minX
fun AABB.yMin() = this.minY
fun AABB.zMin() = this.minZ
fun AABB.xMax() = this.maxX
fun AABB.yMax() = this.maxY
fun AABB.zMax() = this.maxZ