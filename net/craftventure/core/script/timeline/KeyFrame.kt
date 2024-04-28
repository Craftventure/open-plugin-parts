package net.craftventure.core.script.timeline

class KeyFrame(
    at: Long,
    var keyValue: Number,
    var inEasing: KeyFrameEasing = KeyFrameEasing.NONE,
    var outEasing: KeyFrameEasing = KeyFrameEasing.NONE
) {
    var at = at
        set(value) {
            field = if (value < 0) 0
            else value
        }
}