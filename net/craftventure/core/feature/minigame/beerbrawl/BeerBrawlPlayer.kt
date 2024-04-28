package net.craftventure.core.feature.minigame.beerbrawl

import net.craftventure.bukkit.ktx.extension.add
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.extension.increaseAchievementCounter
import net.craftventure.core.feature.minigame.BaseMinigame
import net.craftventure.core.ktx.extension.nextDoubleRange
import net.craftventure.core.ktx.extension.random
import net.craftventure.core.manager.EquipmentManager
import net.craftventure.core.ride.PukeEffect
import net.craftventure.core.utils.TitleUtil.sendTitleWithTicks
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarFlag
import org.bukkit.boss.BarStyle
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*

class BeerBrawlPlayer(
    val player: Player
) {
    private val bossBar = Bukkit.createBossBar(
        "",
        BarColor.YELLOW,
        BarStyle.SOLID,
        BarFlag.DARKEN_SKY,
        BarFlag.CREATE_FOG
    )
    var beerType: BeerBrawl.BeerType = BeerBrawl.BeerType.values().random()!!
    var hasFinishedThisRound = false
    var score: Int = 0
    private var drunkRound = 0
    private var drunkTick = 0
    private var currentSubStateCounter = BaseMinigame.Counter()
    private var isSearching = false
    private var state = FindState.HINT
        set(value) {
            if (field != value) {
                field = value
                onSubStateChanged(value)
            }
        }
    private var lastToiletTime: Long? = null

    var beer: BeerBrawl.BeerType? = null
        set(value) {
            if (field != value) {
                field = value

                if (value != null) {
                    player.sendTitleWithTicks(
                        0, 20 * 3,
                        titleColor = NamedTextColor.YELLOW,
                        subtitleColor = NamedTextColor.YELLOW,
                        title = "${value.displayName} bottled",
                        subtitle = "Deliver this beer to the table"
                    )
                } else {
                    player.sendTitleWithTicks(
                        0, 20 * 3, subtitleColor = NamedTextColor.RED,
                        subtitle = "Your bottle was emptied"
                    )
                }

                EquipmentManager.reapply(player)
            }
        }

    private fun setNewBeerType() {
        beerType = BeerBrawl.BeerType.values().random()!!
    }

    fun enterToilet() {
        val now = System.currentTimeMillis()
        val shouldPlay = lastToiletTime == null || lastToiletTime!! < now - 60_000
        if (!shouldPlay) return
        lastToiletTime = now

        PukeEffect.play(player, offset = 20 * 4, duration = 20 * 2)
        player.addPotionEffect(PotionEffect(PotionEffectType.SLOW, 20 * 5, 100, false, false))
        drunkRound = 0
        executeAsync {
            player.increaseAchievementCounter("minigame_beerbrawl_toilet")
        }
    }

    fun startFind() {
        currentSubStateCounter.reset()
        setNewBeerType()
        isSearching = true
        bossBar.addPlayer(player)
    }

    fun stopFind() {
        isSearching = true
        bossBar.removePlayer(player)
    }

    fun update(game: BaseMinigame<BeerBrawlPlayer>, minigameLevel: BeerBrawlLevel) {
        val range = currentSubStateCounter.range()
        val previousUpdateTime = range.start
        val updateTime = range.endInclusive - 1

        if (isSearching) {
            drunkTick++
            when (state) {
                FindState.HINT -> {
                    if (0 in previousUpdateTime..updateTime) {
                        player.playSound(minigameLevel.beerLocation, beerType.kind.getSoundName(), 3f, 1f)
                        player.sendMessage(CVTextColor.serverNotice + "Ah yes... the next beer... what about... a ${beerType.kind.displayName}...")
                    }
                    if (6000 in previousUpdateTime..updateTime) {
                        bossBar.setTitle("§6Retrieve a ${beerType.displayName} (${beerType.kind.displayName}) and bring it to the man")
                        player.playSound(minigameLevel.beerLocation, beerType.getSoundName(), 3f, 1f)
                        player.sendMessage(CVTextColor.serverNotice + "Bring me a ${beerType.displayName} (${beerType.kind.displayName})")
                    }

                    if (updateTime > 6500) {
                        state = FindState.FIND
                    }
                    applyDrunkTicks()
                }

                FindState.FIND -> {
                    if (!hasFinishedThisRound) {
                        if (player.location.distanceSquared(minigameLevel.beerLocation) < 3 * 3) {
                            if (beer == beerType) {
                                val achievementBeerType = beerType
                                executeAsync {
                                    player.increaseAchievementCounter("minigame_${game.internalName}_beer_deliver")
                                    player.increaseAchievementCounter(
                                        "minigame_beerbrawl_beer_${
                                            achievementBeerType.name.lowercase(
                                                Locale.getDefault()
                                            )
                                        }"
                                    )
                                }
                                finishRound(1)
                            } else if (beer != null) {
                                beer = null
                                executeAsync {
                                    player.increaseAchievementCounter("minigame_beerbrawl_wrong_beer")
                                }
                                player.sendTitleWithTicks(
                                    stay = 20 * 3,
                                    titleColor = NamedTextColor.RED,
                                    title = "That's the wrong beer!",
                                    subtitleColor = NamedTextColor.RED,
                                    subtitle = "I need ${beerType.displayName} (${beerType.kind.displayName})"
                                )
                                player.player?.playSound(minigameLevel.beerLocation, BeerBrawl.SOUND_WRONG_BEER, 3f, 1f)
                            }
                        }
                    }
                    applyDrunkTicks()
                }
            }
        }
    }

    fun cleanup() {
        bossBar.removeAll()
    }

    private fun onSubStateChanged(newSubState: FindState) {
        bossBar.progress = 1.0
        bossBar.color = BarColor.YELLOW
        currentSubStateCounter.reset()

        when (newSubState) {
            FindState.HINT -> {
                bossBar.setTitle("§6Wait for the next request...")
                setNewBeerType()
                drunkRound++

                resetRound()
            }

            FindState.FIND -> {
            }
        }
    }

    private fun applyDrunkTicks() {
        if (drunkRound >= 3) {
            if (drunkTick % 5 == 0) {
                val random = CraftventureCore.getRandom()
                player.velocity = player.velocity.clone()
                    .add(random.nextDoubleRange(-0.15, 0.15), 0.0, random.nextDoubleRange(-0.15, 0.15))
            }
            if (drunkTick > 150) {
                if (!player.hasPotionEffect(PotionEffectType.CONFUSION)) {
                    player.addPotionEffect(PotionEffect(PotionEffectType.CONFUSION, 150, 1, true, false))
                }
            }
        }
        if (drunkTick > 150) {
            drunkTick = 0
        }
    }

    fun finishRound(score: Int) {
        this.score += score
        beer = null
        hasFinishedThisRound = true
        player.sendTitleWithTicks(
            0,
            20 * 3,
            subtitleColor = NamedTextColor.YELLOW,
            subtitle = "Drink delivered, +$score score"
        )
        state = FindState.HINT
        player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 20 * 4, 1, true, false))
    }

    fun resetRound() {
        hasFinishedThisRound = false
//        beer = null
    }

    enum class FindState {
        HINT, FIND
    }
}