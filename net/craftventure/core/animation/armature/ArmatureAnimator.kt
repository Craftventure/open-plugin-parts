package net.craftventure.core.animation.armature

import net.craftventure.core.ktx.extension.clamp

class ArmatureAnimator(
    val armature: Armature
) {
    var duration = armature.joints.flatMap { it.childrenRecursive() + it }
        .mapNotNull { it.animation.lastOrNull()?.time }
        .maxOrNull() ?: 0.0
        private set
    var allJoints = armature.joints.flatMap { it.childrenRecursive() + it }
        private set

    private var time: Double = 0.0
    private var quatA = Quaternion()
    private var vecA = Vector3()
    private var matA = Matrix4x4()
    private var quatB = Quaternion()
    private var vecB = Vector3()
    private var matB = Matrix4x4()

    fun resetAnimationTransforms() {
        allJoints.forEach {
            it.animatedTransform.set(it.transform)
        }
    }

    fun invalidateArmature() {
        allJoints = armature.joints.flatMap { it.childrenRecursive() + it }
        duration = armature.joints.flatMap { it.childrenRecursive() + it }
            .mapNotNull { it.animation.lastOrNull()?.time }
            .maxOrNull() ?: 0.0
    }

    fun setTime(
        time: Double,
        resetTransform: Boolean = true,
        preAnimatedTransformHandler: JointHandler? = null,
        postAnimatedTransformHandler: JointHandler? = null
    ) {
        this.time = time

        if (resetTransform)
            armature.animatedTransform.set(armature.transform)

        armature.joints.forEach {
            update(it, armature.animatedTransform, preAnimatedTransformHandler, postAnimatedTransformHandler)
        }
    }

    private fun update(
        joint: Joint,
        parentTransform: Matrix4x4,
        preAnimatedTransformHandler: JointHandler?,
        postAnimatedTransformHandler: JointHandler?
    ) {
        val previousFrame = joint.animation.lastOrNull { it.time <= time }
        val nextFrame = joint.animation.firstOrNull { it.time > time }
        val progress =
            if (previousFrame != null && nextFrame != null) ((time - previousFrame.time) / (nextFrame.time - previousFrame.time)).clamp(
                0.01,
                1.0
            ) else null

//        Logger.debug(
//            "${joint.id} at ${time.format(2)} with progress=${progress?.format(2)} previous=${previousFrame != null} next=${nextFrame != null}"
//        )// original=(${joint.transform.rotation}, ${joint.transform.transformPoint(Vector3())}) " +
//                    "previous=$previousFrame(${previousFrame?.transform?.rotation}, ${previousFrame?.transform?.transformPoint(
//                        Vector3()
//                    )}) " +
//                    "next=$nextFrame(${nextFrame?.transform?.rotation}, ${nextFrame?.transform?.transformPoint(Vector3())})"
//        )

        joint.animatedTransform.set(parentTransform)
        joint.animatedTransform.multiply(joint.transform)
        if (previousFrame != null && nextFrame != null) {
//            Logger.debug("${joint.id} Frames")
//            Logger.debug(
//                "Progress ${progress.format(2)} for time ${time.format(2)} for frames ${previousFrame.time.format(
//                    2
//                )} ${nextFrame.time.format(2)}"
//            )

            vecA.reset()
            vecB.reset()
            previousFrame.transform.transformPoint(vecA)
            nextFrame.transform.transformPoint(vecB)

            vecA.interpolateWith(vecB, progress!!)

            previousFrame.transform.getRotation(quatA)
            nextFrame.transform.getRotation(quatB)

            quatA.interpolateWith(quatB, progress)

            matA.setIdentity()
            matA.translate(vecA)
            quatA.setTo(matB)
            matA.multiply(matB)

            preAnimatedTransformHandler?.onHandleJoint(joint)
            joint.animatedTransform.set(parentTransform)
            joint.animatedTransform.multiply(matA)
            postAnimatedTransformHandler?.onHandleJoint(joint)
//            joint.animatedTransform.multiply(previousFrame.transform)
        } else if (previousFrame != null) {
//            Logger.debug("${joint.id} Previous")

            vecA.reset()

            previousFrame.transform.transformPoint(vecA)
            previousFrame.transform.getRotation(quatA)

            matA.setIdentity()
            matA.translate(vecA)
            quatA.setTo(matB)
            matA.multiply(matB)

            preAnimatedTransformHandler?.onHandleJoint(joint)
            joint.animatedTransform.set(parentTransform)
            joint.animatedTransform.multiply(matA)
            postAnimatedTransformHandler?.onHandleJoint(joint)
        } else if (nextFrame != null) {
//            Logger.debug("${joint.id} Next")

            vecA.reset()

            nextFrame.transform.transformPoint(vecA)
            nextFrame.transform.getRotation(quatA)

            matA.setIdentity()
            matA.translate(vecA)
            quatA.setTo(matB)
            matA.multiply(matB)

            preAnimatedTransformHandler?.onHandleJoint(joint)
            joint.animatedTransform.set(parentTransform)
            joint.animatedTransform.multiply(matA)
            postAnimatedTransformHandler?.onHandleJoint(joint)
        } else {
//            Logger.debug("${joint.id} Rest")
            preAnimatedTransformHandler?.onHandleJoint(joint)
            joint.animatedTransform.set(parentTransform)
            joint.animatedTransform.multiply(joint.transform)
            postAnimatedTransformHandler?.onHandleJoint(joint)
        }

        joint.childJoints.forEach {
            update(
                it,
                joint.animatedTransform,
                preAnimatedTransformHandler,
                postAnimatedTransformHandler
            )
        }

//        Logger.debug(
//            ChatColor.GREEN + "A: ${joint.id}: ${joint.transform.rotation}/${joint.transform.transformPoint(
//                Vector3()
//            )}"
//        )
//        Logger.debug(
//            ChatColor.RED + "B: ${joint.id}: ${joint.animatedTransform.rotation}/${joint.animatedTransform.transformPoint(
//                Vector3()
//            )}"
//        )
    }

    fun interface JointHandler {
        fun onHandleJoint(joint: Joint)
    }
}