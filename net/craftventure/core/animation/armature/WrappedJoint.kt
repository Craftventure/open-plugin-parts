package net.craftventure.core.animation.armature

class WrappedJoint(
    val joint: Joint,
) {
    private val originalTransform: Matrix4x4 = Matrix4x4(joint.transform)

    fun reset() {
        joint.transform.set(originalTransform)
    }
}