package net.craftventure.core.animation

interface TranslationCurve<T, R> {
    fun translate(t: T): R
}