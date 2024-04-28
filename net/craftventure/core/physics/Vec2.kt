package net.craftventure.core.physics

import net.craftventure.core.ktx.extension.format

data class Vec2(
    var x: Double = 0.toDouble(),
    var y: Double = 0.toDouble()
) {
    constructor(v: Vec2) : this(v.x, v.y)

    constructor() : this(0.0, 0.0)

    val size: Double
        get() = Math.sqrt(x * x + y * y)

    fun set(x: Double, y: Double) {
        this.x = x
        this.y = y
    }

    fun set(v: Vec2) {
        this.x = v.x
        this.y = v.y
    }

    fun translate(tx: Double, ty: Double) {
        x += tx
        y += ty
    }

    fun add(v: Vec2) {
        this.x += v.x
        this.y += v.y
    }

    fun sub(v: Vec2) {
        this.x -= v.x
        this.y -= v.y
    }

    fun sub(x: Double, y: Double) {
        this.x -= x
        this.y -= y
    }

    fun scale(s: Double) {
        this.x *= s
        this.y *= s
    }

    fun normalize() {
        scale(1 / size)
    }

    fun dot(v: Vec2): Double {
        return x * v.x + y * v.y
    }

    fun cross(v: Vec2): Double {
        return x * v.y - y * v.x
    }

    fun cross(a: Double, v: Vec2) {
        this.x = -a * v.y
        this.y = a * v.x
    }

    fun asString() = "x=${x.format(2)} y=${y.format(2)}"

}
