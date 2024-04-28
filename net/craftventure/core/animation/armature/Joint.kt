package net.craftventure.core.animation.armature

class Joint(
    val id: String,
    val name: String,
    val transform: Matrix4x4,
    val childJoints: MutableList<Joint> = mutableListOf()
) {
    //    val inverseBindTransform = Matrix4x4(baseMatrix).apply { invert() }
    var animation = arrayOf<JointKeyFrame>()
    val animatedTransform = Matrix4x4()

//    init {
//        Logger.debug("$id: ${baseMatrix.rotation}/${baseMatrix.transformPoint(Vector3())}")
//    }

    fun resetAnimatedTransformRecursively() {
        animatedTransform.set(transform)
        childJoints.forEach {
            it.resetAnimatedTransformRecursively()
        }
    }

    fun applyAnimatedTransformsRecursively(parentTransform: Matrix4x4) {
        animatedTransform.set(parentTransform)
        animatedTransform.multiply(transform)

        childJoints.forEach { it.applyAnimatedTransformsRecursively(animatedTransform) }
    }

    fun find(vararg path: String): Joint? {
        if (path.isEmpty()) return null
        if (name != path[0]) return null
        if (path.size == 1) {
            return this
        }
        val joint = childJoints.firstOrNull { it.name == path[1] } ?: return null
        return joint.find(*path.drop(1).toTypedArray())
    }

    /**
     * @return All children excluding itself
     */
    fun childrenRecursive(): Set<Joint> {
//        Logger.debug("$id has ${childJoints.size} children")
        val joints = mutableSetOf<Joint>()
        childJoints.forEach {
            joints += it
//            Logger.debug("$id > ${it.id}")
            joints.addAll(it.childrenRecursive())
        }
        return joints
    }

    fun clone(): Joint = Joint(
        id = id,
        name = name,
        transform = transform.clone(),
        childJoints = childJoints.map { it.clone() }.toMutableList()
    ).also {
        it.animation = animation.map { it.clone() }.toTypedArray()
        it.animatedTransform.set(animatedTransform)
    }

    override fun toString(): String {
        return "Joint(id='$id', name='$name', transform=$transform, childJoints=${childJoints}, animation=${animation.contentToString()}, animatedTransform=$animatedTransform)"
    }
}