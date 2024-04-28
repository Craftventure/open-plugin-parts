package net.craftventure.core.feature.minigame.lasergame.gun

import net.craftventure.core.feature.minigame.lasergame.LaserGameItem

enum class LaserGameGunType(val displayName: String, val factory: () -> LaserGameItem) {
    DEFAULT("Handgun", factory = { DefaultGun() }),
    SNIPER("Sniper", factory = { SniperGun() }),
    SHOTGUN("Shotgun", factory = { ShotGun() }),
    BAZOOKA("Bazooka", factory = { BazookaGun() })
}