package net.craftventure.core.feature.casino

import net.craftventure.bukkit.ktx.MaterialConfig
import net.craftventure.bukkit.ktx.manager.MessageBarManager
import net.craftventure.bukkit.ktx.manager.MessageBarManager.Message
import net.craftventure.bukkit.ktx.manager.MessageBarManager.display
import net.craftventure.bukkit.ktx.util.ChatUtils
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.ktx.extension.clamp
import net.craftventure.core.ktx.extension.format
import net.craftventure.core.ktx.util.TimeUtils
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.npc.tracker.NpcAreaTracker
import net.craftventure.core.utils.TimeSpan
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import penner.easing.Quad
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class RandomWheelOfFortune(
    location: Location,
    areaTracker: NpcAreaTracker,
    val countdown: TimeSpan,
    val spinTime: TimeSpan,
    val listener: WheelListener
) {
    private val playerLock = ReentrantLock()
    private var players = HashSet<Player>()
    private var updateTask: Int = 0
    private var start = 0L
    private var needle = NpcEntity("wof", EntityType.ARMOR_STAND, location.clone().add(0.0, -1.45, 0.0)).apply {
        invisible(true)
        helmet(MaterialConfig.CASINO_ARROW)
        head(0f, 0f, 0f)
    }
    private var target: Double = 0.0

    init {
        areaTracker.addEntity(needle)
    }

    fun togglePlayer(player: Player) {
        playerLock.withLock {
            if (contains(player))
                removePlayer(player)
            else
                addPlayer(player)
        }
    }

    fun addPlayer(player: Player): Boolean {
        playerLock.withLock {
            if (!isActuallySpinning()) {
                players.add(player)
                onPlayersChanged()
                return true
            }
            return false
        }
    }

    fun removePlayer(player: Player): Boolean {
        playerLock.withLock {
            if (!isActuallySpinning()) {
                players.remove(player)
                onPlayersChanged()
                return true
            }
            return false
        }
    }

    fun isSpinning() = updateTask != 0

    fun contains(player: Player): Boolean = players.remove(player)

    private fun finish() {
        playerLock.withLock {
            listener.onFinished(players, target)
            if (updateTask != 0)
                Bukkit.getScheduler().cancelTask(updateTask)
            players.clear()
            updateTask = 0
//            Logger.info("Stopping")
        }
    }

    private fun onPlayersChanged() {
        val hasPlayers = players.isNotEmpty()
        if (hasPlayers) {
            if (updateTask != 0) {
                return
            }
//            Logger.info("Starting")
            start = System.currentTimeMillis()
            target = CraftventureCore.getRandom().nextDouble() * 360.0
            updateTask =
                Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), ::update, 1L, 1L)
        } else {
//            Logger.info("Stopping")
            if (updateTask != 0)
                Bukkit.getScheduler().cancelTask(updateTask)
            updateTask = 0
        }
    }

    fun isActuallySpinning(): Boolean {
        if (updateTask == 0) return false
        val time = System.currentTimeMillis() - start
        return time > countdown.toMillis()
    }

    private fun update() {
        val time = System.currentTimeMillis() - start
        if (time < countdown.toMillis()) {
            val countdownLeft = countdown.toMillis().toDouble() - time.toDouble()
            for (player in players)
                display(
        player,
        Message(
            id = ChatUtils.ID_CASINO,
            text = Component.text(
                        "Spinning in ${
                            (countdownLeft / 1000.0).format(
                                1
                            )
                        } seconds...",
                        CVTextColor.serverNotice
                    ),
            type = MessageBarManager.Type.CASINO,
            untilMillis = TimeUtils.secondsFromNow(1.0),
        ),
        replace = true,
    )
        } else if (time < countdown.toMillis() + spinTime.toMillis()) {
            val rotationPercentage = Quad.easeOut(
                ((time - countdown.toMillis()) / spinTime.toMillis().toDouble()).clamp(0.0, 1.0),
                0.0,
                1.0,
                1.0
            )
            val headRotation = ((360 * 4) + target) * rotationPercentage

//            Logger.info("${rotationPercentage.format(2)} ${headRotation.format(2)}")
            needle.head(0f, 0f, (headRotation).toFloat())
        } else {
            finish()
            needle.head(0f, 0f, (target).toFloat())
        }
    }

    interface WheelListener {
        fun onFinished(players: Set<Player>, resultInDegree: Double)
    }
}