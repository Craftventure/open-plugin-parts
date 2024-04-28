package net.craftventure.bukkit.ktx.extension

import net.craftventure.core.ktx.extension.format
import org.bukkit.Location
import org.bukkit.util.Vector
import kotlin.math.absoluteValue


fun Vector.asString(digits: Int = 2): String = "x=${x.format(digits)} y=${y.format(digits)} z=${z.format(digits)}"

fun Vector.inverted() = Vector(-x, -y, -z)

fun Vector.set(source: Vector): Vector {
    this.x = source.x
    this.y = source.y
    this.z = source.z
    return this
}

operator fun Vector.plus(source: Vector): Vector {
    val vector = clone()
    vector += source
    return vector
}

operator fun Vector.plusAssign(source: Vector) {
    this.x += source.x
    this.y += source.y
    this.z += source.z
}

fun Vector.set(source: Location): Vector {
    this.x = source.x
    this.y = source.y
    this.z = source.z
    return this
}

fun Vector.reset() {
    this.x = 0.0
    this.y = 0.0
    this.z = 0.0
}

fun Vector.set(x: Double, y: Double, z: Double): Vector {
    this.x = x
    this.y = y
    this.z = z
    return this
}

fun Vector.add(x: Double, y: Double, z: Double): Vector {
    this.x = this.x + x
    this.y = this.y + y
    this.z = this.z + z
    return this
}

fun Vector.rotateZ(angle: Double) {
    val nx = x * Math.cos(angle) - y * Math.sin(angle)
    val ny = x * Math.sin(angle) + y * Math.cos(angle)
    val nz = z
    set(nx, ny, nz)
}

fun Vector.rotateY(angle: Double) {
    val nz = z * Math.cos(angle) - x * Math.sin(angle)
    val nx = z * Math.sin(angle) + x * Math.cos(angle)
    val ny = y
    set(nx, ny, nz)
}

fun Vector.rotateY(angle: Double, pivot: Vector): Vector {
    this.x -= pivot.x
    this.z -= pivot.z

    rotateY(angle)

    this.x += pivot.x
    this.z += pivot.z

    return this
}

fun Vector.rotateX(angle: Double) {
    val ny = y * Math.cos(angle) - z * Math.sin(angle)
    val nz = y * Math.sin(angle) + z * Math.cos(angle)
    val nx = x
    set(nx, ny, nz)
}


fun Vector.getRelativeAngleBetween(v: Vector): Double {
    return getSign(v) * Math.acos(dot(v) / (length() * v.length()))
}

// http://www.oocities.org/pcgpe/math2d.html
// http://gamedev.stackexchange.com/questions/45412/understanding-math-used-to-determine-if-vector-is-clockwise-counterclockwise-f
fun Vector.getSign(v: Vector): Int {
    return if (y * v.x > x * v.y) -1 else 1
}


fun Vector.rotateVectorTopdown(radians: Double): Vector {
    val ca = Math.cos(radians)
    val sa = Math.sin(radians)
    x = ca * this.x - sa * this.z
    y = this.y
    z = sa * this.x + ca * this.z
    return this
}

fun Vector.setYawPitchDegrees(yaw: Double, pitch: Double): Vector {
    return setYawPitchRadians(Math.toRadians(yaw), Math.toRadians(pitch))
}

fun Vector.setYawPitchRadians(yaw: Double, pitch: Double): Vector {
    val xz = Math.cos(pitch)
    x = (-xz * Math.sin(yaw))
    y = (-Math.sin(pitch))
    z = (xz * Math.cos(yaw))
    return this
}

fun Vector.absSum() = this.x.absoluteValue + this.y.absoluteValue + this.z.absoluteValue

fun Vector.absSumAvg() = this.absSum() / 3.0