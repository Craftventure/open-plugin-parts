package net.craftventure.bukkit.ktx.nbs

import net.craftventure.core.ktx.nbs.NbsFile
import net.craftventure.core.ktx.nbs.NoteUtils
import net.craftventure.core.ktx.util.BackgroundService

class NbsPlayer(
    val nbsFile: NbsFile,
    val soundDelegate: SoundDelegate,
) {
    private var blocks = nbsFile.notes
    private var tick = 0
    private var blockIndex = 0
    private var tickPart = 0.0

    private val animator = object : BackgroundService.Animatable {
        override fun onAnimationUpdate() {
            val tickTempo = 20.0 / nbsFile.tempo
            tickPart += 1.0
            var tickDelta = 0
            while (tickPart >= tickTempo) {
                tickDelta++
                tickPart -= tickTempo
            }
            if (tickDelta == 0) return
//            logcat { "Updating with ${blocks.size}" }

            for (block in blocks.drop(blockIndex)) {
                if (block.atTick > tick) {
                    break
                }
                blockIndex++

                val instrument = block.instrument
                val sound = instrument.getSound()

//                logcat { "Delegating sound at $tick $sound" }

//                val volume: Float = (layer.getVolume() * this.volume as Int * playerVolume as Int / 1000000f
//                        * (1f / 16f * getDistance()))
                val volume = 1f
                val pitch = NoteUtils.getPitchTransposed(block)// NotePitch.getPitch(note.getKey() - 33)

                soundDelegate.playSound(block, sound, volume, pitch)
            }

            tick += tickDelta
            if (tick >= nbsFile.lengthInTicks) {
                if (nbsFile.loop) {
                    tick = nbsFile.loopStartTicks
                    blockIndex = blocks.indexOfFirst { it.atTick >= tick }
                } else {
                    stop()
                }
            }
        }
    }

    fun start() {
        blockIndex = 0
        BackgroundService.add(animator)
    }

    fun stop() {
        BackgroundService.remove(animator)
    }

    fun interface SoundDelegate {
        fun playSound(block: NbsFile.NoteBlock, sound: String, volume: Float, pitch: Float)
    }
}