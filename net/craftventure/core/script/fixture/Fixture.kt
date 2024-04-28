package net.craftventure.core.script.fixture

import net.craftventure.core.script.fixture.property.NumberProperty
import net.craftventure.core.script.fixture.property.ObjectProperty
import net.craftventure.core.script.timeline.Timeline

abstract class Fixture(
    val name: String,
    val location: Location,
    val kind: String
) {
    val properties = mutableListOf<ObjectProperty<*>>()
    val propertyTimelines = HashMap<String, Timeline<NumberProperty<*>>>()
//    val tags = mutableListOf<String>()

    protected fun <T> addProperty(property: ObjectProperty<T>) {
        properties.add(property)
    }

    fun getTimeline(objectProperty: ObjectProperty<*>) = getTimeline(objectProperty.name)

    fun getTimeline(propertyName: String): Timeline<NumberProperty<*>>? {
        propertyTimelines[propertyName]?.let { return it }

        val property = properties.firstOrNull { it.name == propertyName } ?: return null
        if (property is NumberProperty<*>) {
            val timeline = Timeline(property)
            propertyTimelines[property.name] = timeline
            return timeline
        }

        return null
    }

    abstract fun destroy()
}