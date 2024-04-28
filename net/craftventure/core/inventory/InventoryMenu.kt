package net.craftventure.core.inventory

import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.bukkit.ktx.extension.updateMeta
import net.craftventure.bukkit.ktx.util.CraftventureKeys
import net.craftventure.bukkit.ktx.util.SlotBackgroundManager
import net.craftventure.chat.bungee.util.AnnotatedChatUtils
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.FontUtils
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeSync
import net.craftventure.core.extension.getItemId
import net.craftventure.core.extension.openMenu
import net.craftventure.core.extension.setItemId
import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.listener.InventoryListener
import net.craftventure.core.metadata.InventoryTrackingMeta
import net.craftventure.core.metadata.InventoryTrackingMeta.Companion.closeInventoryStack
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.*
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.reflect.KProperty


abstract class InventoryMenu internal constructor(
    protected val owner: Player? = null,
) : Listener, InventoryListener.ManagedInventoryHolder {
    protected var closeButtonIndex by invalidatingUnequality(StaticSlotIndex(4))

    override val menu: InventoryMenu = this

    var canPickup: Boolean = false
    var canCraft: Boolean = false
    var canMoveIn: Boolean = false
    var canMoveOut: Boolean = false
    var canInteract: Boolean = false
    var canDrag: Boolean = false
    var canClick: Boolean = false

    protected val slotBackgroundManager = SlotBackgroundManager()

    private var inventoryUpdateTask: Int? = null
    private var layoutRequested = false
    private var inventoryRecreationRequested = false

    private val invalidationLock = ReentrantLock()

    protected fun triggerSlotChanges() {
        updateActualTitleComponent()
        scheduleInventoryRecreation()
    }

    private fun scheduleInventoryUpdateTask() {
        invalidationLock.withLock {
//        logcat { "scheduleInventoryUpdateTask" }
            if (inventoryUpdateTask == null) {
                inventoryUpdateTask = executeSync {
                    doActualUpdate()
                }
            }
        }
    }

    private fun doActualUpdate() {
        invalidationLock.withLock {
//                logcat { "Updating..." }
            try {
                if (inventoryRecreationRequested) {
                    if (shouldRebuildInventory()) {
                        layoutRequested = true
//                            logcat { "createOrUpdateInventory()" }
                        createOrUpdateInventory()
                        inventoryRecreationRequested = false
                    }
                }
                if (layoutRequested) {
//                        logcat { "doOnLayout()" }
                    doOnLayout()
                }
            } finally {
                layoutRequested = false
                inventoryRecreationRequested = false
                inventoryUpdateTask = null
            }
        }
    }

    protected fun scheduleInventoryRecreation() {
//        logcat { "scheduleInventoryRecreation" }
        invalidationLock.withLock {
            inventoryRecreationRequested = true
            scheduleInventoryUpdateTask()
        }
    }

    protected fun scheduleLayout() {
//        logcat { "scheduleLayout" }
        invalidationLock.withLock {
            layoutRequested = true
            scheduleInventoryUpdateTask()
        }
    }

    protected fun doLayoutNow() {
//        logcat { "scheduleInventoryRecreation" }
        invalidationLock.withLock {
            layoutRequested = true
            doActualUpdate()
        }
    }

    protected fun invalidate() {
        invalidationLock.withLock {
//        logcat { "Invalidate" }
            scheduleInventoryRecreation()
            scheduleLayout()
        }
    }

    private val tasks = hashSetOf<Int>()

    protected fun registerResumedTask(id: Int) {
        tasks.add(id)
    }

    fun openAsMenu(player: Player) {
        player.openMenu(this)
    }

    private var task: Int? = null
    var rowsCount: Int = 6
        set(value) {
            if (field != value) {
                field = value
                scheduleInventoryRecreation()
            }
        }

    protected var lifecycleState: LifecycleState = LifecycleState.PAUSED
        private set(value) {
            if (value != field) {
                logcat { "State changed to $value" }
                field = value

                when (value) {
                    LifecycleState.PAUSED -> onPaused()
                    LifecycleState.RESUMED -> onResumed()
                }
            }
        }

    private var actualTitleComponent: Component = Component.empty()

    private fun updateActualTitleComponent() {
        actualTitleComponent = Component.text()
            .append(underlay)
            .append(slotBackgroundManager.generateComponent())
            .append(titleComponent)
            .build()
    }

    var titleComponent: Component = Component.empty()
        set(value) {
            if (field != value) {
                field = value

                updateActualTitleComponent()
                scheduleInventoryRecreation()
            }
        }

    var underlay: Component = Component.empty()
        set(value) {
            if (field != value) {
                field = value

                updateActualTitleComponent()
                scheduleInventoryRecreation()
            }
        }

//    init {
//        if (owner != null) {
//            slotBackgroundManager.setSlot(
//                SlotBackgroundManager.slotIndex(0),
//                FontCodes.Slot.buttonBackground
//            )
//        }
//    }

    private var lastTitle: Component? = null
    private var isRecreating = false

    //    protected fun calculateIndex(row: Int, column: Int) = ((row - 1) * 9) + (column - 1)
    protected fun calculateZeroBasedIndex(row: Int, column: Int) = (row * 9) + column

    private var inventory: Inventory = generateNewInventory()
    override fun getInventory(): Inventory = inventory

    protected fun requireOwner() = owner!!

    protected open fun onItemClicked(inventory: Inventory, position: Int, player: Player, action: InventoryAction) {
        handleNavigationButtonsClick(inventory, position, player, action)
    }

    protected abstract fun onLayout(inventory: Inventory)

    protected open fun onClickOutside(inventory: Inventory, player: Player) {}

    override fun onOpened(player: Player) {
        if (isRecreating) return
//        logcat { "onOpened ${player.name} isRecreating=$isRecreating" }
        lifecycleState = LifecycleState.RESUMED
    }

    override fun onClosed(player: Player) {
        if (isRecreating) return
//        logcat { "onClosed ${player.name} isRecreating=$isRecreating" }
        if (!hasViewers(player)) {
            lifecycleState = LifecycleState.PAUSED
        }
    }

    protected open fun onResumed() {
        CraftventureCore.getInstance().server.pluginManager.registerEvents(this, CraftventureCore.getInstance())
        doOnLayout()
    }

    protected open fun onPaused() {
        tasks.forEach { Bukkit.getScheduler().cancelTask(it) }
        tasks.clear()
        HandlerList.unregisterAll(this)
    }

    private fun shouldRebuildInventory(): Boolean {
        return inventory.size != (rowsCount * 9) || lastTitle != actualTitleComponent
    }

    private fun createOrUpdateInventory(): Inventory {
        if (DEBUG) Logger.debug("createOrUpdateInventory ${this::class.java.simpleName}")
//        if (lifecycleState!=LifecycleState.RESUMED) return null
        if (shouldRebuildInventory()) {
            val inventory = generateNewInventory()

            isRecreating = true
            val humanEntities = this.inventory.viewers.toList()
            this.inventory = inventory
            onLayout(inventory)
            for (humanEntity in humanEntities) {
                humanEntity.openInventory(inventory)
            }
            isRecreating = false
        }
        return this.inventory
    }

    private fun generateNewInventory(): Inventory {
        if (DEBUG) Logger.debug("createInventory ${this::class.java.simpleName}")
//        logcat { "Creating with $rowsCount" }
        return Bukkit.createInventory(this, rowsCount * 9, actualTitleComponent)
            .also {
                lastTitle = actualTitleComponent
            }
    }

    private fun doOnLayout() {
        if (DEBUG) Logger.debug("doOnLayout ${this::class.java.simpleName}")
        onLayout(createOrUpdateInventory())
    }

    open fun open(player: Player) {
        if (DEBUG) Logger.debug("open to ${player.name} ${this::class.java.simpleName}")
        if (!Bukkit.isPrimaryThread()) {
            executeSync { open(player) }
            return
        }
        createOrUpdateInventory().let {
            player.openInventory(it)
        }
    }

    @JvmOverloads
    fun hasViewers(exclude: HumanEntity? = null): Boolean {
        val count = inventory.viewers.count { it !== exclude }
        if (count == 0)
            return false
        return inventory.viewers.isNotEmpty()
    }

    private fun isThisInventory(comparable: Inventory?): Boolean {
        //        Logger.console("This inventory? %s", inventory != null && (inventory == comparable || inventory.equals(comparable)));
        return inventory == comparable
    }

    @EventHandler(ignoreCancelled = true)
    fun onInventoryClickListener(event: InventoryClickEvent) {
        if (isThisInventory(event.clickedInventory) || isThisInventory(event.inventory)) {
            if (DEBUG)
                Logger.debug("Event: onInventoryClickListener rawSlot=${event.rawSlot} click=${event.click} type=${event.slotType.name} slot=${event.slot} action=${event.action.name} canClick=$canClick")
            if (!canClick) {
                event.isCancelled = true
            }
            if (event.rawSlot < inventory.size) {
                if (event.rawSlot == -999) {
                    onClickOutside(inventory, event.whoClicked as Player)
                } else {
                    onItemClicked(inventory, event.rawSlot, event.whoClicked as Player, event.action)
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onInventoryCreativeListener(event: InventoryCreativeEvent) {
        if (isThisInventory(event.clickedInventory)) {
            if (DEBUG)
                Logger.debug("Event: onInventoryCreativeListener")
            //            Inventory inventory = inventoryCache.get(index);
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onInventoryDragListener(event: InventoryDragEvent) {
        if (isThisInventory(event.inventory)) {
            if (DEBUG)
                Logger.debug("Event: onInventoryDragListener")
            if (!canDrag)
                event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onInventoryInteractListener(event: InventoryInteractEvent) {
        if (isThisInventory(event.inventory)) {
            if (DEBUG)
                Logger.debug("Event: onInventoryInteractListener")
            if (!canInteract)
                event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onInventoryMoveItemListener(event: InventoryMoveItemEvent) {
        if (isThisInventory(event.source) || isThisInventory(event.destination) || isThisInventory(event.initiator)) {
            if (DEBUG)
                Logger.debug("Event: onInventoryMoveItemListener")
            if (!canMoveIn)
                event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onInventoryPickupItemListener(event: InventoryPickupItemEvent) {
        if (isThisInventory(event.inventory)) {
            if (DEBUG)
                Logger.debug("Event: onInventoryPickupItemListener")
            if (!canPickup)
                event.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onCraftItemListener(event: CraftItemEvent) {
        if (isThisInventory(event.inventory)) {
            if (DEBUG)
                Logger.debug("Event: onCraftItemListener")
            if (!canCraft)
                event.isCancelled = true
        }
    }

    protected open fun handleNavigationButtonsClick(
        inventory: Inventory,
        position: Int,
        player: Player,
        action: InventoryAction
    ): Boolean {
        if (position < 0 || position > inventory.size - 1) return false
        val itemStack = inventory.getItem(position)
        if (itemStack != null) {
            val id = itemStack.getItemId()
            when (id) {
                CraftventureKeys.ID_ITEM_MENU_CLOSE -> {
                    executeSync { player.closeInventoryStack() }
                    return true
                }

                CraftventureKeys.ID_ITEM_MENU_UP -> {
                    player.getMetadata<InventoryTrackingMeta>()?.pop()
                    return true
                }
            }
        }
        return false
    }

    protected fun generateUpButton(): ItemStack? {
        val owner = owner ?: return null
        val meta = owner.getMetadata<InventoryTrackingMeta>() ?: return null
        if (!meta.hasBackStack()) return null
        val previousTitle = meta.previousItem()?.titleComponent ?: return null

        return ItemStack(Material.STICK)
            .updateMeta<ItemMeta> {
                setCustomModelData(14)
            }
            .displayName(
                InventoryConstants.NAME_ITEM_UP.append(
                    Component.text(
                        PlainTextComponentSerializer.plainText().serialize(previousTitle)
                            .replace(AnnotatedChatUtils.unicodeCustomBlocks, "")
                            .trim()
                    )
                )
            )
            .setItemId(CraftventureKeys.ID_ITEM_MENU_UP)
    }

    protected open fun addNavigationButtons(inventory: Inventory) {
        inventory.setItem(closeButtonIndex.index, generateCloseButton())
        inventory.setItem(0, generateUpButton())
    }

    companion object {
        const val TITLE_WIDTH = 160

        fun centeredTitle(title: String) =
            FontUtils.centerForWidth(Component.text(title).color(CVTextColor.INVENTORY_TITLE), TITLE_WIDTH)

        fun generateCloseButton() = ItemStack(Material.BARRIER)
            .updateMeta<ItemMeta> {
                setCustomModelData(1)
            }
            .displayName(InventoryConstants.NAME_ITEM_CLOSE)
            .setItemId(CraftventureKeys.ID_ITEM_MENU_CLOSE)

        private val DEBUG: Boolean
            get() = false
//            get() {
//                return false//CraftventureCore.getEnvironment() == CraftventureCore.Environment.DEVELOPMENT
//            }
    }

    enum class LifecycleState {
        PAUSED,
        RESUMED,
    }

    interface SlotIndex {
        val index: Int
    }

    class StaticSlotIndex(override val index: Int) : SlotIndex

    class LastRowSlotIndex(val column: Int, val menu: InventoryMenu) : SlotIndex {
        override val index: Int = ((menu.rowsCount - 1) * 9) + column
    }

    class InvalidationDelegate<T>(
        default: T,
        private val onApply: (() -> Unit)? = null,
        private val equalityCheck: (T, T) -> Boolean
    ) {
        private var value = default
        operator fun getValue(thisRef: InventoryMenu, property: KProperty<*>): T {
            return value
        }

        operator fun setValue(thisRef: InventoryMenu, property: KProperty<*>, value: T) {
            if (!equalityCheck(value, this.value)) {
                this.value = value
                thisRef.scheduleLayout()
                onApply?.invoke()
            }
        }
    }

    fun <T> invalidatingAlways(default: T, onApply: (() -> Unit)? = null) =
        InvalidationDelegate(default, onApply) { _, _ -> false }

    fun <T> invalidatingReferenceInequality(default: T, onApply: (() -> Unit)? = null) =
        InvalidationDelegate(default, onApply) { a, b -> a === b }

    fun <T> invalidatingUnequality(default: T, onApply: (() -> Unit)? = null) =
        InvalidationDelegate(default, onApply) { a, b -> a === b && a == b }
}
