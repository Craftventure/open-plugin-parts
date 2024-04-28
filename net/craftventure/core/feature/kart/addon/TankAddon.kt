package net.craftventure.core.feature.kart.addon

import net.craftventure.core.animation.armature.Armature
import net.craftventure.core.animation.armature.Joint
import net.craftventure.core.feature.kart.Kart
import net.craftventure.core.ktx.extension.clamp

class TankAddon : KartAddon() {
    private var seatBarrel: Kart.VisualFakeSeat? = null

    private var lastYaw = 0.0
    private var lastPitch = 0.0

    override fun onStart(kart: Kart) {
        if (seatBarrel == null) seatBarrel = kart.npcs.firstOrNull { it.settings.id == "barrel" }

        seatBarrel?.settings?.matrixInterceptor = { kart, matrix ->
            matrix.rotateX(lastPitch.clamp(-30.0, 10.0))
        }
    }

    override fun onPreUpdate(kart: Kart) {
        super.onPreUpdate(kart)
        if (!kart.isParked()) {
            lastYaw = kart.player.location.yaw.toDouble()
            lastPitch = kart.player.location.pitch.toDouble()
        }
    }

    private var turretMatrix: Matrix4x4? = null
    private var turretBone: Joint? = null

    override fun onPreArmatureUpdate(kart: Kart, armature: Armature, interactor: Kart.PhysicsInteractor) {
        super.onPreArmatureUpdate(kart, armature, interactor)
        if (turretBone == null) turretBone = armature.find("Body/Turret")
        if (turretMatrix == null) turretMatrix = turretBone?.transform?.clone()

        val turretBone = turretBone ?: return
        val turretMatrix = turretMatrix ?: return

        turretBone.transform.apply {
            setIdentity()
            rotateY(-lastYaw + Math.toDegrees(interactor.body.angle))
            multiply(turretMatrix)
        }
    }
}