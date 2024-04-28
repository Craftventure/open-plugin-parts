package net.craftventure.core.animation.armature

class Armature(
    val id: String,
    val name: String,
    val joints: MutableList<Joint>,
    val transform: Matrix4x4,
    val doInitialTransform: Boolean = true
) {
    //    val inverse = Matrix4x4(baseMatrix).apply { invert() }
    val animatedTransform = Matrix4x4()

    init {
        if (doInitialTransform) {
            val matrix = Matrix4x4()
            matrix.rotate(Quaternion().rotateAxis(1.0, 0.0, 0.0, -90.0))
            matrix.multiply(transform)
            transform.set(matrix)
//        transform.multiply(matrix)
        }
    }

    fun allJoints() = joints.flatMap { it.childrenRecursive() + it }

    fun clone() = Armature(
        id,
        name,
        joints.map { it.clone() }.toMutableList(),
        transform.clone(),
        doInitialTransform = false
    ).also {
        it.animatedTransform.set(this.animatedTransform)
    }

    fun resetAnimatedTransformRecursively() {
        joints.forEach {
            it.resetAnimatedTransformRecursively()
        }
    }

    fun applyAnimatedTransformsRecursively() {
//        animatedTransform.set(transform)
        joints.forEach {
            it.applyAnimatedTransformsRecursively(animatedTransform)
        }
    }

    fun byId(id: String) = allJoints().find { it.name == id }

    fun find(path: String) = find(*path.split("/").toTypedArray())

    fun find(vararg path: String) = joints.map { it.find(*path) }.firstOrNull()

    override fun toString(): String {
        return "Armature(id='$id', name='$name', joints=${joints}, transform=$transform, animatedTransform=$animatedTransform)"
    }
}