package net.craftventure.core.animation.curve

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonClass
import com.squareup.moshi.ToJson
import net.craftventure.core.animation.TranslationCurve

class LineairPointCurve(
    points: List<Vector2>
) : TranslationCurve<Double, Double> {
    init {
        if (points.isEmpty()) throw IllegalStateException("Cannot create an empty curve")
    }

    private val sortedPoints = points.sortedBy { it.x }

    override fun translate(t: Double): Double {
        val rangeStart = sortedPoints.findLast { it.x <= t }
        val rangeEnd = sortedPoints.find { it.x >= t }
        if (rangeStart == null && rangeEnd == null) throw IllegalStateException("No range found?")
        if (rangeStart == null && rangeEnd != null) return rangeEnd.y
        if (rangeStart != null && rangeEnd == null) return rangeStart.y
        if (rangeStart == null || rangeEnd == null) return 0.0
        if (rangeStart === rangeEnd) return rangeStart.y

        val translatedT = (t - rangeStart.x) / (rangeEnd.x - rangeStart.x)

//        println("translatedT=$translatedT")
        val deltaY = rangeEnd.y - rangeStart.y
//        println("delta=$deltaY")
//        println("range=$rangeStart/$rangeEnd")
        return rangeStart.y + (translatedT * deltaY)
    }

    class JsonAdapter {
        @FromJson
        fun fromJson(json: Json) =
            LineairPointCurve(json.values.entries.map {
                Vector2(it.key, it.value)
            })

        @ToJson
        fun toJson(instance: LineairPointCurve) =
            Json(instance.sortedPoints.associate { it.x to it.y })

        @JsonClass(generateAdapter = true)
        data class Json(
            val values: Map<Double, Double>
        )
    }

    companion object {
        val empty = LineairPointCurve(listOf(Vector2(0.0, 0.0)))
    }
}