package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.bukkit.ktx.extension.hideAttributes
import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.bukkit.ktx.extension.toSkullItem
import net.craftventure.bukkit.ktx.util.CraftventureKeys
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.CvComponent
import net.craftventure.chat.bungee.util.FontCodes
import net.craftventure.core.CraftventureCore
import net.craftventure.core.extension.getMeta
import net.craftventure.core.extension.setMeta
import net.craftventure.core.inventory.InventoryMenu
import net.craftventure.core.ride.operator.OperableRide
import net.craftventure.core.ride.operator.controls.OperatorControl
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.math.floor
import kotlin.math.min

class RideOperatorMenu(
    val operableRide: OperableRide
) : InventoryMenu() {
    private val cachedOperatorUuids = arrayOfNulls<UUID>(operableRide.totalOperatorSpots)
    private var forceUpdate = false
    private val controls = ArrayList(operableRide.provideControls())

    init {
        rowsCount = calculateRows()
        underlay = CvComponent.resettingInventoryOverlay(
            FontCodes.Inventory.emptyRowUnderlay.row1,
        )
        titleComponent = centeredTitle((operableRide.ride?.displayName ?: "?") + " RideOP")
        forceUpdate = true

        controls.sortWith(compareBy({ it.group }, { it.sort }))

        invalidate()
    }

    private fun calculateRows(): Int {
        var index = 9
        for (i in 0 until operableRide.totalOperatorSpots) {
            index++
        }

        index++

        var group: String? = controls.firstOrNull()?.group
        for (operatorControl in controls) {
            if (group != operatorControl.group) {
                index += 9 - (index % 9)
            }

            if (index >= 54) break
            group = operatorControl.group
            index++
        }
        return min((index / 9) + 1, 6)
    }

    override fun onItemClicked(inventory: Inventory, position: Int, player: Player, action: InventoryAction) {
        if (handleNavigationButtonsClick(inventory, position, player, action)) return

        val item = inventory.getItem(position)
        val controlId = item?.getMeta(CraftventureKeys.KEY_OPERATOR_CONTROL)
        if (controlId != null) {
//            Logger.debug("Clicked control $controlId")
            CraftventureCore.getOperatorManager().clicked(player, operableRide.ride!!.name!!, controlId)
        }
        val slotId = item?.getMeta(CraftventureKeys.KEY_OPERATOR_SLOT)?.toIntOrNull()
        if (slotId != null) {
//            Logger.debug(
//                "Clicked slot $slotId ${operableRide.getOperatorForSlot(slotId)} ${cachedOperatorUuids.getOrNull(
//                    slotId
//                )}"
//            )

            if (player === operableRide.getOperatorForSlot(slotId)) {
                CraftventureCore.getOperatorManager().cancelOperating(operableRide, player)
                return
            } else if (operableRide.getOperatorForSlot(slotId) === null || player.isCrew()) {
                CraftventureCore.getOperatorManager().startOperating(operableRide, player, slotId)
                return
            }
        }
    }

    override fun onLayout(inventory: Inventory) {
        addNavigationButtons(inventory)

        var index = 9
        for (i in 0 until operableRide.totalOperatorSpots) {
            val currentOperator = operableRide.getOperatorForSlot(i)
            if (forceUpdate || currentOperator == null && cachedOperatorUuids[i] != null || currentOperator != null && currentOperator.uniqueId != cachedOperatorUuids[i]) {
                cachedOperatorUuids[i] = currentOperator?.uniqueId
                val item = if (currentOperator != null) {
                    currentOperator.playerProfile.toSkullItem()
                        .hideAttributes()
                        .displayName(CVTextColor.MENU_DEFAULT_TITLE + "#${i + 1} Currently being operated by ${currentOperator.name}")
                } else {
                    getUnclaimedOperatorItem()
                }
                item.setMeta(CraftventureKeys.KEY_OPERATOR_SLOT, i.toString())
                inventory.setItem(index, item)
            }
            index++
        }

        index++

//        Logger.debug("===========")
        var group: String? = controls.firstOrNull()?.group
        for ((controlIndex, operatorControl) in controls.withIndex()) {
//            val groupSizeRemaining = calculateRemainingGroupSize(controls, controlIndex + 1)
            val nextGroupSize = calculateRemainingGroupSize(controls, controlIndex)
//            val oldIndex = index
            val row = floor(index.toDouble() / 9.0).toInt()
            val rowStart = row * 9
            val rowEnd = rowStart + 9

//            Logger.debug("$index in row=$row from $rowStart to $rowEnd next=$nextGroupSize mod=${(index % 9)}")
            if (group != operatorControl.group && (index % 9) != 0) {
                if (index + 1 + nextGroupSize in rowStart..rowEnd)
                    index += 1
                else
                    index = rowEnd
            }

            if (index >= 54) break
//            Logger.debug("$index = ${operatorControl.id}")
//            Logger.debug("$index(${index % 9}/$oldIndex) $groupSizeRemaining > $nextGroupSize $controlIndex ${operatorControl.id} ${operatorControl.group}")

            if (operatorControl.isInvalidated || forceUpdate) {
                val item = operatorControl.representAsItemStack().hideAttributes()
                item.setMeta(CraftventureKeys.KEY_OPERATOR_CONTROL, operatorControl.id)
                inventory.setItem(index, item)
            }
            group = operatorControl.group
            index++
        }

        forceUpdate = false
    }

    private fun calculateRemainingGroupSize(controls: List<OperatorControl>, startIndexIncluded: Int): Int {
        var size = 0
        val startGroup = controls.getOrNull(startIndexIncluded)?.group
        for (i in startIndexIncluded until controls.size) {
            val control = controls[i]
            if (control.group != startGroup) {
                return size
            }
            size++
        }
        return size
    }

    fun updateOperatorControls() {
        doLayoutNow()
    }

    private fun getUnclaimedOperatorItem(): ItemStack {
        return ItemStack(Material.STONE_BUTTON).displayName(CVTextColor.MENU_DEFAULT_TITLE + "Claim this operator spot")
    }
}