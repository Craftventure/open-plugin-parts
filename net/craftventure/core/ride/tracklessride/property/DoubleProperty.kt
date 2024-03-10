package net.craftventure.core.ride.tracklessride.property

import kotlin.reflect.KMutableProperty0

open class DoubleProperty(
    override val id: String,
    open var value: Double
) : Property {
    companion object {
        fun ofDelegate(id: String, property: KMutableProperty0<Double>): DoubleProperty {
            return object : DoubleProperty(id, property.get()) {
                override var value by property
            }
        }
    }
}