package net.craftventure.bukkit.ktx.extension

import org.bukkit.map.MapCanvas

fun MapCanvas.drawHorizontalLine(xStart: Int, xEnd: Int, y: Int, color: Byte) {
    for (x in xStart until xEnd) {
        setPixel(x, y, color)
    }
}