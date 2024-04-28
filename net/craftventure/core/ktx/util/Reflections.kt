package net.craftventure.core.ktx.util

import java.lang.reflect.Field
import java.lang.reflect.InaccessibleObjectException

object Reflections {
    private val fields = hashMapOf<Class<*>, MutableMap<String, Field>>()

    @Throws(InaccessibleObjectException::class, SecurityException::class)
    fun getField(clazz: Class<*>, name: String): Field? {
        val map = fields[clazz].let {
            if (it == null) {
                val newMap = mutableMapOf<String, Field>()
                fields[clazz] = newMap
                newMap
            } else it
        }
        val field = map[name]
        if (field != null) {
            return field
        } else {
            val classField = clazz.getDeclaredField(name) ?: clazz.getField(name) ?: getField(clazz.superclass, name)
            if (classField != null) {
                classField.isAccessible = true
                map[name] = classField
                return classField
            }
        }
        return null
    }
}

fun Any.field(name: String) = Reflections.getField(this::class.java, name)
inline fun <reified T> Any.fieldValue(name: String) = this.field(name)!!.get(this) as T