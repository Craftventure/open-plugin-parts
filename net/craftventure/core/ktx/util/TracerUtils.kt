package net.craftventure.core.ktx.util

object TracerUtils {
    @JvmStatic
    fun getRootCause(throwable: Throwable?): Throwable? {
        return if (throwable!!.cause != null) {
            getRootCause(throwable.cause)
        } else throwable
    }
}