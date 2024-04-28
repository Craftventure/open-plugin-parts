package net.craftventure.bukkit.ktx.util

import com.squareup.moshi.JsonClass
import org.bukkit.util.BoundingBox

interface BoundingBoxProducer {
    fun create(): BoundingBox

    @JsonClass(generateAdapter = true)
    data class SizedProducer(
        val width: Double,
        val height: Double,
        val heightCentered: Boolean = true,
    ) : BoundingBoxProducer {
        override fun create(): BoundingBox = BoundingBox(
            -width * 0.5,
            if (heightCentered) -height * 0.5 else 0.0,
            -width * 0.5,
            width * 0.5,
            if (heightCentered) height * 0.5 else height,
            width * 0.5,
        )
    }

    @JsonClass(generateAdapter = true)
    data class SquareSizedProducer(
        val size: Double,
        val heightCentered: Boolean = true,
    ) : BoundingBoxProducer {
        override fun create(): BoundingBox = BoundingBox(
            -size * 0.5,
            if (heightCentered) -size * 0.5 else 0.0,
            -size * 0.5,
            size * 0.5,
            if (heightCentered) size * 0.5 else size,
            size * 0.5,
        )
    }
}