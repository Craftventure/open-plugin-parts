package net.craftventure.core.animation.armature

class JointKeyFrame(
    val interpolation: String,
    val transform: Matrix4x4,
    val time: Double
) {
    fun clone() = JointKeyFrame(
        interpolation,
        transform.clone(),
        time
    )
}