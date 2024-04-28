package net.craftventure.core.script.fixture

import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import io.reactivex.subjects.BehaviorSubject
import net.craftventure.core.api.CvApi
import net.craftventure.core.ktx.extension.orElse
import net.craftventure.core.ktx.extension.toOptional
import net.craftventure.core.script.fixture.fountain.*
import net.craftventure.core.script.timeline.KeyFrame
import net.craftventure.core.script.timeline.KeyFrameEasing
import java.io.File
import java.io.FileReader
import java.util.*

class ComposedScene {
    val fixtures = mutableListOf<Fixture>()
    val selectedFixture = BehaviorSubject.createDefault(Optional.empty<Fixture>())
    val settings = BehaviorSubject.createDefault(Optional.empty<Settings>())

    fun addFixture(item: Fixture) {
        if (fixtures.firstOrNull { it.name == item.name } != null)
            throw IllegalStateException("An item with name '${item.name}' is already in included within the current scene")
        if (selectedFixture.value.orElse() == null)
            selectedFixture.onNext(Optional.of(item))
        fixtures.add(item)
    }

    fun load(file: File): ComposedScene {
        val reader = JsonReader(FileReader(file))
        val data = CvApi.gsonComposed.fromJson<SceneJson>(reader, object : TypeToken<SceneJson>() {}.type)
        settings.onNext(data.settings.toOptional())
//        println(CvApi.getComposedGson().toJson(data))

        val newFixtures = mutableListOf<Fixture>()
        for (jsonFixture in data.fixtures) {
//            println(jsonFixture.kind)
            when (jsonFixture.kind) {
                "fountain:shooter" -> {
                    val shooter = Shooter(jsonFixture.name, jsonFixture.location.toLocation())
                    newFixtures.add(shooter)

                    for (jsonProperty in jsonFixture.properties) {
                        val timeline = shooter.getTimeline(jsonProperty.name)!!
                        for (jsonKeyframe in jsonProperty.keyframes) {
                            timeline.addKeyframe(
                                KeyFrame(
                                    (jsonKeyframe.at * 1000).toLong(),
                                    jsonKeyframe.value,
                                    inEasing = jsonKeyframe.inEasing ?: KeyFrameEasing.NONE,
                                    outEasing = jsonKeyframe.outEasing ?: KeyFrameEasing.NONE
                                )
                            )
                        }
                    }
                }
                "fountain:supershooter" -> {
                    val shooter = SuperShooter(jsonFixture.name, jsonFixture.location.toLocation())
                    newFixtures.add(shooter)

                    for (jsonProperty in jsonFixture.properties) {
                        val timeline = shooter.getTimeline(jsonProperty.name)!!
                        for (jsonKeyframe in jsonProperty.keyframes) {
                            timeline.addKeyframe(
                                KeyFrame(
                                    (jsonKeyframe.at * 1000).toLong(),
                                    jsonKeyframe.value,
                                    inEasing = jsonKeyframe.inEasing ?: KeyFrameEasing.NONE,
                                    outEasing = jsonKeyframe.outEasing ?: KeyFrameEasing.NONE
                                )
                            )
                        }
                    }
                }
                "fountain:oarsmanjet" -> {
                    val shooter = OarsmanJet(jsonFixture.name, jsonFixture.location.toLocation())
                    newFixtures.add(shooter)

                    for (jsonProperty in jsonFixture.properties) {
                        val timeline = shooter.getTimeline(jsonProperty.name)!!
                        for (jsonKeyframe in jsonProperty.keyframes) {
                            timeline.addKeyframe(
                                KeyFrame(
                                    (jsonKeyframe.at * 1000).toLong(),
                                    jsonKeyframe.value,
                                    inEasing = jsonKeyframe.inEasing ?: KeyFrameEasing.NONE,
                                    outEasing = jsonKeyframe.outEasing ?: KeyFrameEasing.NONE
                                )
                            )
                        }
                    }
                }
                "fountain:lillyjet" -> {
                    val shooter = LillyJet(jsonFixture.name, jsonFixture.location.toLocation())
                    newFixtures.add(shooter)

                    for (jsonProperty in jsonFixture.properties) {
                        val timeline = shooter.getTimeline(jsonProperty.name)!!
                        for (jsonKeyframe in jsonProperty.keyframes) {
                            timeline.addKeyframe(
                                KeyFrame(
                                    (jsonKeyframe.at * 1000).toLong(),
                                    jsonKeyframe.value,
                                    inEasing = jsonKeyframe.inEasing ?: KeyFrameEasing.NONE,
                                    outEasing = jsonKeyframe.outEasing ?: KeyFrameEasing.NONE
                                )
                            )
                        }
                    }
                }
                "fountain:bloom" -> {
                    val shooter = Bloom(jsonFixture.name, jsonFixture.location.toLocation())
                    newFixtures.add(shooter)

                    for (jsonProperty in jsonFixture.properties) {
                        val timeline = shooter.getTimeline(jsonProperty.name)!!
                        for (jsonKeyframe in jsonProperty.keyframes) {
                            timeline.addKeyframe(
                                KeyFrame(
                                    (jsonKeyframe.at * 1000).toLong(),
                                    jsonKeyframe.value,
                                    inEasing = jsonKeyframe.inEasing ?: KeyFrameEasing.NONE,
                                    outEasing = jsonKeyframe.outEasing ?: KeyFrameEasing.NONE
                                )
                            )
                        }
                    }
                }
                else -> throw IllegalStateException("Unknown kind ${jsonFixture.kind}")
            }
        }

        fixtures.clear()
        fixtures.addAll(newFixtures)

        return this
    }

    private class SceneJson {
        var settings: Settings? = null
        val fixtures = mutableListOf<JsonFixture>()
    }

//    private class JsonSettings {
//        var x: Double? = 0.0
//        var y: Double? = 0.0
//        var z: Double? = 0.0
//        var waterLevel: Double? = 0.0
//    }

    private class JsonFixture {
        lateinit var name: String
        lateinit var kind: String
        lateinit var location: JsonLocation
        var properties = mutableListOf<JsonProperty>()
    }

    private class JsonLocation {
        var x: Double? = 0.0
        var y: Double? = 0.0
        var z: Double? = 0.0

        fun toLocation() = Location(x!!, y!!, z!!)
    }

    private class JsonProperty {
        lateinit var name: String
        var keyframes = mutableListOf<JsonKeyFrame>()
    }

    private class JsonKeyFrame {
        var at: Double = 0.0
        var value: Number = 0.0
        var inEasing: KeyFrameEasing? = null
        var outEasing: KeyFrameEasing? = null
    }

    class Settings {
        var x: Double? = 0.0
        var y: Double? = 0.0
        var z: Double? = 0.0
        var waterLevel: Double? = 0.0
        var duration: Double? = 0.0
    }
}