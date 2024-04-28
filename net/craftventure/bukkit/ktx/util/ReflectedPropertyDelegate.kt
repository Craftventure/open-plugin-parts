package net.craftventure.bukkit.ktx.util

import net.craftventure.core.ktx.util.Reflections
import java.lang.reflect.Field
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class ReflectedPropertyDelegate<T>(val name: String) : ReadWriteProperty<Any, T> {
    private var field: Field? = null

    private fun getField(instance: Any): Field {
        if (field != null) return field!!
        field = Reflections.getField(instance::class.java, name)!!
        return field!!
    }

    override operator fun getValue(thisRef: Any, property: KProperty<*>): T {
        return getField(thisRef).get(thisRef) as T
    }

    override operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        getField(thisRef).set(thisRef, value)
    }
}

fun <T> Any.reflectionField(name: String) = ReflectedPropertyDelegate<T>(name)