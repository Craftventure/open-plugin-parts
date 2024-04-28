package net.craftventure.core.feature.kart.addon

import net.craftventure.core.animation.armature.Armature
import net.craftventure.core.feature.kart.Kart

abstract class KartAddon {
    open fun onStart(kart: Kart) {}
    open fun onDestroy(kart: Kart) {}
    open fun onPreUpdate(kart: Kart) {}
    open fun onPostUpdate(kart: Kart) {}
    open fun onPreSeatUpdate(kart: Kart) {}
    open fun onPreArmatureUpdate(kart: Kart, armature: Armature, interactor: Kart.PhysicsInteractor) {}
    open fun onPostArmatureUpdate(kart: Kart, armature: Armature, interactor: Kart.PhysicsInteractor) {}

    companion object {
        val REGISTER = hashMapOf<String, Class<out KartAddon>>()

        fun reverseRegister(controller: KartAddon) = REGISTER.entries
            .firstOrNull { it.value.isInstance(controller) }?.key

        init {
            REGISTER["tank"] = TankAddon::class.java
            REGISTER["atat"] = AtAtAddon::class.java
        }
    }
}