package net.craftventure.bukkit.ktx.extension

import org.bukkit.util.BoundingBox

fun BoundingBox.atCenter(x: Double, y: Double, z: Double): BoundingBox {
    return BoundingBox(
        x - (this.widthX / 2.0),
        y - (this.height / 2.0),
        z - (this.widthZ / 2.0),
        x + (this.widthX / 2.0),
        y + (this.height / 2.0),
        z + (this.widthZ / 2.0)
    )
}