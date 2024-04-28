package net.craftventure.core.ktx.nbs

import net.craftventure.core.ktx.util.EndianDataInputStream
import okio.ByteString.Companion.readByteString
import java.io.File
import java.nio.ByteOrder

data class NbsFile(
    val name: String?,
    val author: String?,
    val originalAuthor: String?,
    val description: String?,
    val lengthInTicks: Int,
    val tempo: Double,
    val loop: Boolean,
    val maxLoop: Int,
    val loopStartTicks: Int,
    val instruments: List<BaseInstrument>,
    val notes: List<NoteBlock>,
) {
    companion object {
        @Throws
        fun readFrom(file: File): NbsFile {
            file.inputStream().use { inputStream ->
                EndianDataInputStream(inputStream, ByteOrder.LITTLE_ENDIAN).use {

                    require(it.readShort() == 0.toShort()) { "NBS from old app version, please resave it" }
                    val version = it.readByte()
                    require(version == 5.toByte()) { "Only version 5 is supported" }
                    val vanillaInstrumentCount = it.readByte()
                    val songLengthInTicks = it.readShort()
                    val layerCount = it.readShort()
                    val songName = it.readSizedString()
                    val author = it.readSizedString()
                    val originalAuthor = it.readSizedString()
                    val description = it.readSizedString()
                    val tempo = it.readShort()
                    it.skipBytes(2)
                    val timeSignature = it.readByte()
                    it.skipBytes(4 + 4 + 4 + 4 + 4)
                    val midiSource = it.readSizedString()
                    val loop = it.readByte()
                    val maxLoop = it.readByte()
                    val loopStartTick = it.readShort()

//                println("version=$version count=$vanillaInstrumentCount length=$songLengthInTicks layer=$layerCount name=$songName by $author or $originalAuthor with $description from $midiSource")
//                println("loop=$loop maxLoop=$maxLoop loopStart=$loopStartTick tempo=$tempo timeSignature=$timeSignature")

                    val noteblocks = mutableListOf<UnlinkedNoteBlock>()
                    var tick = -1
                    while (true) {
                        val jumpTicks = it.readShort().toInt()
                        if (jumpTicks == 0) {
                            break;
                        }
                        tick += jumpTicks

                        var layer = -1
                        while (true) {
                            val jumpLayers = it.readShort().toInt()
                            if (jumpLayers == 0) {
                                break
                            }
                            layer += jumpLayers

                            val instrument = it.readByte()
                            val key = it.readByte()

                            val velocity = it.readByte()
                            val panning = it.readByte()
                            val pitch = it.readShort()

//                        println("Player $instrument ")
                            noteblocks += UnlinkedNoteBlock(
                                tick,
                                instrument,
                                key,
                                velocity,
                                panning,
                                pitch,
                            )
                        }
                    }

                    for (layerIndex in 0 until layerCount) {
                        val name = it.readSizedString()
                        val locked = it.readByte()
                        val volume = it.readByte()
                        val stereo = it.readByte()

//                    println("Layer $name with volume $volume (stereo=$stereo)")
                    }

                    val instruments = mutableListOf<BaseInstrument>()
                    VanillaInstrumentType.values().take(vanillaInstrumentCount.toInt())
                        .forEachIndexed { index, vanillaInstrumentType ->
                            instruments += VanillaInstrument(index, vanillaInstrumentType)
                        }

                    val customInstrumentCount = it.readByte()
                    for (customInstrumentIndex in 0 until customInstrumentCount) {
                        val name = it.readSizedString()
                        val sound = it.readSizedString()!!
                        val pitch = it.readByte()
                        val pressPianoKey = it.readByte()

                        instruments += CustomInstrument(
                            vanillaInstrumentCount + customInstrumentIndex,
                            name,
                            sound,
                            pitch
                        )
//                    println("Instrument $name by $sound with pitch $pitch")
                    }

                    return NbsFile(
                        songName,
                        author,
                        originalAuthor,
                        description,
                        songLengthInTicks.toInt(),
                        tempo / 100.0,
                        loop == 1.toByte(),
                        maxLoop.toInt(),
                        loopStartTick.toInt(),
                        instruments,
                        noteblocks.map { block ->
                            NoteBlock(
                                block.atTick,
                                instruments.first { it.id == block.instrument.toInt() },
                                block.key,
                                block.velocity,
                                block.panning,
                                block.pitch,
                            )
                        }.sortedBy { it.atTick },
                        /*.apply {
                                                this.forEach { block ->
                                                    println("At ${block.atTick} instrument ${block.instrument.id} ${block.instrument.getSound()}")
                                                }
                                            }*/
                    )
                }
            }
        }
    }

    private data class UnlinkedNoteBlock(
        val atTick: Int,
        val instrument: Byte,
        val key: Byte,
        val velocity: Byte,
        val panning: Byte,
        val pitch: Short,
    )

    data class NoteBlock(
        val atTick: Int,
        val instrument: BaseInstrument,
        val key: Byte,
        val velocity: Byte,
        val panning: Byte,
        val pitch: Short,
    )

    sealed class BaseInstrument {
        abstract val id: Int
        abstract fun getSound(): String
    }

    class VanillaInstrument(
        override val id: Int,
        val instrument: VanillaInstrumentType,
    ) : BaseInstrument() {
        override fun getSound(): String = instrument.soundName
    }

    data class CustomInstrument(
        override val id: Int,
        val name: String?,
        val file: String,
        val pitch: Byte,
    ) : BaseInstrument() {
        override fun getSound(): String = File(file).nameWithoutExtension
    }

    enum class VanillaInstrumentType(
        val soundName: String,
    ) {
        Piano("minecraft:block.note_block.harp"),
        DoubleBass("minecraft:block.note_block.bass"),
        BassDrum("minecraft:block.note_block.basedrum"),
        SnareDrum("minecraft:block.note_block.snare"),
        Sticks("minecraft:block.note_block.hat"),
        BassGuitar("minecraft:block.note_block.guitar"),
        Flute("minecraft:block.note_block.flute"),
        Bell("minecraft:block.note_block.bell"),
        Chime("minecraft:block.note_block.chime"),
        Xylophone("minecraft:block.note_block.xylophone"),
        IronXylophone("minecraft:block.note_block.iron_xylophone"),
        CowBell("minecraft:block.note_block.cow_bell"),
        Didgeridoo("minecraft:block.note_block.didgeridoo"),
        Bit("minecraft:block.note_block.bit"),
        Banjo("minecraft:block.note_block.banjo"),
        Pling("minecraft:block.note_block.pling"),
    }
}

private fun EndianDataInputStream.readSizedString(): String? {
    val length = readInt()
    if (length == 0) return null
//    println("Reading string of length $length")
    return readByteString(length).string(Charsets.UTF_8)
}