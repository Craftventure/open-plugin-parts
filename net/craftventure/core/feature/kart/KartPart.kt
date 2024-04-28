package net.craftventure.core.feature.kart

import net.craftventure.core.ktx.extension.orElse
import net.craftventure.core.ktx.extension.toOptional
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor

interface KartPart {
    val id: String
    val extends: String?

    fun isValid(): Boolean
}

interface NamedPart {
    val displayName: String
}

fun <T : KartPart> T?.requireValid(): T {
    if (this == null) throw IllegalStateException("No value set")
    if (!isValid()) throw IllegalStateException("${this::class.java.simpleName} invalid: '$this'")
    return this
}

infix fun <T : KartPart> T.merge(mapping: KProperty1<T, *>.() -> Any?): T {
    //data class always has primary constructor ---v
    val constructor = this::class.primaryConstructor!!
    //calculate the property order
    val order = constructor.parameters.mapIndexed { index, it -> it.name to index }
        .associate { it }

    // merge properties
    @Suppress("UNCHECKED_CAST")
    val merged = (this::class as KClass<T>).declaredMemberProperties
        .sortedWith(compareBy { order[it.name] })
        .map { it.mapping() }
        .toTypedArray()

//    Logger.debug("Calling constructor with [\n${merged.joinToString(",\n")}\n]")

    return constructor.call(*merged)
}

infix fun <T : KartPart> T.extendFrom(right: T): T {
    val left = this
    return left merge mapping@{
        val leftValue = this.get(left)
        val rightValue = this.get(right)
        val isOptional = leftValue is Optional<*> || rightValue is Optional<*>
        return@mapping if (isOptional) {
            val actualLeft = (leftValue as? Optional<*>).orElse()
            val actualRight = (rightValue as? Optional<*>).orElse()
//            Logger.debug("${this.name}: Optional<${this.typeParameters}> with left=${leftValue}/${actualLeft}/${actualLeft is KartPart} vs right=${rightValue}/${actualRight}/${actualRight is KartPart}")
            if (actualLeft is KartPart && actualRight is KartPart) {
                (actualLeft extendFrom actualRight).toOptional()
            } else
                leftValue ?: rightValue
//                (actualLeft ?: actualRight).toOptional()
        } else
            leftValue ?: rightValue
    }
}

fun <T : KartPart> T.resolve(options: List<T>): T {
    var new = this
    val hasTried = mutableSetOf<String>()
    var last: T = new
//    Logger.debug("Resolving ${last.extends} with ${options.joinToString(", ") { it.id }}")
    while (last.extends != null && last.extends !in hasTried) {
        val other = options.first { it.id == last.extends }
        new = new extendFrom other
//        Logger.debug("Extending from ${other.id}")
        last = other
        hasTried.add(new.extends!!)
    }
    return new
}

fun <T : KartPart> List<T>.resolve(id: String): T? {
    val initial = this.firstOrNull { it.id == id } ?: return null
    return initial.resolve(this)
}