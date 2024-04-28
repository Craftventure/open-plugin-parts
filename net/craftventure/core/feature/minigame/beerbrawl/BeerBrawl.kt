package net.craftventure.core.feature.minigame.beerbrawl

import net.craftventure.bukkit.ktx.extension.add
import net.craftventure.bukkit.ktx.extension.sendTo
import net.craftventure.bukkit.ktx.extension.setLeatherArmorColor
import net.craftventure.bukkit.ktx.manager.MessageBarManager
import net.craftventure.bukkit.ktx.manager.MessageBarManager.Message
import net.craftventure.bukkit.ktx.manager.MessageBarManager.display
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.bukkit.ktx.util.ChatUtils
import net.craftventure.bukkit.ktx.util.ComponentBuilder.Companion.displayNameWithBuilder
import net.craftventure.bukkit.ktx.util.SoundUtils
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.CVTextColor.serverNotice
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.extension.rewardAchievement
import net.craftventure.core.feature.minigame.BaseMinigame
import net.craftventure.core.feature.minigame.Minigame
import net.craftventure.core.feature.minigame.MinigamePlayer
import net.craftventure.core.ktx.extension.asOrdinalAppended
import net.craftventure.core.ktx.extension.nextDoubleRange
import net.craftventure.core.ktx.util.TimeUtils
import net.craftventure.core.manager.EquipmentManager.EquippedItemData.Companion.toEquippedItemData
import net.craftventure.core.serverevent.PlayerEquippedItemsUpdateEvent
import net.craftventure.core.serverevent.PlayerStuckEvent
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.type.BankAccountType
import net.craftventure.database.type.TransactionType
import net.kyori.adventure.text.Component
import org.bukkit.Color
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.time.Duration
import java.util.concurrent.TimeUnit

class BeerBrawl(
    id: String,
    val minigameLevel: BeerBrawlLevel,
    minRequiredPlayers: Int = if (PluginProvider.isTestServer()) 1 else 3,
    minKeepPlayingRequiredPlayers: Int = 1,
    name: String,
    exitLocation: Location,
    description: String,
    representationItem: ItemStack,
    warpName: String,
) : BaseMinigame<BeerBrawlPlayer>(
    internalName = id,
    displayName = name,
    minRequiredPlayers = minRequiredPlayers,
    exitLocation = exitLocation,
    minKeepPlayingRequiredPlayers = minKeepPlayingRequiredPlayers,
//    startCountdownTicks = 5 * 20,
    preparingTicks = 0,
    description = description,
    representationItem = representationItem,
    warpName = warpName,
) {
    override val maxPlayers: Int
        get() = minigameLevel.maxPlayers
    override val levelBaseTimeLimit: Long
        get() = TimeUnit.SECONDS.toMillis(minigameLevel.playTimeInSeconds.toLong())

    override fun provideDuration(): Minigame.MinigameDuration = Minigame.MinigameDuration(
        Duration.ofSeconds(210),
        Minigame.DurationType.EXACT
    )

    private var substate = SubState.INTRO
        set(value) {
            if (field != value) {
                field = value

                currentSubStateCounter.reset()

                when (value) {
                    SubState.INTRO -> {
                    }

                    SubState.PLAY -> {
                        players.forEach { it.metadata.startFind() }
                    }

                    SubState.FINALE -> {
                        players.forEach { it.metadata.stopFind() }
                    }
                }
            }
        }
    private var currentSubStateCounter = Counter()
    private var updateTick = 0

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        players.firstOrNull { it.player == event.player }?.let {
            if (isRunning) {
                event.respawnLocation = minigameLevel.startLocation
            } else {
                event.respawnLocation = exitLocation
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerStuck(event: PlayerStuckEvent) {
        if (state == Minigame.State.RUNNING) {
            // Cancel players using /stuck during minigame
            for (player in players) {
                if (event.player == player.player) {
                    event.isCancelled = true
                    player.metadata.beer = null
                    player.player.teleport(minigameLevel.startLocation)
                    return
                }
            }
        }
    }

    private fun smash(player: BeerBrawlPlayer, target: BeerBrawlPlayer) {
        if (player.player.getCooldown(Material.STICK) > 0) return
        executeAsync {
            player.player.rewardAchievement("minigame_beerbrawl_slap_other")
            target.player.rewardAchievement("minigame_beerbrawl_get_slapped")
        }
        player.player.setCooldown(Material.STICK, (20 * 1.5).toInt())
//        Logger.info("Handle ${player.player.name} smashing ${target.player.name}")
        val random = CraftventureCore.getRandom()
        target.player.velocity = target.player.velocity.clone()
            .add(random.nextDoubleRange(-0.6, 0.6), random.nextDoubleRange(0.0, 0.5), random.nextDoubleRange(-0.6, 0.6))
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val clickedPlayer = event.entity as? Player ?: return
        val clickedGamePlayer = players.firstOrNull { it.player === clickedPlayer } ?: return
        val player = players.firstOrNull { it.player === event.damager } ?: return

        event.isCancelled = true
        smash(player.metadata, clickedGamePlayer.metadata)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerInteractEntityEvent(event: PlayerInteractEntityEvent) {
        val clickedPlayer = event.rightClicked as? Player ?: return
        val clickedGamePlayer = players.firstOrNull { it.player === clickedPlayer } ?: return
        if (event.hand != EquipmentSlot.HAND) return
        val player = players.firstOrNull { it.player === event.player } ?: return

        event.isCancelled = true
        smash(player.metadata, clickedGamePlayer.metadata)
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.clickedBlock == null) return
        if (event.hand != EquipmentSlot.HAND) return
        val player = players.firstOrNull { it.player === event.player } ?: return

        if (event.clickedBlock?.type != Material.AIR) {
            val clickedType = BeerType.values().firstOrNull { beerType ->
                val location = event.clickedBlock!!.location
                val beerLocation = beerType.location
                location.blockX == beerLocation.blockX && location.blockY == beerLocation.blockY && location.blockZ == beerLocation.blockZ
            }

//            Logger.info("CLicked type =$clickedType")

            if (clickedType != null) {
                event.isCancelled = true
                if (player.player.getCooldown(Material.GLASS_BOTTLE) > 0) return

                player.player.setCooldown(Material.GLASS_BOTTLE, 20 * 2)
                player.player.setCooldown(Material.POTION, 20 * 2)
                player.metadata.beer = clickedType
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onEntityDamageByEntityEvent(event: EntityDamageByEntityEvent) {
        val player = players.firstOrNull { it.player === event.damager }
        if (player != null) {
            val isPater = event.entity.location.distanceSquared(minigameLevel.beerLocation) < 2 * 2
            if (isPater) {
                executeAsync {
                    player.player.rewardAchievement("minigame_beerbrawl_slap_pater")
                }
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    override fun onEntityDamageEvent(event: EntityDamageEvent) {
        val player = players.firstOrNull { it.player === event.entity }
        if (player != null) {
            if (event.cause == EntityDamageEvent.DamageCause.FIRE) {
                executeAsync {
                    player.player.rewardAchievement("minigame_beerbrawl_fire")
                }
            }
        }
    }

    override fun sortedPlayers() = players.sortedBy { -it.metadata.score }

    override fun update(timePassed: Long) {
        super.update(timePassed)
        if (state != Minigame.State.RUNNING) return

        updateTick++

        val range = currentSubStateCounter.range()
        val previousUpdateTime = range.start
        val updateTime = range.endInclusive - 1

//        Logger.info("Update from $previousUpdateTime to $updateTime")

        val sortedPlayers = sortedPlayers()
        val anyScored = sortedPlayers.any { it.metadata.score > 0 }
        if (anyScored) {
            for ((index, player) in sortedPlayers.withIndex()) {
                display(
                    player.player,
                    Message(
                        id = ChatUtils.ID_MINIGAME,
                        text = Component.text(
                            "You are ${(index + 1).asOrdinalAppended()} with a total score of ${player.metadata.score}",
                            CVTextColor.serverNotice
                        ),
                        type = MessageBarManager.Type.MINIGAME,
                        untilMillis = TimeUtils.secondsFromNow(1.0),
                    ),
                    replace = true,
                )
            }
        }

        when (substate) {
            SubState.INTRO -> {
                if (0 in previousUpdateTime..updateTime) {
                    players.forEach {
                        it.player.sendMessage(CVTextColor.serverNotice + "Hello there! Come over to my table!")
                    }
                }
                if (3000 in previousUpdateTime..updateTime) {
                    players.forEach {
                        it.player.sendMessage(CVTextColor.serverNotice + "Apparently my waiter couldn't handle me anymore, so I need you to get me my beers.")
                    }
                }
                if (8000 in previousUpdateTime..updateTime) {
                    players.forEach {
                        it.player.sendMessage(CVTextColor.serverNotice + "I like them being delivered as quickly as possible, I'll tell you what I want, and you bring it to me.. okay?")
                    }
                }

                if (updateTime > 24000) {
                    substate = SubState.PLAY
                }
            }

            SubState.PLAY -> {
                players.forEach {
                    it.metadata.update(this, minigameLevel)
                    if (it.player in minigameLevel.toiletArea) {
                        it.metadata.enterToilet()
                    }
                }
                if (updateTime > 60 * 3 * 1000) {
                    substate = SubState.FINALE
                }
            }

            SubState.FINALE -> {
                if (0 in previousUpdateTime..updateTime) {
                    players.forEach {
                        it.player.playSound(minigameLevel.beerLocation, SOUND_FINALE, 3f, 1f)
                        it.player.sendMessage(CVTextColor.serverNotice + "Ok... I had enough now")
                    }
                }
                if (4000 in previousUpdateTime..updateTime) {
                    players.forEach {
                        it.player.sendMessage(CVTextColor.serverNotice + "Thank you for delivering my beer. I'm going to sleep again now, see you around!")
                    }
                }

                if (updateTime > 6000) {
                    stop(Minigame.StopReason.ALL_PLAYERS_FINISHED)
                }
            }
        }

//            for (player in players) {
//                val timeLeft = maxGameTimeLength() - playTime()
//                MessageBarManager.display(
//                        player.player,
//                        ChatUtils.createComponent(
//                                "Game ending in ${DateUtils.format(timeLeft, "?")}, your score ${player.score}",
//                                CVChatColor.WARNING
//                        ),
//                        MessageBarManager.Type.MINIGAME,
//                        TimeUtils.secondsFromNow(1.0),
//                        ChatUtils.ID_MINIGAME
//                )
//            }
    }


//    private fun getRetrievalScore(): Int {
//        val finishedPlayers = players.count { it.hasFinishedThisRound }
//        val score = when (finishedPlayers) {
//            0 -> 10
//            1 -> 8
//            2 -> 6
//            3 -> 5
//            4 -> 4
//            5 -> 3
//            6 -> 2
//            7 -> 1
//            else -> 0
//        }
//        return score
//    }

    override fun onPlayerLeft(minigamePlayer: MinigamePlayer<BeerBrawlPlayer>, reason: Minigame.LeaveReason) {
        super.onPlayerLeft(minigamePlayer, reason)
        minigamePlayer.player.removePotionEffect(PotionEffectType.CONFUSION)
        minigamePlayer.metadata.cleanup()
    }

    override fun onUpdatePlayerWornItems(
        player: MinigamePlayer<BeerBrawlPlayer>,
        event: PlayerEquippedItemsUpdateEvent
    ) {
        super.onUpdatePlayerWornItems(player, event)

        event.appliedEquippedItems.clearArmor()
        event.appliedEquippedItems.clearSpecials()

        if (state == Minigame.State.PREPARING_GAME || state == Minigame.State.RUNNING) {
            event.appliedEquippedItems.balloonItem = null
            event.appliedEquippedItems.consumptionItem =
                ItemStack(if (player.metadata.beer != null) Material.POTION else Material.GLASS_BOTTLE).apply {
                    val meta = this.itemMeta
                    if (player.metadata.beer != null) {
                        meta?.displayName(serverNotice + "${player.metadata.beer?.displayName} (${player.metadata.beer?.kind?.displayName})")
                    } else {
                        meta?.displayName(CVTextColor.subtle + "Empty bottle")
                    }
                    if (meta is PotionMeta) {
                        if (player.metadata.beer != null) {
                            meta.color = Color.fromRGB(0xebc701)
                        } else {
                            meta.color = Color.fromRGB(0x7aadff)
                        }
                    }
                    this.itemMeta = meta
                }.toEquippedItemData()
            event.appliedEquippedItems.weaponItem = ItemStack(Material.STICK).apply {
                displayNameWithBuilder {
                    text("Brawlstick ")
                    subtle("(you don't have to hold this in your hand to brawl!)")
                }
            }.toEquippedItemData()

            event.appliedEquippedItems.helmetItem = null
            event.appliedEquippedItems.chestplateItem =
                ItemStack(Material.LEATHER_CHESTPLATE).setLeatherArmorColor(Color.fromRGB(0xffffff))
                    .toEquippedItemData()
            event.appliedEquippedItems.leggingsItem =
                ItemStack(Material.LEATHER_LEGGINGS).setLeatherArmorColor(Color.fromRGB(0x000000)).toEquippedItemData()
            event.appliedEquippedItems.bootsItem =
                ItemStack(Material.LEATHER_BOOTS).setLeatherArmorColor(Color.fromRGB(0x000000)).toEquippedItemData()

//            event.wornData.balloonItem = null
//            event.wornData.miscItem = null
            event.appliedEquippedItems.title = null
        } else {
//            event.wornData.helmetItem = null
//            event.wornData.chestplateItem = null
//            event.wornData.leggingsItem = null
//            event.wornData.bootsItem = null
//            event.wornData.weaponItem = null
        }
    }

    override fun onStateChanged(oldState: Minigame.State, newState: Minigame.State) {
        super.onStateChanged(oldState, newState)
        if (newState == Minigame.State.PREPARING_GAME) {
            substate = SubState.INTRO
            currentSubStateCounter.reset()
            val gamePlayers = mutableListOf<BeerBrawlPlayer>()
            minigameLevel.area.world.entities.forEach {
                if (minigameLevel.area.isInArea(it) && it.name == "camera") {
                    it.remove()
                }
            }
        }
    }

    override fun onPreJoin(player: Player) {
        super.onPreJoin(player)
        player.gameMode = GameMode.ADVENTURE
        player.teleport(minigameLevel.startLocation, PlayerTeleportEvent.TeleportCause.PLUGIN)
        player.playSound(minigameLevel.beerLocation, SOUND_INTRO, 3f, 1f)
    }

    override fun provideMeta(player: Player): BeerBrawlPlayer = BeerBrawlPlayer(player)

    override fun describeBalanceRewards(): List<Minigame.BalanceReward> {
        return super.describeBalanceRewards() + listOf(
            Minigame.BalanceReward(BankAccountType.VC, 10, "when you have the highscore"),
            Minigame.BalanceReward(BankAccountType.VC, 5, "in any other cases"),
        )
    }

    override fun onPreStop(stopReason: Minigame.StopReason) {
        val finishedNormally =
            stopReason == Minigame.StopReason.ALL_PLAYERS_FINISHED || stopReason == Minigame.StopReason.OUT_OF_TIME
        if (finishedNormally) {
            val players = sortedPlayers()
            val fullGame = minigameLevel.maxPlayers == players.size
            val highestScore = players.firstOrNull()?.metadata?.score ?: 1000000
            for (minigamePlayer in players) {
                val player = minigamePlayer.player
                Translation.MINIGAME_WIN_HEADER.getTranslation(player)?.sendTo(player)
                for ((index, otherPlayer) in players.withIndex()) {
                    Translation.MINIGAME_ENTRY_TIMED.getTranslation(
                        player,
                        (index + 1).asOrdinalAppended(),
                        otherPlayer.player.name,
                        otherPlayer.metadata.score
                    )?.sendTo(player)
                }
                Translation.MINIGAME_WIN_FOOTER.getTranslation(player)?.sendTo(player)

                val first = minigamePlayer.metadata.score == highestScore
                val delta = if (first) 10L else 5L
                val accountType = BankAccountType.VC

                executeAsync {
                    MainRepositoryProvider.bankAccountRepository
                        .delta(player.uniqueId, accountType, delta, TransactionType.MINIGAME)
                    when {
                        first -> {
                            player.sendMessage(CVTextColor.serverNotice + "+$delta ${accountType.pluralName} for playing and finishing first")
                            MainRepositoryProvider.achievementProgressRepository
                                .increaseCounter(player.uniqueId, "minigame_${internalName}_win_full")
                        }

                        else -> {
                            player.sendMessage(CVTextColor.serverNotice + "+$delta ${accountType.pluralName} for playing")
                            MainRepositoryProvider.achievementProgressRepository
                                .increaseCounter(player.uniqueId, "minigame_${internalName}_lost")
                        }
                    }
                    val realHighestScore = players.count { it.metadata.score == highestScore } == 1
                    if (first && realHighestScore)
                        MainRepositoryProvider.achievementProgressRepository.increaseCounter(
                            player.uniqueId,
                            "minigame_${internalName}_win"
                        )
                    else if (first)
                        player.sendMessage(CVTextColor.serverNotice + "You didn't apply for a win as you played a tie")
                    MainRepositoryProvider.achievementProgressRepository
                        .increaseCounter(player.uniqueId, "minigame_${internalName}_play")
                }
            }
        } else {
            for (minigamePlayer in players) {
                val player = minigamePlayer.player
                Translation.MINIGAME_TOO_FEW_PLAYERS.getTranslation(player, displayName)?.sendTo(player)
            }
        }
    }

    override fun toJson(): Json = Json()

    class Json : BaseMinigame.Json() {
        override fun createGame(): Minigame {
            TODO("Not yet implemented")
        }
    }

    enum class SubState {
        INTRO, PLAY, FINALE
    }

    enum class BeerType(
        val displayName: String,
        val kind: BeerKind,
        private val sound: String,
        val location: Vector
    ) {
        JADNA_BOSNO("Jadna Bosno Suverena", BeerKind.TRIPEL, "jadnabosno_suverena", Vector()),
        PATER_WEISS("Pater Water", BeerKind.WEISS, "paterwater_weiss", Vector()),
        LEFFE("Leffe", BeerKind.TRIPEL, "leffe_tripel", Vector()),

        //
        PISSWASSER_BLONDE("Pißwasser", BeerKind.BLONDE, "pisswasser_blonde", Vector()),
        HOP_NAR_BLONDE("Hop Nar", BeerKind.BLONDE, "hopnar_blonde", Vector(1)),
        GON_BROUW_BROWN("Gon Brouw", BeerKind.BROWN, "gonbrouw_brown", Vector(1)),

        //
        FLUTWEISER_LAGER("Flutweiser", BeerKind.LAGER, "flutweisser_lager", Vector()),
        STRAFFE_HENDRIK("Straffe Hendrik", BeerKind.TRIPEL, "straffehendrik_tripel", Vector(120)),
        SCHIEDSRICHTER_BRAU(
            "Schiedsrichter Brau",
            BeerKind.WEISS,
            "schiedsrichterbrau_weiss",
            Vector(1)
        ),

        //
        BRUGSE_ZOT("Brugse Zot", BeerKind.BLONDE, "brugsezot_blonde", Vector()),
        RASPUTIN_KVASS("Rasputin Kvass", BeerKind.UNKNOWN, "rasputinkvass_unknown", Vector(1)),
        TSINGTAO("Tsingtao", BeerKind.LAGER, "tsingtao_lager", Vector(120)),

        //
        //
        DE_KLOK("De Klok", BeerKind.BEER, "klok_beer", Vector(1)),
        DRUREN_BROUW("Druren Brouw", BeerKind.BLONDE, "drurenbrouw_blonde", Vector(140)),
        BALTIKA("Baltika", BeerKind.DARGON, "baltika_dargon", Vector(1)),

        //
        GUINESS_STOUT("Guinness", BeerKind.STOUT, "guinness_stout", Vector(10)),
        OMACKAYS_LAGER("O'Mackay's", BeerKind.LAGER, "omackeys_lager", Vector(140)),
        ERDINGER_WEISS("Erdinger", BeerKind.WEISS, "erdinger_weiss", Vector(14)),

        //
        JOPEN_MOOIE_NEL("Jopen", BeerKind.MOOIE_NEL, "jopen_mooienel", Vector(14)),
        PALM_AMBER("Palm", BeerKind.AMBER, "palm_amber", Vector(10)),
        SCHULTENBRAU_LAGER("Schultenbrau", BeerKind.LAGER, "schultenbrau_lager", Vector(10)),

        //
        JONKHEER_COOKIE_BLONDE(
            "Jonkheer Cookie",
            BeerKind.BLONDE,
            "jonkheercookie_blonde",
            Vector()
        ),
        LA_TRAPPE_TRIPEL("La Trappe", BeerKind.TRIPEL, "latrappe_tripel", Vector(10)),
        LA_TRAPPE_QUADRUPEL("La Trappe", BeerKind.QUADRUPEL, "latrappe_quadrupel", Vector(140)),

        //
        LAMSBERGEN_TRIPEL("Van Lamsbergen", BeerKind.TRIPEL, "lamsbergen_tripel", Vector(10));

        fun getSoundName() = "${SoundUtils.SOUND_PREFIX}:minigame.beerbrawl.bring.$sound"
    }

    enum class BeerKind(
        val displayName: String,
        private val sound: String
    ) {
        UNKNOWN("Unknown", "unknown"),
        LAGER("Lager", "lager"),
        STOUT("Stout", "stout"),
        BEER("Beer", "beer"),
        WEISS("Weiss", "weiss"),
        TRIPEL("Tripel", "tripel"),
        BLONDE("Blonde", "blonde"),
        DARGON("Dargon", "dargon"),
        BROWN("Brown", "brown"),
        MOOIE_NEL("Mooie Nel", "mooienel"),
        AMBER("Amber", "amber"),
        QUADRUPEL("Quadrupel", "quadrupel");

        fun getSoundName() = "${SoundUtils.SOUND_PREFIX}:minigame.beerbrawl.kind.$sound"
    }

    companion object {
        val SOUND_INTRO = "${SoundUtils.SOUND_PREFIX}:minigame.beerbrawl.intro"
        val SOUND_FINALE = "${SoundUtils.SOUND_PREFIX}:minigame.beerbrawl.finale"
        val SOUND_WRONG_BEER = "${SoundUtils.SOUND_PREFIX}:minigame.beerbrawl.wrongbeer"
    }
}