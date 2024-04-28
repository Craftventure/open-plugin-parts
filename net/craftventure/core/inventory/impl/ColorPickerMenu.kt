package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.MaterialConfig.dataItem
import net.craftventure.bukkit.ktx.extension.applyAllHideItemFlags
import net.craftventure.bukkit.ktx.extension.distanceTo
import net.craftventure.bukkit.ktx.extension.setColor
import net.craftventure.bukkit.ktx.extension.updateMeta
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.displayNameWithBuilder
import net.craftventure.bukkit.ktx.util.SlotBackgroundManager
import net.craftventure.chat.bungee.util.CvComponent
import net.craftventure.chat.bungee.util.FontCodes
import net.craftventure.chat.bungee.util.UiElement
import net.craftventure.core.inventory.InventoryMenu
import net.craftventure.core.metadata.InventoryTrackingMeta.Companion.popMenu
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Color
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import kotlin.math.absoluteValue

class ColorPickerMenu(
    player: Player,
    val colorPickerPresenter: ItemStack = ItemStack(Material.POTION)
        .updateMeta<PotionMeta> { setCustomModelData(1) },
    val itemstackRepresenter: ((Color) -> ItemStack?)? = null,
    val listener: ResultListener,
    startColor: Color = DyeColor.WHITE.color,
) : InventoryMenu(
    owner = player,
), Listener {
    private val colors = DyeColor.values().take(18)
    private var color: Color by invalidatingUnequality(colors.minBy { it.color.distanceTo(startColor) }.color)
    private var shadedColor: Color by invalidatingUnequality(color)

    private val shades = listOf(
        Shade(-0.8),
        Shade(-0.6),
        Shade(-0.4),
        Shade(-0.2),
        Shade(0.0),
        Shade(0.1),
        Shade(0.3),
        Shade(0.6),
        Shade(0.8),
    )

    init {
        titleComponent = centeredTitle("Pick a color")

        underlay = CvComponent.resettingInventoryOverlay(
            UiElement("\uE0E6", 256)
        )

        rebuildIndications()
    }

    private fun rebuildIndications() {
        slotBackgroundManager.clearTag("selected")

        val colorIndex = colors.indexOfFirst { it.color == color }.takeIf { it >= 0 }
        if (colorIndex != null) {
            slotBackgroundManager.setSlot(
                SlotBackgroundManager.slotIndex(18 + colorIndex),
                FontCodes.Slot.border,
                NamedTextColor.DARK_GREEN,
                tag = "selected"
            )
        }

        val shadeIndex = shades.indexOfFirst { it.withColor(color) == shadedColor }.takeIf { it >= 0 }
        if (shadeIndex != null) {
            slotBackgroundManager.setSlot(
                SlotBackgroundManager.slotIndex(45 + shadeIndex),
                FontCodes.Slot.border,
                NamedTextColor.DARK_GREEN,
                tag = "selected"
            )
        }

        triggerSlotChanges()
    }

//    override fun onDestroy() {
//        super.onDestroy()
//        HandlerList.unregisterAll(this)
//    }

    override fun onItemClicked(inventory: Inventory, position: Int, player: Player, action: InventoryAction) {
        if (handleNavigationButtonsClick(inventory, position, player, action)) return

        if (position == 14) {
            player.popMenu()
            listener.onPicked(shadedColor)
        }

        colors.forEachIndexed { index, dyeColor ->
            if (position == 18 + index) {
                color = dyeColor.color
                shadedColor = color
                rebuildIndications()
            }
        }

        shades.forEachIndexed { index, shade ->
            if (position == 45 + index) {
                shadedColor = shade.withColor(color)
                rebuildIndications()
            }
        }
    }

    override fun onLayout(inventory: Inventory) {
        addNavigationButtons(inventory)

        inventory.setItem(
            13,
            itemstackRepresenter?.invoke(shadedColor)?.applyAllHideItemFlags()?.displayNameWithBuilder {
                text("Preview")
            })
        inventory.setItem(14, dataItem(Material.STICK, 15).displayNameWithBuilder {
            text("Apply color")
        })

        colors.forEachIndexed { index, dyeColor ->
            inventory.setItem(18 + index, colorPickerPresenter.clone().applyAllHideItemFlags().apply {
                setColor(dyeColor.color)
                displayNameWithBuilder { text("Set base color to ${dyeColor.name.lowercase()}") }
            })
        }

        shades.forEachIndexed { index, shade ->
            val color = shade.withColor(color)
            inventory.setItem(45 + index, colorPickerPresenter.clone().applyAllHideItemFlags().apply {
                setColor(color)
                displayNameWithBuilder { text("Set color to this shade") }
            })
        }
    }

    data class Shade(
        val percentage: Double,
    ) {
        fun withColor(color: Color): Color {
            var red = color.red
            var green = color.green
            var blue = color.blue

            if (percentage >= 0) {
                red = (red + ((255 - red) * percentage)).toInt()
                green = (green + ((255 - green) * percentage)).toInt()
                blue = (blue + ((255 - blue) * percentage)).toInt()
            } else {
                val t = 1 - percentage.absoluteValue
                red = (red * t).toInt()
                green = (green * t).toInt()
                blue = (blue * t).toInt()
            }

            return Color.fromRGB(red, green, blue)
        }
    }

    interface ResultListener {
        fun onPicked(color: Color)
        fun onCancelled() {}
    }
}