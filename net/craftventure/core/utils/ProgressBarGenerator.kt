package net.craftventure.core.utils

object ProgressBarGenerator {
    val progressSymbols = arrayOf("▏", "▎", "▍", "▌", "▋", "▊", "▉", "█")

    fun generateProgressBar(
        value: Double,
        length: Int = 10,
        progressSymbols: Array<String> = this.progressSymbols,
        emptyProgressPlaceHolder: String = " "
    ): String {
        var progress = ""
        val progressPerSegment = 1.0 / length.toDouble()
        val progressPerSymbol = progressPerSegment / progressSymbols.size.toDouble()
        for (i in 0 until length) {
            val startProgress = i * progressPerSegment

            var added = false
            for ((index, symbol) in progressSymbols.withIndex().reversed()) {
                if (value >= startProgress + (progressPerSymbol * index)) {
                    progress += symbol
                    added = true
                    break
                }
            }
            if (!added)
                progress += emptyProgressPlaceHolder
        }
        return progress
    }
}