package net.craftventure.chat.core.util

import kotlin.math.abs

object SpaceHelper {
    fun width(pixels: Int, noSplit: Boolean = true): String {
        if (pixels == 0) return ""
        val sizedSpace = if (pixels > 0)
            if (noSplit)
                Regular
            else RegularNoSplit
        else
            if (noSplit)
                NegativeNoSplit
            else Negative

        var pixelsLeft = abs(pixels)

        var output = ""
        while (pixelsLeft > 0) {
            if (pixelsLeft >= 1024) {
                output += sizedSpace.s1024
                pixelsLeft -= 1024
            } else if (pixelsLeft >= 512) {
                output += sizedSpace.s512
                pixelsLeft -= 512
            } else if (pixelsLeft >= 256) {
                output += sizedSpace.s256
                pixelsLeft -= 256
            } else if (pixelsLeft >= 128) {
                output += sizedSpace.s128
                pixelsLeft -= 128
            } else if (pixelsLeft >= 64) {
                output += sizedSpace.s64
                pixelsLeft -= 64
            } else if (pixelsLeft >= 32) {
                output += sizedSpace.s32
                pixelsLeft -= 32
            } else if (pixelsLeft >= 16) {
                output += sizedSpace.s16
                pixelsLeft -= 16
            } else if (pixelsLeft >= 8) {
                output += sizedSpace.s8
                pixelsLeft -= 8
            } else if (pixelsLeft >= 7) {
                output += sizedSpace.s7
                pixelsLeft -= 7
            } else if (pixelsLeft >= 6) {
                output += sizedSpace.s6
                pixelsLeft -= 6
            } else if (pixelsLeft >= 5) {
                output += sizedSpace.s5
                pixelsLeft -= 5
            } else if (pixelsLeft >= 4) {
                output += sizedSpace.s4
                pixelsLeft -= 4
            } else if (pixelsLeft >= 3) {
                output += sizedSpace.s3
                pixelsLeft -= 3
            } else if (pixelsLeft >= 2) {
                output += sizedSpace.s2
                pixelsLeft -= 2
            } else {
                output += sizedSpace.s1
                pixelsLeft -= 1
            }
        }

        return output
    }

    abstract class SizedSpace {
        abstract val s1: Char
        abstract val s2: Char
        abstract val s3: Char
        abstract val s4: Char
        abstract val s5: Char
        abstract val s6: Char
        abstract val s7: Char
        abstract val s8: Char
        abstract val s16: Char
        abstract val s32: Char
        abstract val s64: Char
        abstract val s128: Char
        abstract val s256: Char
        abstract val s512: Char
        abstract val s1024: Char
        abstract val sMax: Char
    }

    object Negative : SizedSpace() {
        override val s1 = '\uF801'
        override val s2 = '\uF802'
        override val s3 = '\uF803'
        override val s4 = '\uF804'
        override val s5 = '\uF805'
        override val s6 = '\uF806'
        override val s7 = '\uF807'
        override val s8 = '\uF808'
        override val s16 = '\uF809'
        override val s32 = '\uF80A'
        override val s64 = '\uF80B'
        override val s128 = '\uF80C'
        override val s256 = '\uF80D'
        override val s512 = '\uF80E'
        override val s1024 = '\uF80F'
        override val sMax = '\uF800'
    }

    object NegativeNoSplit : SizedSpace() {
        override val s1 = '\uF811'
        override val s2 = '\uF812'
        override val s3 = '\uF813'
        override val s4 = '\uF814'
        override val s5 = '\uF815'
        override val s6 = '\uF816'
        override val s7 = '\uF817'
        override val s8 = '\uF818'
        override val s16 = '\uF819'
        override val s32 = '\uF81A'
        override val s64 = '\uF81B'
        override val s128 = '\uF81C'
        override val s256 = '\uF81D'
        override val s512 = '\uF81E'
        override val s1024 = '\uF81F'
        override val sMax = '\uF810'
    }

    object Regular : SizedSpace() {
        override val s1 = '\uF821'
        override val s2 = '\uF822'
        override val s3 = '\uF823'
        override val s4 = '\uF824'
        override val s5 = '\uF825'
        override val s6 = '\uF826'
        override val s7 = '\uF827'
        override val s8 = '\uF828'
        override val s16 = '\uF829'
        override val s32 = '\uF82A'
        override val s64 = '\uF82B'
        override val s128 = '\uF82C'
        override val s256 = '\uF82D'
        override val s512 = '\uF82E'
        override val s1024 = '\uF82F'
        override val sMax = '\uF820'
    }

    object RegularNoSplit : SizedSpace() {
        override val s1 = '\uF831'
        override val s2 = '\uF832'
        override val s3 = '\uF833'
        override val s4 = '\uF834'
        override val s5 = '\uF835'
        override val s6 = '\uF836'
        override val s7 = '\uF837'
        override val s8 = '\uF838'
        override val s16 = '\uF839'
        override val s32 = '\uF83A'
        override val s64 = '\uF83B'
        override val s128 = '\uF83C'
        override val s256 = '\uF83D'
        override val s512 = '\uF83E'
        override val s1024 = '\uF83F'
        override val sMax = '\uF830'
    }
}