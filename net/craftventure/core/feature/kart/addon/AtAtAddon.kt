package net.craftventure.core.feature.kart.addon

import net.craftventure.bukkit.ktx.extension.asString
import net.craftventure.core.animation.armature.Armature
import net.craftventure.core.animation.armature.ArmatureAnimator
import net.craftventure.core.animation.armature.WrappedJoint
import net.craftventure.core.feature.kart.Kart
import net.craftventure.core.ktx.extension.clamp
import net.craftventure.core.ktx.util.AngleUtils
import net.craftventure.core.ktx.util.Logger

class AtAtAddon : KartAddon() {
    private var animator: ArmatureAnimator? = null
    private val start = System.currentTimeMillis()
    private var lastTime = start
    private var animationTime = 0.0

    private var neckJoint: WrappedJoint? = null
    private var headConnectorJoint: WrappedJoint? = null

    private fun requireAnimator(armature: Armature) {
        if (animator == null) {
            animator = ArmatureAnimator(armature)
        }
    }

    override fun onPostArmatureUpdate(kart: Kart, armature: Armature, interactor: Kart.PhysicsInteractor) {
        requireAnimator(armature)

        if (animator!!.duration == 0.0) return

        val now = System.currentTimeMillis()
        val delta = now - lastTime
        lastTime = now

        // animation moves 3.0 blocks in 5.0 seconds)
        val deltaMultiplier = (kart.currentSpeed * 20) / (3.0 / 5.0)

        animationTime += (delta / 1000.0) * deltaMultiplier

//        var time = (now - start) / 1000.0
        while (animationTime > animator!!.duration) {
            animationTime -= animator!!.duration
        }
        while (animationTime < 0) {
            animationTime += animator!!.duration
        }


//        Logger.debug("=====================================")
//        Logger.debug("Updating armature atat to ${animationTime.format(2)}")

//        animator!!.invalidateArmature()
        armature.animatedTransform.set(interactor.matrix)
        armature.animatedTransform.multiply(armature.transform)


        val neckJoint = neckJoint ?: animator!!.allJoints.first { it.name == "Neck" }
            .let { WrappedJoint(it) }
            .also { neckJoint = it }
        val headConnectorJoint = headConnectorJoint ?: animator!!.allJoints.first { it.name == "SeatConnector" }
            .let { WrappedJoint(it) }
            .also { headConnectorJoint = it }

        if (!kart.isParked()) {
            val angleOfDriver = AngleUtils.clampDegrees(kart.player.location.yaw.toDouble())
            val angleOfKart = AngleUtils.clampDegrees(Math.toDegrees(interactor.body.angle))
            val diff = AngleUtils.distance(angleOfKart, angleOfDriver)
            val clampedNeckDiff = diff.clamp(-30.0, 30.0)
            val clampedHeadDiff = diff.clamp(-60.0, 60.0)

//            Logger.debug(
//                "driver=${angleOfDriver.format(2)} " +
//                        "body=${interactor.body.angle.format(2)} " +
//                        "kart=${angleOfKart.format(2)} " +
//                        "diff=${diff.format(2)} " +
//                        "neck=${clampedNeckDiff.format(2)} " +
//                        "head=${clampedHeadDiff.format(2)}"
//            )

//            neckJoint.reset()
//            neckJoint.joint.transform.rotateZ(clampedNeckDiff)
//
//            headConnectorJoint.reset()
//            if (clampedHeadDiff != clampedNeckDiff) {
//                headConnectorJoint.joint.transform.rotateZ(clampedHeadDiff - clampedNeckDiff)
//            }
//            headConnectorJoint.joint.transform.rotateX(kart.player.location.pitch.toDouble().clamp(-25.0, 50.0))

            animator!!.setTime(animationTime, resetTransform = false, postAnimatedTransformHandler = { joint ->
                when {
                    joint === neckJoint.joint -> {
                        joint.animatedTransform.rotateZ(clampedNeckDiff)
                    }
                    joint === headConnectorJoint.joint -> {
                        if (clampedHeadDiff != clampedNeckDiff) {
                            joint.animatedTransform.rotateZ(clampedHeadDiff - clampedNeckDiff)
                        }
                        joint.animatedTransform.rotateX(kart.player.location.pitch.toDouble().clamp(-25.0, 50.0))
                    }
                }
            })
        } else {
            animator!!.setTime(animationTime, resetTransform = false)
        }


        if (false)
            animator!!.allJoints.forEach {
                Logger.debug(
                    "Joint ${it.name.padEnd(20)}: ${
                        it.transform.toVector().asString(1)
                    } ${it.transform.rotation.yawPitchRoll.asString(2)} to ${
                        it.animatedTransform.toVector().asString(1)
                    } ${it.animatedTransform.rotation.yawPitchRoll.asString(2)}"
                )
            }
    }
}