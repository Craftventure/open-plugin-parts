package net.craftventure.core.metadata

import net.craftventure.annotationkit.GenerateService
import net.craftventure.bukkit.ktx.entitymeta.BasePlayerMetadata
import net.craftventure.bukkit.ktx.entitymeta.PlayerMetaFactory
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.bukkit.ktx.util.SoundUtils.GUI_OPEN
import net.craftventure.core.inventory.InventoryMenu
import net.craftventure.core.listener.InventoryListener
import net.craftventure.core.manager.EquipmentManager
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
import java.util.*


class InventoryTrackingMeta(
    player: Player
) : BasePlayerMetadata(player) {
//    init {
//        logcat { "Creating for ${player.name} @${hashCode()}" }
//    }

    private var lastMenuOpenTime = 0L
    private val stack = LinkedList<InventoryMenu>()
    private var inTransaction = false
    private var stackChangedByTransaction = false
    var isMenuOpen = false
        private set(value) {
            if (field != value) {
                field = value
//                logcat { "isMenuOpen=$value" }
                val player = player()
                EquipmentManager.reapply(player)
            }
        }

    override fun debugComponent(): Component? {
        return Component.text(
            "inTransaction=${inTransaction} stackChangedByTransaction=$stackChangedByTransaction isMenuOpen=$isMenuOpen stack=${stack.size}, [${
                stack.map { it.javaClass.simpleName }.joinToString(", ")
            }]"
        )
    }

    fun onInventoryOpened() {
        if (!isMenuOpen)
            playMenuOpenSound()
        lastMenuOpenTime = System.currentTimeMillis()
        isMenuOpen = true
    }

    fun onInventoryClosed(reason: InventoryCloseEvent.Reason) {
//        logcat { "Close $reason" }//: ${Logger.miniTrace(10)}" }

        if (reason != InventoryCloseEvent.Reason.OPEN_NEW)
            isMenuOpen = false

        when (reason) {
            InventoryCloseEvent.Reason.OPEN_NEW -> {
//                logcat { "Opening new" }
            }

            InventoryCloseEvent.Reason.PLAYER, InventoryCloseEvent.Reason.TELEPORT -> {
//                logcat { "Clearing backstack ($reason)" }
                clearBackstack()
            }

            else -> {
                pop()
//                logcat { "Popping current" }
            }
        }
    }

    fun closeAllMenus() {
        stack.clear()
        val player = player()
        player.closeInventory()
    }

    private fun playMenuOpenSound() {
        if (lastMenuOpenTime < System.currentTimeMillis() - 1000) {
            lastMenuOpenTime = System.currentTimeMillis()
            val player = player()
            player.playSound(player.location, GUI_OPEN, 1f, 1f)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
//        logcat { "Destroying inv meta" }
        stack.clear()
    }

    fun transaction(block: InventoryTrackingMeta.() -> Unit) {
        if (inTransaction) {
            block(this)
            return
        }

        inTransaction = true
        stackChangedByTransaction = false
        try {
            block(this)
        } finally {
            inTransaction = false
        }

        if (stackChangedByTransaction) {
            handleStackChange()
            stackChangedByTransaction = false
        }

    }

    fun previousItem() = stack.getOrNull(stack.size - 2)

    fun hasBackStack() = stack.size >= 2

    fun stackSize() = stack.size

    fun clearBackstack() {
//        logcat { "Clearing backstack" }
        if (stack.isEmpty()) return
        stack.clear()
        handleStackChangeIfNotInTransaction()
    }

    fun replaceCurrent(inventory: InventoryMenu) {
//        logcat { "Replacing current" }
        transaction {
            pop()
            push(inventory)
        }
//        inventory.open(player)
    }

    fun push(vararg inventories: InventoryMenu) {
//        logcat { "Pushing ${inventories.size}" }
        transaction {
            inventories.forEach(::push)
        }
    }

    fun push(inventory: InventoryMenu): Boolean {
//        logcat { "Pushing @${hashCode()}" }
        stack.add(inventory)
        handleStackChangeIfNotInTransaction()
        return true
    }

    fun pop(): Boolean {
//        logcat { "Pop" }
        if (stack.isEmpty()) return false
        val removed = stack.removeLast()
        if (removed != null) {
            handleStackChangeIfNotInTransaction()
        }
        return removed != null
    }

    private fun handleStackChangeIfNotInTransaction() {
//        logcat { "Marked stack as changed" }
        if (inTransaction) {
            stackChangedByTransaction = true
            return
        }

        handleStackChange()
    }

    private fun handleStackChange() {
//        logcat { "Handling stack change size=${stack.size}" }
        val menu = stack.lastOrNull()
        val player = player()
        if (menu != null) {
            menu.open(player)
        } else {
            if (player.openInventory.topInventory.holder is InventoryListener.ManagedInventoryHolder)
                player.closeInventory(InventoryCloseEvent.Reason.PLAYER)
        }
    }

    @GenerateService
    class Generator : PlayerMetaFactory() {
        override fun create(player: Player) = player.getOrCreateMetadata { InventoryTrackingMeta(player) }
    }

    companion object {

        fun Player.popMenu() {
            val meta = get(this)
            if (meta != null)
                meta.pop()
            else closeInventoryStack()
        }

        fun Player.closeInventoryStack() {
            val meta = get(this)
            if (meta != null)
                meta.closeAllMenus()
            else closeInventory()
        }

        fun get(player: Player) = player.getMetadata<InventoryTrackingMeta>()
    }
}
