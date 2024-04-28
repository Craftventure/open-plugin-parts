package net.craftventure.bukkit.ktx.util

import net.craftventure.chat.bungee.util.FontCodes
import net.craftventure.chat.bungee.util.SpacedElementHelper
import net.craftventure.chat.bungee.util.UiElement
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SlotBackgroundManager(
    private val spacedElementHelper: SpacedElementHelper = SpacedElementHelper(),
) {
    data class RowedSlotIndex(val row: Int, val column: Int)

    data class SlotInfo(
        val color: TextColor,
        val uiElement: UiElement,
        val tag: String? = null,
        val offset: Int = 0,
    )

    private val slots = hashMapOf<RowedSlotIndex, SlotInfo>()

    private val lock = ReentrantLock()

    fun generateComponent(): TextComponent.Builder {
        lock.withLock {
            if (slots.isEmpty()) return Component.text()

            spacedElementHelper.reset()

//        title.append(Component.text(SpaceHelper.width(-2)))

            slots.forEach { (index, info) ->
                val row = index.row
                val column = index.column

//                logcat { "${index.row}/${index.column}" }

                val slotElement = info.uiElement
                val x = column * 18
                spacedElementHelper.add(
                    x + info.offset,
                    slotElement,
                    Component.text(slotElement.code).color(info.color)
                )
            }

            spacedElementHelper.moveToX(0)
            return spacedElementHelper.builder
        }
    }

    fun clearTag(tag: String) {
        val remove = slots.filter { it.value.tag == tag }
        remove.forEach { index, info -> slots.remove(index) }
    }

    fun setSlot(
        index: RowedSlotIndex,
        rowAlts: FontCodes.Slot.RowAlts,
        color: TextColor = NamedTextColor.WHITE,
        tag: String? = null,
        offset: Int = (16 - rowAlts.row1.width) / 2,
    ) {
        slots[index] = SlotInfo(color, rowAlts.row(index.row), tag, offset = offset)
    }

    fun setSlot(
        index: RowedSlotIndex,
        element: UiElement,
        color: TextColor = NamedTextColor.WHITE,
        tag: String? = null,
        offset: Int = (16 - element.width) / 2,
    ) {
        slots[index] = SlotInfo(color, element, tag, offset = offset)
    }

    operator fun set(index: Int, info: SlotInfo) {
        slots[slotIndex(index)] = info
    }

    operator fun set(index: RowedSlotIndex, info: SlotInfo) {
        slots[index] = info
    }

    fun clear(index: RowedSlotIndex) {
        slots.remove(index)
    }

    companion object {
        fun slotIndex(index: Int) = RowedSlotIndex(index / 9, index % 9)
    }
}