package net.craftventure.core.feature.minigame.snowball

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.MaterialConfig.dataItem
import net.craftventure.bukkit.ktx.extension.sendTo
import net.craftventure.bukkit.ktx.extension.setLeatherArmorColor
import net.craftventure.bukkit.ktx.manager.MessageBarManager
import net.craftventure.bukkit.ktx.manager.MessageBarManager.Message
import net.craftventure.bukkit.ktx.manager.MessageBarManager.display
import net.craftventure.bukkit.ktx.util.ChatUtils
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.async.executeAsync
import net.craftventure.core.feature.minigame.BaseMinigame
import net.craftventure.core.feature.minigame.Minigame
import net.craftventure.core.feature.minigame.MinigamePlayer
import net.craftventure.core.ktx.extension.asOrdinalAppended
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.ktx.util.TimeUtils
import net.craftventure.core.manager.EquipmentManager.EquippedItemData.Companion.toEquippedItemData
import net.craftventure.core.manager.ProjectileEvents.removeUponEnteringBubbleColumn
import net.craftventure.core.serverevent.PlayerEquippedItemsUpdateEvent
import net.craftventure.core.serverevent.PlayerStuckEvent
import net.craftventure.core.utils.ItemStackUtils
import net.craftventure.core.utils.TitleUtil.sendTitleWithTicks
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.type.BankAccountType
import net.craftventure.database.type.TransactionType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.*
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarFlag
import org.bukkit.boss.BarStyle
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType
import java.time.Duration
import java.util.concurrent.TimeUnit


class SnowballFight(
    id: String,
    val minigameLevel: SnowballFightLevel,
    minRequiredPlayers: Int = 2,
    name: String,
    exitLocation: Location,
    minKeepPlayingRequiredPlayers: Int = 2,
    val snowballItem: ItemStack = ItemStack(Material.SNOWBALL),
    val snowballCooldownTicks: Int = 8,
    val redColor: java.awt.Color = java.awt.Color(0xff0000),
    val blueColor: java.awt.Color = java.awt.Color(0x0000ff),
    val rewardWin: Int,
    val rewardDraw: Int,
    val rewardPlay: Int,
    val balanceType: BankAccountType = BankAccountType.VC,
    description: String,
    warpName: String?,
    representationItem: ItemStack,
    saveScores: Boolean,
) : BaseMinigame<SnowballPlayer>(
    internalName = id,
    displayName = name,
    minRequiredPlayers = minRequiredPlayers,
    exitLocation = exitLocation,
    preparingTicks = 0,
    minKeepPlayingRequiredPlayers = minKeepPlayingRequiredPlayers,
    description = description,
    representationItem = representationItem,
    warpName = warpName,
    saveScores = saveScores,
) {
    override val maxPlayers: Int
        get() = minigameLevel.maxPlayers
    override val levelBaseTimeLimit: Long
        get() = TimeUnit.SECONDS.toMillis(minigameLevel.playTimeInSeconds.toLong())

    private val bossBar = Bukkit.createBossBar(
        "§6Let the fight begin!",
        BarColor.YELLOW,
        BarStyle.SOLID,
        BarFlag.DARKEN_SKY
    )

    override fun provideDuration(): Minigame.MinigameDuration = Minigame.MinigameDuration(
        Duration.ofSeconds(minigameLevel.playTimeInSeconds.toLong()),
        Minigame.DurationType.EXACT
    )

    @EventHandler(ignoreCancelled = true)
    fun onPlayerStuck(event: PlayerStuckEvent) {
        if (state == Minigame.State.RUNNING) {
            for (player in players) {
                if (event.player == player.player) {
                    event.isCancelled = true
                    // TODO: Respawn after X seconds? Removes 1 kill from this member?
                    return
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onProjectileThrownEvent(event: ProjectileLaunchEvent) {
        val entity = event.entity
        if (entity is Snowball) {
            val shooter = entity.shooter
            if (shooter is Player) {
                players.firstOrNull { it.player === shooter }?.let { player ->
                    event.isCancelled = true

                    if (state == Minigame.State.RUNNING) {
                        shooter.setCooldown(Material.SNOWBALL, snowballCooldownTicks)

                        val snowball = player.player.world.spawn(entity.location, Snowball::class.java)
                        snowball.removeUponEnteringBubbleColumn()
                        player.player.world.playSound(
                            player.player.location,
                            Sound.ENTITY_SNOWBALL_THROW,
                            SoundCategory.PLAYERS,
                            1f,
                            1f
                        )
                        snowball.item = snowballItem
                        snowball.shooter = player.player
                        snowball.velocity = entity.velocity
                    }
                }
            }
        }
    }

    @EventHandler
    fun onEntityHit(event: EntityDamageByEntityEvent) {
        val entity = event.entity
        if (entity is Player) {
            players.firstOrNull { it.player === entity }?.let { hitPlayer ->
                event.isCancelled = true

                val projectile = event.damager
                if (projectile is Snowball) {
                    players.firstOrNull { it.player === projectile.shooter }?.let { shooter ->
                        if (shooter.metadata.team != hitPlayer.metadata.team) {
                            hitPlayer.metadata.hit(shooter)

                            if (hitPlayer.metadata.isDead) {
                                hitPlayer.allowNextTeleport()
                                hitPlayer.metadata.revive()
                                hitPlayer.player.teleport(
                                    minigameLevel.getSpawn(hitPlayer.metadata).toLocation(hitPlayer.player.world),
                                    PlayerTeleportEvent.TeleportCause.PLUGIN
                                )
                                players.forEach {
                                    it.player.sendMessage(
                                        (shooter.metadata.team.color + shooter.player.name) + (CVTextColor.serverNotice + " knocked ") + (hitPlayer.metadata.team.color + hitPlayer.player.name) + (CVTextColor.serverNotice + " out")
                                    )
                                }
                                updateScores()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateScores() {
        var redTeamKills = 0
        var blueTeamKills = 0

        for (player in players) {
            if (player.metadata.team == SnowballPlayer.Team.RED)
                redTeamKills += player.metadata.kills
            else if (player.metadata.team == SnowballPlayer.Team.BLUE)
                blueTeamKills += player.metadata.kills
        }

        when {
            redTeamKills > blueTeamKills -> {
                bossBar.setTitle("§eRed is winning with $redTeamKills vs $blueTeamKills")
                bossBar.color = BarColor.RED
            }

            blueTeamKills > redTeamKills -> {
                bossBar.setTitle("§eBlue is winning with $blueTeamKills vs $redTeamKills")
                bossBar.color = BarColor.BLUE
            }

            else -> {
                bossBar.setTitle("§7It's a draw")
                bossBar.color = BarColor.YELLOW
            }
        }
    }

//    @EventHandler(priority = EventPriority.NORMAL)
//    fun onPlayerRespawn(event: PlayerRespawnEvent) {
//        players.firstOrNull { it.player == event.player }?.apply {
//            event.respawnLocation = minigameLevel.getSpawn(this).toLocation(event.player.world)
//        }
//    }

    // TODO: Implement some caching to prevent allocation/sorting a list every tick
    override fun sortedPlayers() = players.sortedWith(
        compareBy(
            { -it.metadata.kills },
            { it.metadata.deaths }
        )
    )

    override fun update(timePassed: Long) {
        super.update(timePassed)
        if (state == Minigame.State.RUNNING) {
            for (player in players) {
                val timeLeft = maxGameTimeLength() - playTime
                display(
                    player.player,
                    Message(
                        id = ChatUtils.ID_MINIGAME,
                        text = Component.text(
                            "Game ending in ${
                                DateUtils.format(
                                    timeLeft,
                                    "?"
                                )
                            }, K${player.metadata.kills} / D${player.metadata.deaths}",
                            CVTextColor.serverNotice
                        ),
                        type = MessageBarManager.Type.MINIGAME,
                        untilMillis = TimeUtils.secondsFromNow(1.0),
                    ),
                    replace = true,
                )
            }
        }
    }

    override fun onPlayerJoined(player: MinigamePlayer<SnowballPlayer>) {
        super.onPlayerJoined(player)
        bossBar.addPlayer(player.player)
    }

    override fun onPlayerLeft(minigamePlayer: MinigamePlayer<SnowballPlayer>, reason: Minigame.LeaveReason) {
        super.onPlayerLeft(minigamePlayer, reason)
        minigamePlayer.player.removePotionEffect(PotionEffectType.ABSORPTION)
        bossBar.removePlayer(minigamePlayer.player)
        minigamePlayer.player.absorptionAmount = 0.0
        if (reason != Minigame.LeaveReason.GAME_STOPPING)
            updateScores()
    }

    override fun onUpdatePlayerWornItems(
        player: MinigamePlayer<SnowballPlayer>,
        event: PlayerEquippedItemsUpdateEvent
    ) {

        event.appliedEquippedItems.clearArmor()
        event.appliedEquippedItems.clearSpecials()

        if (state == Minigame.State.PREPARING_GAME || state == Minigame.State.RUNNING) {
            event.appliedEquippedItems.weaponItem = snowballItem.toEquippedItemData()

            if (player.metadata.team == SnowballPlayer.Team.RED) {
                event.appliedEquippedItems.helmetItem =
                    ItemStack(Material.LEATHER_HELMET).setLeatherArmorColor(Color.fromRGB(redColor.rgb and 0x00FFFFFF))
                        .toEquippedItemData()
                event.appliedEquippedItems.chestplateItem =
                    ItemStack(Material.LEATHER_CHESTPLATE).setLeatherArmorColor(Color.fromRGB(redColor.rgb and 0x00FFFFFF))
                        .toEquippedItemData()
                event.appliedEquippedItems.leggingsItem =
                    ItemStack(Material.LEATHER_LEGGINGS).setLeatherArmorColor(Color.fromRGB(redColor.rgb and 0x00FFFFFF))
                        .toEquippedItemData()
                event.appliedEquippedItems.bootsItem =
                    ItemStack(Material.LEATHER_BOOTS).setLeatherArmorColor(Color.fromRGB(redColor.rgb and 0x00FFFFFF))
                        .toEquippedItemData()
            } else {
                event.appliedEquippedItems.helmetItem =
                    ItemStack(Material.LEATHER_HELMET).setLeatherArmorColor(Color.fromRGB(blueColor.rgb and 0x00FFFFFF))
                        .toEquippedItemData()
                event.appliedEquippedItems.chestplateItem =
                    ItemStack(Material.LEATHER_CHESTPLATE).setLeatherArmorColor(Color.fromRGB(blueColor.rgb and 0x00FFFFFF))
                        .toEquippedItemData()
                event.appliedEquippedItems.leggingsItem =
                    ItemStack(Material.LEATHER_LEGGINGS).setLeatherArmorColor(Color.fromRGB(blueColor.rgb and 0x00FFFFFF))
                        .toEquippedItemData()
                event.appliedEquippedItems.bootsItem =
                    ItemStack(Material.LEATHER_BOOTS).setLeatherArmorColor(Color.fromRGB(blueColor.rgb and 0x00FFFFFF))
                        .toEquippedItemData()
            }
        }
    }

    override fun onStateChanged(oldState: Minigame.State, newState: Minigame.State) {
        super.onStateChanged(oldState, newState)
        if (newState == Minigame.State.RUNNING) {
            bossBar.setTitle("§6Let the fight begin!")
        }
    }

    override fun onPreJoin(player: Player) {
        super.onPreJoin(player)
        player.gameMode = GameMode.ADVENTURE
    }

    override fun provideMeta(player: Player): SnowballPlayer {
        val redCount = players.count { it.metadata.team == SnowballPlayer.Team.RED }
        val blueCount = players.count { it.metadata.team == SnowballPlayer.Team.BLUE }
        val team =
            if (redCount == blueCount) SnowballPlayer.Team.RED else if (redCount > blueCount) SnowballPlayer.Team.BLUE else SnowballPlayer.Team.RED
        val gamePlayer = SnowballPlayer(player, team)
        player.teleport(
            minigameLevel.getSpawn(gamePlayer).toLocation(player.world),
            PlayerTeleportEvent.TeleportCause.PLUGIN
        )
        return gamePlayer
    }

    override fun onPreStop(stopReason: Minigame.StopReason) {
        bossBar.removeAll()
        var redTeamKills = 0
        var blueTeamKills = 0

        for (player in players) {
            if (player.metadata.team == SnowballPlayer.Team.RED)
                redTeamKills += player.metadata.kills
            else if (player.metadata.team == SnowballPlayer.Team.BLUE)
                blueTeamKills += player.metadata.kills
        }

        val winningTeam = when {
            redTeamKills > blueTeamKills -> SnowballPlayer.Team.RED
            redTeamKills < blueTeamKills -> SnowballPlayer.Team.BLUE
            else -> null
        }

        when {
            redTeamKills > blueTeamKills -> players.forEach {
                it.player.sendTitleWithTicks(
                    20,
                    20 * 3,
                    20,
                    NamedTextColor.YELLOW,
                    "§4$redTeamKills§e vs §1$blueTeamKills",
                    TextColor.color(redColor.rgb),
                    "Red team wins"
                )
            }

            blueTeamKills > redTeamKills -> players.forEach {
                it.player.sendTitleWithTicks(
                    20,
                    20 * 3,
                    20,
                    NamedTextColor.YELLOW,
                    "§4$redTeamKills§e vs §1$blueTeamKills",
                    TextColor.color(blueColor.rgb),
                    "Blue team wins"
                )
            }

            else -> players.forEach {
                it.player.sendTitleWithTicks(
                    20,
                    20 * 3,
                    20,
                    NamedTextColor.YELLOW,
                    "§4$redTeamKills§e vs §1$blueTeamKills",
                    NamedTextColor.YELLOW,
                    "It's a draw"
                )
            }
        }

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
                        "K${otherPlayer.metadata.kills} / D${otherPlayer.metadata.deaths}"
                    )?.sendTo(player)
                }
                Translation.MINIGAME_WIN_FOOTER.getTranslation(player)?.sendTo(player)

                executeAsync {
                    val won = minigamePlayer.metadata.team == winningTeam
//                    (internalName == "summer2018_coconut").let {
                    val draw = winningTeam == null
                    val delta = if (won) rewardWin else if (draw) rewardDraw else rewardPlay
                    MainRepositoryProvider.bankAccountRepository
                        .delta(player.uniqueId, balanceType, delta.toLong(), TransactionType.MINIGAME)
                    when {
                        won -> player.sendMessage(CVTextColor.serverNotice + "+$delta ${balanceType.pluralName} for playing and winning")
                        draw -> player.sendMessage(CVTextColor.serverNotice + "+$delta ${balanceType.pluralName} for playing a draw")
                        else -> player.sendMessage(CVTextColor.serverNotice + "+$delta ${balanceType.pluralName} for playing")
                    }
//                    }
                    if (won) {
                        MainRepositoryProvider.achievementProgressRepository
                            .increaseCounter(player.uniqueId, "minigame_${internalName}_win")
//                        if (internalName == "summer2018_coconut") {
//                            val givenItem = MainRepositoryProvider.playerOwnedItemRepository.createOneLimited(player.uniqueId, "balloon_coconut", -1)
//                            if (givenItem)
//                                player.sendMessage(CVChatColor.COMMAND_GENERAL + "You are now the proud owner of a coconut balloon!")
//                        }
                    }
                    MainRepositoryProvider.achievementProgressRepository
                        .increaseCounter(player.uniqueId, "minigame_${internalName}_play")
                }
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

    override fun describeBalanceRewards(): List<Minigame.BalanceReward> {
        return super.describeBalanceRewards() + listOf(
            Minigame.BalanceReward(balanceType, rewardWin, "when your team wins"),
            Minigame.BalanceReward(balanceType, rewardDraw, "when it's a draw"),
            Minigame.BalanceReward(balanceType, rewardPlay, "when your team loses"),
        )
    }

    override fun toJson(): Json = toJson(Json())

    @JsonClass(generateAdapter = true)
    class Json : BaseMinigame.Json() {
        lateinit var level: SnowballFightLevel.Json
        var rewardAccountType: BankAccountType = BankAccountType.VC

        var rewardWin: Int = 10
        var rewardDraw: Int = 7
        var rewardPlay: Int = 5

        var snowballItem: String? = null
        var snowballCooldownTicks: Int = 8

        val balanceType: BankAccountType = BankAccountType.VC

        override fun createGame() = SnowballFight(
            id = internalName,
            minigameLevel = level.create(),
            minRequiredPlayers = minRequiredPlayers,
            name = displayName,
            exitLocation = exitLocation,
            minKeepPlayingRequiredPlayers = minKeepPlayingRequiredPlayers,
            snowballItem = ItemStackUtils.fromString(snowballItem) ?: ItemStack(Material.SNOWBALL),
            snowballCooldownTicks = snowballCooldownTicks,
            rewardWin = rewardWin,
            rewardDraw = rewardDraw,
            rewardPlay = rewardPlay,
            balanceType = balanceType,
            description = description,
            representationItem = ItemStackUtils.fromString(representationItem) ?: dataItem(Material.DIAMOND_SWORD, -1),
            warpName = warpName,
            saveScores = saveScores,
        )

//        id: String,
//        val minigameLevel: SnowballFightLevel,
//        minRequiredPlayers: Int = 2,
//        name: String,
//        exitLocation: Location,
//        minKeepPlayingRequiredPlayers: Int = 2
    }
}