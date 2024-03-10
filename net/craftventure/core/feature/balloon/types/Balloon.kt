package net.craftventure.core.feature.balloon.types

import net.craftventure.core.feature.balloon.holders.BalloonHolder
import org.bukkit.Location


interface Balloon {
    val balloonHolder: BalloonHolder?

    val balloonLocation: Location?
    val leashLength: Double get() = 1.8

    fun spawn(balloonHolder: BalloonHolder)
    fun update()
    fun despawn(withEffects: Boolean)
}
