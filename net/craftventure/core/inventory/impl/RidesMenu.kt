package net.craftventure.core.inventory.impl

import net.craftventure.bukkit.ktx.extension.*
import net.craftventure.bukkit.ktx.util.ComponentBuilder
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.displayNameWithBuilder
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.lore
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.loreWithBuilder
import net.craftventure.bukkit.ktx.util.ItemStackUtils2
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.CvComponent
import net.craftventure.chat.bungee.util.FontCodes
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.core.extension.openMenu
import net.craftventure.core.inventory.LayoutInventoryMenu
import net.craftventure.core.ktx.extension.format
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.ride.RideManager
import net.craftventure.core.ride.tracked.ParkTrain
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.isNew
import net.craftventure.database.bukkit.extensions.itemStack
import net.craftventure.database.bukkit.extensions.stateTranslation
import net.craftventure.database.bukkit.extensions.teleportIfPermissioned
import net.craftventure.database.generated.cvdata.tables.pojos.Realm
import net.craftventure.database.generated.cvdata.tables.pojos.Ride
import net.craftventure.database.generated.cvdata.tables.pojos.RideCounter
import net.craftventure.database.generated.cvdata.tables.pojos.Warp
import net.craftventure.database.repository.MinigameScoreRepository
import net.craftventure.database.repository.PlayerKeyValueRepository
import net.craftventure.database.type.MinigameScoreType
import net.craftventure.database.type.RideState
import net.craftventure.database.type.RideType
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.*

class RidesMenu(
    player: Player
) : LayoutInventoryMenu(
    itemStartColumnOffset = 0,
    itemStartRowOffset = 1,
    maxItemRows = 4,
    maxItemColumns = 9,
    owner = player,
) {
    private var items = emptyList<InventoryItem>()
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }
    private var loading: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                updateTitle()
            }
        }
    private var showHistoricRides: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }
    var realmFilter: Realm? = null
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    init {
        underlay = CvComponent.resettingInventoryOverlay(
            FontCodes.Inventory.emptyRowUnderlay.row1,
            FontCodes.Inventory.emptyRowUnderlay.row6,
        )
        updateTitle()
        executeAsync {
            val items = mutableListOf<InventoryItem>()
            val rideCounterDatabase = MainRepositoryProvider.rideCounterRepository
            for (ride in MainRepositoryProvider.rideRepository.itemsPojo()) {
//                Logger.debug("Ride ${ride.id} open=${ride.openSince} featured=${ride.featured} new=${ride.isNew}")
                val rideCounter = rideCounterDatabase.get(player.uniqueId, ride.name!!)
                val score: String? = when (ride.name) {
                    "riversofouzo" -> {
                        val topScoreEver = MainRepositoryProvider.minigameScoreRepository.getTopCounters(
                            gameId = ride.name,
                            limit = 1,
                            type = MinigameScoreType.TOTAL,
                            scoreAscending = false,
                            includeCrew = true,
                            scoreAggregate = MinigameScoreRepository.ScoreAggregate.MAX,
                            uuids = listOf(player.uniqueId)
                        ).firstOrNull()
                        if (topScoreEver != null) {
                            "Personal top score of ${topScoreEver.score}"
                        } else null
                    }

                    "autopia" -> {
                        val topScoreEver = MainRepositoryProvider.minigameScoreRepository.getTopCounters(
                            gameId = "autopia2",
                            limit = 1,
                            type = MinigameScoreType.TOTAL,
                            scoreAscending = false,
                            includeCrew = true,
                            scoreAggregate = MinigameScoreRepository.ScoreAggregate.MIN,
                            uuids = listOf(player.uniqueId)
                        ).firstOrNull()
                        if (topScoreEver != null) {
                            "Personal fastest lap of ${DateUtils.format(topScoreEver.score!!.toLong(), "?")}"
                        } else null
                    }

                    "parktrain" -> {
                        val distanceTravelled = MainRepositoryProvider.playerKeyValueRepository
                            .get(player.uniqueId, PlayerKeyValueRepository.DISTANCE_TRAVELED_BY_TRAIN)
                        val actualDistance = distanceTravelled?.value?.toDoubleOrNull()
                        if (actualDistance != null) {
                            val description = trainDescriptions.filter { it.inRange(actualDistance) }
                                .takeIf { it.isNotEmpty() }
                                ?.random()
                                ?.generator?.invoke(actualDistance)
                            val km = actualDistance / 1000.0
                            "You travelled a total distance of ${km.format(2)} km${description ?: ""}"
                        } else null
                    }

                    "tram" -> {
                        val distanceTravelled = MainRepositoryProvider.playerKeyValueRepository
                            .get(player.uniqueId, PlayerKeyValueRepository.DISTANCE_TRAVELED_BY_TRAM)
                        val actualDistance = distanceTravelled?.value?.toDoubleOrNull()
                        if (actualDistance != null) {
                            val description = tramDescriptions.filter { it.inRange(actualDistance) }
                                .takeIf { it.isNotEmpty() }
                                ?.random()
                                ?.generator?.invoke(actualDistance)
                            val km = actualDistance / 1000.0
                            "You travelled a total distance of ${km.format(2)} km${description ?: ""}"
                        } else null
                    }

                    else -> null
                }
                val rideData =
                    ride.representAsItemStack(
                        player = player,
                        rideCounter = rideCounter,
                        additionalInfo = score
                    )

                val item = InventoryItem(
                    ride,
                    rideCounter,
                    rideData.warp,
                    rideData.itemStack
                )
                items.add(item)
            }

            this.items = items.sortedWith(rideComparator)
            loading = false
        }
    }

    private fun updateTitle() {
        titleComponent = generateCenteredPagedTitle("Rides", items.isEmpty())
    }

    override fun onPageChanged() {
        super.onPageChanged()
        updateTitle()
    }

    private fun filteredItems() = items.asSequence()
        .filter {
            it.ride.state!!.showInMenu || (showHistoricRides && (it.rideCounter?.count ?: 0) > 0)
        }
        .filter {
            realmFilter == null || realmFilter?.id.equals(it.ride.realmId, true)
        }

    override fun provideItems(): List<ItemStack> = filteredItems().map { it.item }.toList()
    override fun onProvidedItemClicked(
        inventory: Inventory,
        index: Int,
        row: Int,
        column: Int,
        player: Player,
        action: InventoryAction
    ) {
        super.onProvidedItemClicked(inventory, index, row, column, player, action)

        filteredItems().toList().getOrNull(index)?.let {
            executeSync {
                player.closeInventory(InventoryCloseEvent.Reason.PLAYER)
                it.warp?.teleportIfPermissioned(player)
            }
        }
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
        if (inventory.getItem(position) == null) return

        if (position == 5) {
            executeSync { player.openMenu(RealmsMenu(player)) }
        } else if (position == 6) {
            realmFilter = null
        } else if (position == 7) {
            showHistoricRides = !showHistoricRides
        }
    }

    override fun onLayoutBase(inventory: Inventory) {
        addNavigationButtons(inventory)

        inventory.setItem(
            5,
            ItemStack(Material.ENDER_CHEST).displayName(CVTextColor.MENU_DEFAULT_TITLE + "Open the realms menu")
        )
        realmFilter?.let { realmFilter ->
            inventory.setItem(
                6,
                ItemStack(Material.YELLOW_TERRACOTTA)
                    .displayName(CVTextColor.MENU_DEFAULT_TITLE + "Remove realm filter")
                    .loreWithBuilder { text(("Currently filtering on " + realmFilter.displayName)) }
            )
        }
        inventory.setItem(
            7,
            ItemStack(if (showHistoricRides) Material.OAK_SAPLING else Material.DEAD_BUSH)
                .displayName(CVTextColor.MENU_DEFAULT_TITLE + (if (showHistoricRides) "Hide historic (removed) rides" else "Show historic (removed) rides"))
                .loreWithBuilder { text("Only historic rides which you rode at least 1 time will be shown") }
        )
    }

    data class InventoryItem(
        val ride: Ride,
        val rideCounter: RideCounter?,
        val warp: Warp?,
        val item: ItemStack
    )

    companion object {
        val rideComparator = compareByDescending<InventoryItem> { it.ride.featured }
            .thenBy { it.ride.state }
            .thenBy { it.ride.displayName }
            .thenBy { it.ride.openSince }

        val trainDescriptions = listOf(
            TrainDistanceDescription(10_000_000, Int.MAX_VALUE) {
                ", has science gone too far?"
            },
            TrainDistanceDescription(1_000_000, Int.MAX_VALUE) {
                ", which is quite far for a ride on a virtual train in a virtual themepark"
            },
            TrainDistanceDescription(5_000_000, Int.MAX_VALUE) {
                ", what are you even trying to achieve?"
            },
            TrainDistanceDescription(1_000, 11_350) {
                ", that's ${
                    (it / 11_350).times(100)
                        .format(2)
                }% to the deepest point on earth, the Mariana Trench"
            },
            TrainDistanceDescription(1_000, 8_850) {
                ", that's ${
                    (it / 8_850).times(100)
                        .format(2)
                } percent of the height of mount everest"
            },
            TrainDistanceDescription(250_000, Int.MAX_VALUE) {
                ", that's ${(it / 10_921_000).format(2)} times around the moon"
            },
            TrainDistanceDescription(500_000, Int.MAX_VALUE) {
                ", that's ${(it / 21_344_000).format(2)} times around mars"
            },
            TrainDistanceDescription(1_000_000, Int.MAX_VALUE) {
                ", that's ${(it / 40_075_000).format(2)} times around the earth"
            },
            TrainDistanceDescription(750_000, 30_000_000) {
                ", that's ${(it / 30_000_000).format(2)} times the length of the Pan-American highway"
            },
            TrainDistanceDescription(0, Int.MAX_VALUE) {
                val totalLength = ParkTrain.totalLength
                ", that's ${(it / totalLength).format(2)} times the total length of the current parktrain route"
            }
        )

        val tramDescriptions = listOf(
            TrainDistanceDescription(10_000_000, Int.MAX_VALUE) {
                ", has science gone too far?"
            },
            TrainDistanceDescription(1_000_000, Int.MAX_VALUE) {
                ", which is quite far for a ride on a virtual tram in a virtual themepark"
            },
            TrainDistanceDescription(5_000_000, Int.MAX_VALUE) {
                ", what are you even trying to achieve?"
            },
            TrainDistanceDescription(1_000, 11_350) {
                ", that's ${
                    (it / 11_350).times(100)
                        .format(2)
                }% to the deepest point on earth, the Mariana Trench"
            },
            TrainDistanceDescription(1_000, 8_850) {
                ", that's ${
                    (it / 8_850).times(100)
                        .format(2)
                } percent of the height of mount everest"
            },
            TrainDistanceDescription(250_000, Int.MAX_VALUE) {
                ", that's ${(it / 10_921_000).format(2)} times around the moon"
            },
            TrainDistanceDescription(500_000, Int.MAX_VALUE) {
                ", that's ${(it / 21_344_000).format(2)} times around mars"
            },
            TrainDistanceDescription(1_000_000, Int.MAX_VALUE) {
                ", that's ${(it / 40_075_000).format(2)} times around the earth"
            },
            TrainDistanceDescription(750_000, 30_000_000) {
                ", that's ${(it / 30_000_000).format(2)} times the length of the Pan-American highway"
            },
//            TrainDistanceDescription(0, Int.MAX_VALUE) {
//                val totalLength = ParkTrain.totalLength
//                ", that's ${(it / totalLength).format(2)} times the total length of the current parktrain route"
//            }
        )

        data class RideRepresentation(
            val ride: Ride,
            val warp: Warp?,
            val itemStack: ItemStack
        )

        fun Ride.representAsItemStack(
            player: Player?,
            rideCounter: RideCounter?,
            additionalInfo: String? = null
        ): RideRepresentation {
            val ride = this
            val warp = ride.warpId?.let { MainRepositoryProvider.warpRepository.findCachedByName(it) }

            val baseItem =
                if (ride.state!!.isOpen || ride.state == RideState.HISTORIC || ride.state == RideState.UNDER_CONSTRUCTION) {
                    val item =
                        ride.itemStackDataId?.let { MainRepositoryProvider.itemStackDataRepository.findCached(it) }
                    item?.itemStack
                } else null

            val itemStack: ItemStack = baseItem ?: ItemStack(Material.TERRACOTTA)

            if (baseItem == null) {
                if (ride.state === RideState.OPEN)
                    itemStack.type = Material.LIME_TERRACOTTA
                else if (ride.state === RideState.VIP_PREVIEW)
                    itemStack.type =
                        if (player?.isVIP() == true) Material.BLUE_TERRACOTTA else Material.RED_TERRACOTTA
                else if (ride.state === RideState.CLOSED)
                    itemStack.type = Material.RED_TERRACOTTA
                else if (ride.state === RideState.MAINTENANCE)
                    itemStack.type = Material.ORANGE_TERRACOTTA
            }

            if (ride.state!!.isOpen && ride.featured!!) {
                ItemStackUtils2.addEnchantmentGlint(itemStack)
            }

            itemStack.displayNameWithBuilder {
                if (ride.state == RideState.VIP_PREVIEW)
                    text("VIP-Preview! ", CVTextColor.serverNotice)
                if (ride.state!!.isOpen && ride.isNew)
                    text("NEW! ", CVTextColor.serverNotice)

                val rideNameSuffix =
                    if (ride.type != null && ride.type != RideType.UNKNOWN) " (" + ride.type!!.stateName + ")" else ""
                text(ride.displayName + rideNameSuffix)
            }

            val openText: String? = if (ride.openSince != null) {
                val localDate = ride.openSince!!
                    .atZone(ZoneId.of("Europe/Amsterdam"))
                    .withZoneSameLocal(ZoneId.of("Europe/Amsterdam"))
                val now = ZonedDateTime.now()
                if (now.isBefore(localDate)) {
                    "Opening at ${
                        localDate.format(
                            DateTimeFormatter.ofPattern("eee MMMM d, HH:mm zzz").withLocale(Locale.ENGLISH)
                        )
                    }"
                } else null
//                        lore += CVChatColor.MENU_DEFAULT_LORE_ACCENT + "Opened since ${localDate.format(
//                            DateTimeFormatter.ofLocalizedDate(
//                                FormatStyle.MEDIUM
//                            ).withLocale(Locale.ENGLISH)
//                        )}\n"
            } else null

            val loreBuilder = ComponentBuilder.LoreBuilder()

            loreBuilder.text(
                openText ?: ride.state!!.stateTranslation.getRawTranslation(
                    player
                ) ?: "?", if (ride.state!!.isOpen) NamedTextColor.DARK_GREEN else NamedTextColor.RED
            )

            CraftventureCore.getOperatorManager().getOpereableRideById(ride.name!!)?.let { opereableRide ->
                val operator = opereableRide.getOperatorForSlot(0) ?: return@let
                loreBuilder.text(" (Operated by ${operator.name})", null)
            }

            if (ride.description != null) {
                loreBuilder.moveToBlankLine()
                loreBuilder.text(ride.description!!)
            }

            val rideInstance = RideManager.getRide(ride.name!!)
            rideInstance?.getQueues()?.let { queues ->
                queues.forEach {
                    loreBuilder.emptyLines()
//                    val min = it.getCurrentEstimateMin()
                    val max = it.getCurrentEstimateMax()
                    if (it.isActive /*&& min != null*/ && max != null) {
//                        val minFormatted = DateUtils.format(min * 1000L, "?")
                        val maxFormatted = DateUtils.format(max * 1000L, "?")

                        loreBuilder.accented("Estimated queue time of $maxFormatted")
                    }// else
//                        loreBuilder.accented("Queue ${it.id} active=${it.active} queued=${it.getQueuedCount()} range=${it.getCurrentEstimateMin()}/${it.getCurrentEstimateMax()}")
                }

            }
            //            if (!ride.state.isOpen)

//                lore += "\n${CVChatColor.MENU_DEFAULT_LORE_ACCENT}$openText\n"

            if (additionalInfo != null) {
                loreBuilder.emptyLines()
                loreBuilder.accented(additionalInfo)
            }

            if (player != null) {
                loreBuilder.emptyLines()
                loreBuilder.accented(
                    "Ridden " + (rideCounter?.count
                        ?: 0) + " times"
                )
                rideCounter?.lastAt?.let { dateTime ->
                    loreBuilder.text(
                        ", last ride at ${
                            dateTime.format(
                                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM)
                                    .withLocale(Locale.ENGLISH)
                            )
                        } (at zone ${
                            ZoneId.of("Europe/Amsterdam").getDisplayName(TextStyle.NARROW, Locale.getDefault())
                        })",
                        usePreviousColor = true,
                        addSpace = false
                    )
                }
            }

            if (warp != null) {
                loreBuilder.action("Click to warp")
            }
            itemStack.lore(loreBuilder)
            itemStack.hideAttributes()
                .hideEnchants()
                .hideUnbreakable()
                .unbreakable()

            return RideRepresentation(
                ride,
                warp,
                itemStack
            )
        }
    }

    data class TrainDistanceDescription(
        val startMeters: Int,
        val endMeters: Int,
        val generator: (distanceInMeters: Double) -> String
    ) {
        fun inRange(distance: Double) = distance in startMeters.toDouble()..endMeters.toDouble()
    }
}