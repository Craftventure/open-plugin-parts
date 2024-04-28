package net.craftventure.core.ride.trackedride

import org.bukkit.util.Vector

class SplineHandle {
    @Transient
    private var handle = Vector(0, 0, 0)

    constructor(`in`: SplineHandle) {
        handle = `in`.handle.clone()
    }

    constructor(x: Double, y: Double, z: Double) {
        handle.x = x
        handle.y = y
        handle.z = z
    }

    constructor(vector: Vector) {
        handle.x = vector.x
        handle.y = vector.y
        handle.z = vector.z
    }

    var x: Double by handle::x
    var y: Double by handle::y
    var z: Double by handle::z

    fun addOffset(x: Double, y: Double, z: Double): SplineHandle {
        handle.x = handle.x + x
        handle.y = handle.y + y
        handle.z = handle.z + z
        return this
    }

    fun toVector(): Vector {
        return handle
    }
}