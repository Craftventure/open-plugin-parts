package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.MaterialConfig.dataItem
import net.craftventure.bukkit.ktx.extension.displayName
import net.craftventure.bukkit.ktx.util.CraftventureKeys
import net.craftventure.bukkit.ktx.util.CraftventureKeys.shopId
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.CvComponent
import net.craftventure.chat.bungee.util.FontCodes
import net.craftventure.core.async.executeSync
import net.craftventure.core.inventory.LayoutInventoryMenu
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.metadata.InventoryTrackingMeta.Companion.closeInventoryStack
import org.bukkit.Material
import org.bukkit.block.*
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import org.bukkit.material.Openable
import org.bukkit.persistence.PersistentDataType
import java.lang.reflect.InvocationTargetException

class TileStateEditorMenu(
    player: Player,
    private val block: Block
) : LayoutInventoryMenu(
    itemStartColumnOffset = 0,
    itemStartRowOffset = 1,
    maxItemRows = 4,
    maxItemColumns = 9,
    owner = player,
) {
    var state: BlockState = block.state

    var helpers = listOf(
        TileStateHelper(
            { state -> state is Sign },
            { state ->
                ItemStack(Material.OAK_SIGN).apply {
                    displayName(CVTextColor.MENU_DEFAULT_TITLE + "Edit")
                }
            },
            { player ->
                val sign = block.state as Sign
                try {
                    player.sendSignChange(
                        block.location,
                        sign.lines()
                    )
                    player.openSign(sign)
                } catch (e: InvocationTargetException) {
                    Logger.capture(e)
                }
            }
        ),
        TileStateHelper(
            { state -> state is TileState },
            { state ->
                state as TileState
                val hasBook =
                    state.persistentDataContainer.get(
                        CraftventureKeys.TILE_STATE_BOOK,
                        PersistentDataType.BYTE_ARRAY
                    ) != null
                if (hasBook)
                    ItemStack(Material.BARRIER).apply {
                        displayName(CVTextColor.MENU_DEFAULT_TITLE + "Remove book")
                    }
                else {
                    val book = player.inventory.find { it?.type == Material.WRITTEN_BOOK }
                    if (book != null) {
                        val meta = book.itemMeta as BookMeta
                        ItemStack(Material.WRITABLE_BOOK).apply {
                            displayName(CVTextColor.MENU_DEFAULT_TITLE + "Set book from inventory")
                            lore(
                                listOf(
                                    CVTextColor.MENU_DEFAULT_LORE + listOfNotNull(
                                        meta.author,
                                        meta.title,
                                        "Pages: ${meta.pageCount}"
                                    ).joinToString("\n")
                                )
                            )
                        }
                    } else {
                        ItemStack(Material.WRITABLE_BOOK).apply {
                            displayName(CVTextColor.MENU_DEFAULT_TITLE + "Set book (none found in inventory?)")
                        }
                    }
                }
            },
            {
                val state = (state as TileState)
                val hasBook =
                    state.persistentDataContainer.get(
                        CraftventureKeys.TILE_STATE_BOOK,
                        PersistentDataType.BYTE_ARRAY
                    ) != null
                if (hasBook) {
                    state.persistentDataContainer.remove(CraftventureKeys.TILE_STATE_BOOK)
                    state.update()
                } else {
                    val book = player.inventory.find { it?.type == Material.WRITTEN_BOOK }
                    if (book != null) {
                        state.persistentDataContainer.set(
                            CraftventureKeys.TILE_STATE_BOOK,
                            PersistentDataType.BYTE_ARRAY,
                            book.serializeAsBytes()
                        )
                        state.update()
                    } else {
                        player.sendMessage(CVTextColor.serverError + "No books found in your inventory to set")
                    }
                }
                executeSync { player.closeInventoryStack() }
            }
        ),
        TileStateHelper(
            { state ->
                state is TileState && state.persistentDataContainer.get(
                    CraftventureKeys.TILE_STATE_BOOK,
                    PersistentDataType.BYTE_ARRAY
                ) != null
            },
            { state ->
                state as TileState
                ItemStack(Material.WRITTEN_BOOK).apply {
                    displayName(CVTextColor.MENU_DEFAULT_TITLE + "Take book")
                }
            },
            {
                val bookData = (state as TileState).persistentDataContainer.get(
                    CraftventureKeys.TILE_STATE_BOOK,
                    PersistentDataType.BYTE_ARRAY
                )

                try {
                    val itemStack = ItemStack.deserializeBytes(bookData!!)
                    player.inventory.addItem(itemStack)
                    executeSync { player.closeInventoryStack() }
                } catch (e: Exception) {
                    player.sendMessage(CVTextColor.serverError + "Failed to take book: ${e.message}")
                    executeSync { player.closeInventoryStack() }
                }
            }
        ),
        TileStateHelper(
            { state -> state is TileState },
            { state ->
                state as TileState
                val allowInteract =
                    state.persistentDataContainer.get(
                        CraftventureKeys.TILE_STATE_ALLOW_INTERACT,
                        PersistentDataType.STRING
                    ) != null
                if (allowInteract)
                    ItemStack(Material.STONE_BUTTON).apply {
                        displayName(CVTextColor.MENU_DEFAULT_TITLE + "Disable 'allow interactions'")
                    }
                else {
                    ItemStack(Material.STONE_BUTTON).apply {
                        displayName(CVTextColor.MENU_DEFAULT_TITLE + "Enable 'allow interactions'")
                    }
                }
            },
            {
                val state = (state as TileState)
                val allowInteract =
                    state.persistentDataContainer.get(
                        CraftventureKeys.TILE_STATE_ALLOW_INTERACT,
                        PersistentDataType.STRING
                    ) != null
                if (allowInteract) {
                    state.persistentDataContainer.remove(CraftventureKeys.TILE_STATE_ALLOW_INTERACT)
                    state.update()
                } else {
                    state.persistentDataContainer.set(
                        CraftventureKeys.TILE_STATE_ALLOW_INTERACT,
                        PersistentDataType.STRING,
                        "*"
                    )
                    state.update()
                }
                invalidate()
            }
        ),
        TileStateHelper(
            { state -> block.blockData is Openable },
            { state ->
                val state = block.blockData as Openable
                if (state.isOpen)
                    ItemStack(Material.LEVER).apply {
                        displayName(CVTextColor.MENU_DEFAULT_TITLE + "Close (openable)")
                    }
                else
                    ItemStack(Material.LEVER).apply {
                        displayName(CVTextColor.MENU_DEFAULT_TITLE + "Open (openable)")
                    }
            },
            {
                val state = block.blockData
                state as Openable
                state.isOpen = !state.isOpen
                block.blockData = state
                invalidate()
            }
        ),
        TileStateHelper(
            { state -> state is Lidded },
            { state ->
                state as Lidded
                ItemStack(Material.LEVER).apply {
                    displayName(CVTextColor.MENU_DEFAULT_TITLE + "Open (lidded)")
                }
            },
            {
                val state = (state as Lidded)
                state.open()
                invalidate()
            }
        ),
        TileStateHelper(
            { state -> state is Lidded },
            { state ->
                state as Lidded
                ItemStack(Material.LEVER).apply {
                    displayName(CVTextColor.MENU_DEFAULT_TITLE + "Close (lidded)")
                }
            },
            {
                val state = (state as Lidded)
                state.close()
                invalidate()
            }
        ),
        TileStateHelper(
            { state -> state.let { it is Lockable && it.isLocked } },
            { state ->
                state as Lockable
                ItemStack(Material.CHEST).apply {
                    displayName(CVTextColor.MENU_DEFAULT_TITLE + "Unlock")
                }
            },
            {
                val state = (state as Lockable)
                state.setLock(null)
                invalidate()
            }
        ),
        TileStateHelper(
            { state -> state is TileState },
            { state ->
                state as TileState
                val shopId = state.persistentDataContainer.shopId
                if (shopId != null)
                    ItemStack(Material.BARRIER).apply {
                        displayName(CVTextColor.MENU_DEFAULT_TITLE + "Remove shoplink '${shopId}'")
                    }
                else
                    ItemStack(Material.BOOK).apply {
                        displayName(CVTextColor.MENU_DEFAULT_TITLE + "Setup shoplink")
                    }
            },
            {
                executeSync { player.closeInventoryStack() }
                val state = (state as TileState)
                val shopId = state.persistentDataContainer.shopId
                if (shopId != null) {
                    state.persistentDataContainer.shopId = null
                    state.update()
                } else {
                    executeSync {
                        player.sendMessage(CVTextColor.serverError + "This feature is temporarily disabled")
//                        AnvilGUI.Builder()
//                            .plugin(PluginProvider.plugin)
//                            .onClose {
//
//                            }
//                            .onComplete { player, name ->
//                                state.persistentDataContainer.shopId = name
//                                state.update()
//                                AnvilGUI.Response.close()
//                            }
//                            .itemLeft(ItemStack(Material.STONE))
//                            .open(player)
                    }
                }
            }
        ),
    )
    val activeHelpers: List<TileStateHelper>
        get() = helpers.filter { it.active(state) }

    init {
        underlay = CvComponent.resettingInventoryOverlay(
            FontCodes.Inventory.emptyRowUnderlay.row1,
            FontCodes.Inventory.emptyRowUnderlay.row6,
        )
        updateTitle()
    }

    private fun updateTitle() {
        titleComponent = generateCenteredPagedTitle("TileState editor", false)
    }

    override fun onPageChanged() {
        super.onPageChanged()
        updateTitle()
    }

    override fun provideItems(): List<ItemStack> = activeHelpers.map { it.layout(state) }
    override fun onProvidedItemClicked(
        inventory: Inventory,
        index: Int,
        row: Int,
        column: Int,
        player: Player,
        action: InventoryAction
    ) {
        super.onProvidedItemClicked(inventory, index, row, column, player, action)
        activeHelpers.getOrNull(index)?.let { it.click(player) }
    }

    override fun onStaticItemClicked(
        inventory: Inventory,
        position: Int,
        row: Int,
        column: Int,
        player: Player,
        action: InventoryAction
    ) {
        if (handleNavigationButtonsClick(inventory, position, player, action)) return
        if (position == 1) {
            state = block.state
            invalidate()
        }
    }

    override fun onLayoutBase(inventory: Inventory) {
        addNavigationButtons(inventory)

        dataItem(Material.ARROW, 3).apply {
            displayName(CVTextColor.MENU_DEFAULT_TITLE + "Refresh state")
//            lore(CVChatColor.MENU_DEFAULT_LORE + "")
            inventory.setItem(1, this)
        }
    }

    data class TileStateHelper(
        val active: (BlockState) -> Boolean,
        val layout: (BlockState) -> ItemStack,
        val click: (Player) -> Unit
    )
}