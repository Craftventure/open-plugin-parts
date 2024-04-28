package net.craftventure.core.ride.trackedride

import net.craftventure.audioserver.spatial.SpatialAudio
import net.craftventure.backup.service.exhaustive
import net.craftventure.core.ktx.extension.clamp
import net.craftventure.core.ktx.extension.equalsWithPrecision
import net.craftventure.core.ktx.util.Logger
import org.bukkit.Bukkit
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class SpatialTrainSounds(val name: String, val rideTrain: RideTrain, val settings: Settings) {
    private val sounds = ConcurrentHashMap<LinkedSound, SpatialAudio>()
    private val lastSounds = ConcurrentHashMap<RideCar, SpatialAudio>()

    private val isDebug: Boolean
        get() = DEBUG && (debugName == null || name == debugName)

    private val debugTag = "$name/${rideTrain.trainId}"

    //    private val start = System.currentTimeMillis()
    private var started = false

    private fun getCarSound(
        rideCar: RideCar,
        targetType: TrackSegment.TrackType
    ): LinkedSound? {
        val key =
            sounds.keys.firstOrNull { it.car === rideCar && it.trackType == targetType }
        if (key != null) {
//            if (isDebug)
//                Logger.debug("Sound retrieved for $debugTag car ${rideCar.carId} and key=$key")
            return key
        }
        return null
    }

    private fun getOrCreateCarSound(
        rideCar: RideCar,
        targetType: TrackSegment.TrackType
    ): LinkedSound {
        val key =
            sounds.keys.firstOrNull { it.car === rideCar && it.trackType == targetType }
        if (key != null) {
//            if (isDebug)
//                Logger.debug("Sound retrieved for $debugTag car ${rideCar.carId} and key=$key")
            return key
        }
        val newKey = key ?: LinkedSound(rideCar, targetType)
        val targetSound = when (targetType) {
            TrackSegment.TrackType.DEFAULT -> "/sound/$name/roll.ogg"
            TrackSegment.TrackType.CHAIN_LIFT -> "/sound/$name/lift.ogg"
            TrackSegment.TrackType.LSM_LAUNCH -> "/sound/$name/lsm.ogg"
            TrackSegment.TrackType.WHEEL_TRANSPORT -> "/sound/$name/transport.ogg"
            TrackSegment.TrackType.MAG_BRAKE -> "/sound/$name/mag_brake.ogg"
            TrackSegment.TrackType.FRICTION_BRAKE -> "/sound/$name/brake.ogg"
        }.exhaustive
        val spatialAudio = SpatialAudio(
            rideCar.location.toLocation(Bukkit.getWorld("world")!!), targetSound,
            distance = 30.0,
            fadeOutStartDistance = 15.0
        )
        sounds[newKey] = spatialAudio
        spatialAudio.sync(SpatialAudio.SYNC_WITH_GROUP)
        if (started) {
            spatialAudio.start()
        }
        if (isDebug)
            Logger.debug("Sound created for $debugTag car ${rideCar.carId}: ${targetType}")
        return newKey
    }

    fun update() {
        if (!started) return

//        Logger.debug("Updating $debugTag")
        val speed = abs(CoasterMathUtils.btpToMps(rideTrain.velocity))
        val speedPlaybackFactor = (speed / 20.0).clamp(0.0, 1.2)

        val cars = rideTrain.cars
        if (cars.isEmpty()) return
        val firstCar = cars[0]
        updateForCar(
            firstCar,
            firstCar,
            firstCar.trackSegment!!.trackType!!,
            speed = speed,
            speedPlaybackFactor = speedPlaybackFactor
        )
        if (cars.size >= 2) {
            val lastCar = cars[cars.size - 1]
            if (lastCar.trackSegment!!.trackType == firstCar.trackSegment!!.trackType)
                stopForCar(lastCar)
            else
                updateForCar(
                    lastCar,
                    firstCar,
                    lastCar.trackSegment!!.trackType!!,
                    speed = speed,
                    speedPlaybackFactor = speedPlaybackFactor
                )
//            updateForCar(cars[cars.size - 1], speed, speedPlaybackFactor)
        }
    }

    private fun stopForCar(
        rideCar: RideCar,
        targetType: TrackSegment.TrackType = rideCar.trackSegment!!.trackType!!
    ) {
        val rollSound = getCarSound(rideCar, TrackSegment.TrackType.DEFAULT)
        if (rollSound != null)
            sounds.getValue(rollSound).pause()

        if (targetType != TrackSegment.TrackType.DEFAULT && targetType in settings.supports) {
            val sound = getCarSound(rideCar, targetType)
            if (sound != null) {
                val audio = sounds.getValue(sound)
                audio.pause()
            }
        }

        lastSounds[rideCar]?.pause()
        lastSounds.remove(rideCar)
    }

    private fun updateForCar(
        rideCar: RideCar,
        soundLocaton: RideCar = rideCar,
        targetType: TrackSegment.TrackType = rideCar.trackSegment!!.trackType!!,
        speed: Double,
        speedPlaybackFactor: Double
    ) {
        if (settings.hasRoll) {
            val audio = sounds.getValue(getOrCreateCarSound(rideCar, TrackSegment.TrackType.DEFAULT))
            audio.runInBatch(true) {
                if (speedPlaybackFactor.equalsWithPrecision(0.0)) {
                    audio.pause()
                } else {
                    audio.resume()
                    audio.setLocation(soundLocaton.location, false)

                    audio.refDistance(2.0)
                    audio.maxDistance(5.0 + (speed * 4.0).clamp(15.0, 80.0))

                    audio.rate((0.5 + speedPlaybackFactor).clamp(0.5, 2.0))

                    val t = speed / 15.0
                    if (speed < 15.0) {
                        audio.volume(t * t * 0.6)
                    } else {
                        audio.volume(1.0 * 0.6)
                    }
                }
            }
//            if (!speedPlaybackFactor.equalsWithPrecision(0.0))
//                audio.setLocation(soundLocaton.location,false)
        }

        if (targetType != TrackSegment.TrackType.DEFAULT && targetType in settings.supports) {
            val sound = getOrCreateCarSound(rideCar, targetType)
            val audio = sounds.getValue(sound)

            val lastSound = lastSounds[rideCar]
            if (lastSound !== audio) {
                if (lastSound != null && isDebug)
                    Logger.debug("Stopping $debugTag ${lastSound.soundUrl}")
                lastSound?.pause()
                lastSounds[rideCar] = audio
            }

            audio.runInBatch(true) {
                if (speedPlaybackFactor.equalsWithPrecision(0.0)) {
                    audio.pause()
                } else {
                    audio.resume()
                    audio.setLocation(soundLocaton.location, false)

                    audio.refDistance(1.0)
                    audio.maxDistance(5.0 + (speed * 4.0).clamp(15.0, 80.0))

                    when (sound.trackType) {
                        TrackSegment.TrackType.DEFAULT -> {
                        }
                        TrackSegment.TrackType.CHAIN_LIFT -> {
                            audio.volume(0.25)
                            audio.rate((0.8 + (speedPlaybackFactor * 2.0)).clamp(0.5, 2.0))
                        }
                        TrackSegment.TrackType.WHEEL_TRANSPORT -> {
                            audio.volume(0.25)
                        }
                        TrackSegment.TrackType.FRICTION_BRAKE -> {
                            audio.sync(SpatialAudio.SYNC_RESET_EVERY_START)
                            audio.volume(0.6)
                            loop(false)
                        }
                        TrackSegment.TrackType.MAG_BRAKE -> {
                            audio.sync(SpatialAudio.SYNC_RESET_EVERY_START)
                            audio.volume(0.6)
                            loop(false)
                        }
                        TrackSegment.TrackType.LSM_LAUNCH -> {
                            audio.rate((0.5 + speedPlaybackFactor).clamp(0.5, 2.0))

                            val t = speed / 15.0
                            if (speed < 15.0) {
                                audio.volume(t * t * 0.6)
                            } else {
                                audio.volume(1.0 * 0.6)
                            }
                        }
                    }
                }
            }
//            if (!speedPlaybackFactor.equalsWithPrecision(0.0))
//                audio.setLocation(soundLocaton.location)
        } else {
            val sound = lastSounds[rideCar]
            if (sound != null && isDebug)
                Logger.debug("Pausing $debugTag")
            sound?.pause()
            lastSounds.remove(rideCar)
        }
    }

    fun start() {
        if (isDebug)
            Logger.debug("Start $debugTag $started")
        if (started) return
        started = true
        sounds.values.forEach { it.start() }
    }

    fun stop() {
        if (isDebug)
            Logger.debug("Stop $debugTag $started")
        if (!started) return
        started = false
        sounds.values.forEach { it.stop() }
    }

//    fun release(){}

    data class LinkedSound(
        val car: RideCar,
        val trackType: TrackSegment.TrackType
    )

    data class Settings @JvmOverloads constructor(
        val supports: Set<TrackSegment.TrackType>,
        val hasRoll: Boolean = true
    )

    companion object {
        val DEBUG = false//CraftventureCore.isTestServer()
        val debugName: String? = "alphadera"
    }
}