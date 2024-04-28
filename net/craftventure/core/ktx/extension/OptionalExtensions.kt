package net.craftventure.core.ktx.extension

import java.util.*

fun <T> Optional<T>?.orElse() = this?.orElse(null)

fun <T> T?.toOptional() = Optional.ofNullable(this)

val <T> Optional<T>?.force: T
    get() = this!!.get()!!