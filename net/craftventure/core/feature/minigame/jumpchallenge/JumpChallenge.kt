package net.craftventure.core.feature.minigame.jumpchallenge

import net.craftventure.bukkit.ktx.extension.sendTo
import net.craftventure.bukkit.ktx.extension.setLeatherArmorColor
import net.craftventure.bukkit.ktx.manager.MessageBarManager
import net.craftventure.bukkit.ktx.manager.MessageBarManager.Message
import net.craftventure.bukkit.ktx.manager.MessageBarManager.display
import net.craftventure.bukkit.ktx.util.ChatUtils
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.feature.minigame.BaseMinigame
import net.craftventure.core.feature.minigame.Minigame
import net.craftventure.core.feature.minigame.MinigamePlayer
import net.craftventure.core.ktx.extension.asOrdinalAppended
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.ktx.util.TimeUtils
import net.craftventure.core.manager.EquipmentManager.EquippedItemData.Companion.toEquippedItemData
import net.craftventure.core.serverevent.PlayerEquippedItemsUpdateEvent
import net.craftventure.core.serverevent.PlayerStuckEvent
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType
import java.time.Duration
import java.util.concurrent.TimeUnit

class JumpChallenge(
    id: String,
    val minigameLevel: JumpChallengeLevel,
    minRequiredPlayers: Int = 2,
    name: String,
    exitLocation: Location,
    description: String,
    representationItem: ItemStack,
    warpName: String,
) : BaseMinigame<JumpChallengePlayer>(
    internalName = id,
    displayName = name,
    minRequiredPlayers = minRequiredPlayers,
    exitLocation = exitLocation,
    preparingTicks = 0,
    description = description,
    representationItem = representationItem,
    warpName = warpName,
) {
    override val maxPlayers: Int
        get() = minigameLevel.maxPlayers
    override val levelBaseTimeLimit: Long
        get() = TimeUnit.SECONDS.toMillis(minigameLevel.playTimeInSeconds.toLong())

    override fun represent(): ItemStack = ItemStack(Material.FEATHER)

    override fun provideDuration(): Minigame.MinigameDuration = Minigame.MinigameDuration(
        Duration.ofSeconds(minigameLevel.playTimeInSeconds.toLong()),
        Minigame.DurationType.MAXIMUM
    )

    @EventHandler(ignoreCancelled = true)
    fun onPlayerStuck(event: PlayerStuckEvent) {
        if (state == Minigame.State.RUNNING) {
            for (player in players) {
                if (event.player == player.player) {
                    player.allowNextTeleport()
                    player.player.teleport(
                        minigameLevel.spawnLocation.toLocation(player.player.world),
                        PlayerTeleportEvent.TeleportCause.PLUGIN
                    )
                    event.isCancelled = true
                    return
                }
            }
        }
    }

    // TODO: Implement some caching to prevent allocation/sorting a list every tick
    override fun sortedPlayers() = players.sortedWith(
        compareBy(
            { it.metadata.finishTime < 0 },
            { it.metadata.finishTime }
        )
    )

    override fun update(timePassed: Long) {
        super.update(timePassed)
        if (state == Minigame.State.RUNNING) {
            for (player in players) {
                player.metadata.update(level = minigameLevel)
            }
            val sortedPlayers = sortedPlayers()
            var allFinished = true
            var anyFinished = false

            for (player in sortedPlayers) {
                val timeLeft = maxGameTimeLength() - playTime
                display(
        player.player,
        Message(
            id = ChatUtils.ID_MINIGAME,
            text = Component.text(
                        "Game ending in ${DateUtils.format(timeLeft, "?")}",
                        CVTextColor.serverNotice
                    ),
            type = MessageBarManager.Type.MINIGAME,
            untilMillis = TimeUtils.secondsFromNow(1.0),
        ),
        replace = true,
    )
                if (!player.metadata.finished()) {
                    allFinished = false
                    anyFinished = true
                }
            }

            if (anyFinished) {

            }

            if (allFinished)
                stop(Minigame.StopReason.ALL_PLAYERS_FINISHED)
        }
    }

    override fun onUpdatePlayerWornItems(
        player: MinigamePlayer<JumpChallengePlayer>,
        event: PlayerEquippedItemsUpdateEvent
    ) {
        super.onUpdatePlayerWornItems(player, event)
        if (state == Minigame.State.PREPARING_GAME || state == Minigame.State.RUNNING) {
            event.appliedEquippedItems.balloonItem = null
//        event.wornData.miscItem = null
            event.appliedEquippedItems.title = null

            event.appliedEquippedItems.helmetItem =
                ItemStack(Material.LEATHER_HELMET).setLeatherArmorColor(Color.fromRGB(0xff0000)).toEquippedItemData()
            event.appliedEquippedItems.chestplateItem =
                ItemStack(Material.LEATHER_CHESTPLATE).setLeatherArmorColor(Color.fromRGB(0xff0000))
                    .toEquippedItemData()
            event.appliedEquippedItems.leggingsItem =
                ItemStack(Material.LEATHER_LEGGINGS).setLeatherArmorColor(Color.fromRGB(0xff0000)).toEquippedItemData()
            event.appliedEquippedItems.bootsItem =
                ItemStack(Material.LEATHER_BOOTS).setLeatherArmorColor(Color.fromRGB(0xff0000)).toEquippedItemData()
        }
    }

    override fun onPlayerLeft(minigamePlayer: MinigamePlayer<JumpChallengePlayer>, reason: Minigame.LeaveReason) {
        super.onPlayerLeft(minigamePlayer, reason)
        minigamePlayer.player.removePotionEffect(PotionEffectType.INVISIBILITY)
    }

    override fun onPreJoin(player: Player) {
        super.onPreJoin(player)
        val location = minigameLevel.spawnLocation.toLocation(Bukkit.getWorld("world")!!)
        player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN)
    }

    override fun provideMeta(player: Player): JumpChallengePlayer = JumpChallengePlayer(player)

    override fun onPreStop(stopReason: Minigame.StopReason) {
        val finishedNormally =
            stopReason == Minigame.StopReason.ALL_PLAYERS_FINISHED || stopReason == Minigame.StopReason.OUT_OF_TIME
        if (finishedNormally) {
            val players = sortedPlayers()
            for (minigamePlayer in players) {
                val player = minigamePlayer.player
                Translation.MINIGAME_WIN_HEADER.getTranslation(player)?.sendTo(player)
                for ((index, otherPlayer) in players.withIndex()) {
                    Translation.MINIGAME_ENTRY_TIMED.getTranslation(
                        player,
                        (index + 1).asOrdinalAppended(),
                        otherPlayer.player.name,
                        if (otherPlayer.metadata.finishTime > 0) DateUtils.formatWithMillis(
                            otherPlayer.metadata.finishTime - playStartTime,
                            "?"
                        ) else "DNF"
                    )?.sendTo(player)
                }
                Translation.MINIGAME_WIN_FOOTER.getTranslation(player)?.sendTo(player)
            }
        } else {
            for (minigamePlayer in players) {
                val player = minigamePlayer.player
//                if (stopReason == StopReason.TOO_FEW_PLAYERS) {
                Translation.MINIGAME_TOO_FEW_PLAYERS.getTranslation(player, displayName)?.sendTo(player)
//                } else {
//                    Translation.
//                }
            }
        }
    }

    override fun toJson(): Json = toJson(Json())

    class Json : BaseMinigame.Json() {
        override fun createGame(): Minigame {
            TODO("Not yet implemented")
        }
    }
}
