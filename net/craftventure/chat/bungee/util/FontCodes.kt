package net.craftventure.chat.bungee.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

object FontCodes {
    object Inventory {
        //        val rideOpOverlay = UiElement('\uE048', 256)
//        val generic54Empty = UiElement('\uE049', 256)
//        val generic54TopOnly = UiElement('\uE04A', 256)
//        val emptyFirstRowOverlay = UiElement('\uE04B', 256)
        val emptyRowUnderlay = object : Slot.RowAlts {
            override val row1 = UiElement("\uE072", 256)
            override val row2 = UiElement("\uE073", 256)
            override val row3 = UiElement("\uE074", 256)
            override val row4 = UiElement("\uE075", 256)
            override val row5 = UiElement("\uE076", 256)
            override val row6 = UiElement("\uE077", 256)
        }
    }

    object Slot {
        val underlay20 = object : RowAlts {
            override val row1 = UiElement("\uE06C", 16)
            override val row2 = UiElement("\uE06D", 16)
            override val row3 = UiElement("\uE06E", 16)
            override val row4 = UiElement("\uE06F", 16)
            override val row5 = UiElement("\uE070", 16)
            override val row6 = UiElement("\uE071", 16)
        }
        val border = object : RowAlts {
            override val row1 = UiElement("\uE079", 32)
            override val row2 = UiElement("\uE07A", 32)
            override val row3 = UiElement("\uE07B", 32)
            override val row4 = UiElement("\uE07C", 32)
            override val row5 = UiElement("\uE07D", 32)
            override val row6 = UiElement("\uE07E", 32)
        }

        val underlayProgress0 = RowAlts.of("\uE07F".first().code, 16)
        val underlayProgress1 = RowAlts.of("\uE07F".first().code + (6 * 1), 16)
        val underlayProgress2 = RowAlts.of("\uE07F".first().code + (6 * 2), 16)
        val underlayProgress3 = RowAlts.of("\uE07F".first().code + (6 * 3), 16)
        val underlayProgress4 = RowAlts.of("\uE07F".first().code + (6 * 4), 16)
        val underlayProgress5 = RowAlts.of("\uE07F".first().code + (6 * 5), 16)
        val underlayProgress6 = RowAlts.of("\uE07F".first().code + (6 * 6), 16)
        val underlayProgress7 = RowAlts.of("\uE07F".first().code + (6 * 7), 16)
        val underlayProgress8 = RowAlts.of("\uE07F".first().code + (6 * 8), 16)
        val underlayProgress9 = RowAlts.of("\uE07F".first().code + (6 * 9), 16)
        val underlayProgress10 = RowAlts.of("\uE07F".first().code + (6 * 10), 16)
        val underlayProgress11 = RowAlts.of("\uE07F".first().code + (6 * 11), 16)
        val underlayProgress12 = RowAlts.of("\uE07F".first().code + (6 * 12), 16)
        val underlayProgress13 = RowAlts.of("\uE07F".first().code + (6 * 13), 16)
        val underlayProgress14 = RowAlts.of("\uE07F".first().code + (6 * 14), 16)
        val underlayProgress15 = RowAlts.of("\uE07F".first().code + (6 * 15), 16)
        val underlayProgress16 = RowAlts.of("\uE07F".first().code + (6 * 16), 16)

        fun underlayProgress(progress: Double): RowAlts? {
            val step = 1 / 16.0
            return when {
                progress.isNaN() -> null
                progress >= 1.0 -> underlayProgress16
                progress >= 1 - (step * 1) -> underlayProgress15
                progress >= 1 - (step * 2) -> underlayProgress14
                progress >= 1 - (step * 3) -> underlayProgress13
                progress >= 1 - (step * 4) -> underlayProgress12
                progress >= 1 - (step * 5) -> underlayProgress11
                progress >= 1 - (step * 6) -> underlayProgress10
                progress >= 1 - (step * 7) -> underlayProgress9
                progress >= 1 - (step * 8) -> underlayProgress8
                progress >= 1 - (step * 9) -> underlayProgress7
                progress >= 1 - (step * 10) -> underlayProgress6
                progress >= 1 - (step * 11) -> underlayProgress5
                progress >= 1 - (step * 12) -> underlayProgress4
                progress >= 1 - (step * 13) -> underlayProgress3
                progress >= 1 - (step * 14) -> underlayProgress2
                progress >= 1 - (step * 15) -> underlayProgress1
                else -> underlayProgress0
            }
        }

        interface RowAlts {
            val row1: UiElement
            val row2: UiElement
            val row3: UiElement
            val row4: UiElement
            val row5: UiElement
            val row6: UiElement

            fun row(row: Int) = when (row) {
                0 -> row1
                1 -> row2
                2 -> row3
                3 -> row4
                4 -> row5
                5 -> row6
                else -> throw IndexOutOfBoundsException("Only supports up to 6 rows")
            }

            companion object {
                fun of(firstCharacter: Int, width: Int) = object : RowAlts {
                    override val row1 = UiElement(Character.toChars(firstCharacter + 0).concatToString(), width)
                    override val row2 = UiElement(Character.toChars(firstCharacter + 1).concatToString(), width)
                    override val row3 = UiElement(Character.toChars(firstCharacter + 2).concatToString(), width)
                    override val row4 = UiElement(Character.toChars(firstCharacter + 3).concatToString(), width)
                    override val row5 = UiElement(Character.toChars(firstCharacter + 4).concatToString(), width)
                    override val row6 = UiElement(Character.toChars(firstCharacter + 5).concatToString(), width)
                }
            }
        }

        val buttonBackground = UiElement("\uE078", 16)
    }

    object Button {
        val close = UiElement("\uE04D", 16)
    }
}

open class UiElement(val code: String, val width: Int) {
    val component = Component.text(code).color(NamedTextColor.WHITE)

    operator fun plus(other: UiElement) = UiElement(this.code + other.code, this.width + other.width + 1)
}