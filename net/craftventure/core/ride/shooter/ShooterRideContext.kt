package net.craftventure.core.ride.shooter

import net.craftventure.bukkit.ktx.manager.MessageBarManager
import net.craftventure.bukkit.ktx.manager.MessageBarManager.Message
import net.craftventure.bukkit.ktx.manager.MessageBarManager.display
import net.craftventure.bukkit.ktx.util.ChatUtils
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.extension.markAsWornItem
import net.craftventure.core.ktx.util.TimeUtils
import net.craftventure.core.manager.EquipmentManager.EquippedItemData.Companion.toEquippedItemData
import net.craftventure.core.ride.shooter.config.ShooterConfig
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar
import net.craftventure.core.utils.ItemStackUtils
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import java.time.LocalDateTime
import java.util.*

class ShooterRideContext(
    val config: ShooterConfig
) {
    private var entityFactory = mapOf<String, ShooterEntityFactory>()
    private var particleFactory = mapOf<String, ShooterParticlePathFactory>()
    private val scenes by lazy {
        config.scenes.map { entry ->
            ShooterScene(entry.key, entry.value, this)
        }
    }
    private val sceneMap by lazy { scenes.associateBy { it.id } }
    private val teams = mutableSetOf<Team>()
    val gunItem by lazy {
        ItemStackUtils.fromString(config.gunItem)!!
            .markAsWornItem()
            .toEquippedItemData("shooter_ride_gun")
    }

    fun init() {
        scenes
        sceneMap
    }

    fun update() {
        teams.forEach { team ->
            val score = team.score
            team.players.forEach { player ->
                display(
                    player.player,
                    Message(
                        id = ChatUtils.ID_RIDE,
                        text = Component.text(
                            "Current score $score",
                            CVTextColor.serverNotice
                        ),
                        type = MessageBarManager.Type.RIDE,
                        untilMillis = TimeUtils.secondsFromNow(1.0),
                    ),
                    replace = true,
                )
            }
        }
    }

    fun destroy() {
        scenes.forEach {
            it.destroy()
        }
    }

    fun getEntityFactory(id: String) = entityFactory[id]

    fun setEntityFactories(factories: List<ShooterEntityFactory>) {
        entityFactory = factories.associateBy { it.id }
    }

    fun getParticleFactory(id: String) = particleFactory[id]

    fun setParticleFactories(factories: List<ShooterParticlePathFactory>) {
        particleFactory = factories.associateBy { it.id }
    }

    open class Team(
        val players: MutableSet<PlayerData>,
        val playerIds: Set<UUID> = players.map { it.player.uniqueId }.toSet(),
    ) {
        val shots get() = players.sumOf { it.shots }
        val hits get() = players.sumOf { it.hits }
        val score get() = players.sumOf { it.score }
        val firstKill get() = players.filter { it.firstKill != null }.minOf { it.firstKill!! }
        val hitRatio get() = shots.toDouble() / hits.toDouble()

        fun dataFor(player: Player) = players.firstOrNull { it.player === player }

        operator fun contains(player: Player) = players.any { it.player === player }

        override fun toString(): String {
            return "Team(players=$players, shots=$shots, hits=$hits, score=$score, firstKill=$firstKill, hitRatio=$hitRatio)"
        }

        fun remove(player: Player) {
            if (players.removeIf { it.player == player }) {
                MessageBarManager.remove(player, ChatUtils.ID_RIDE)
            }
        }

        data class PlayerData(
            val player: Player,
        ) {
            var shots: Int = 0
                private set
            var hits: Int = 0
                private set
            var score: Int = 0
                private set
            var firstKill: LocalDateTime? = null
                private set
            val hitRatio get() = shots.toDouble() / hits.toDouble()

            fun shot() {
                shots++
            }

            fun hit(score: Int) {
                hits++
                this.score += score
                if (firstKill == null)
                    firstKill = LocalDateTime.now()
            }
        }
    }

    fun startScene(id: String, car: TracklessRideCar) {
//        logcat(logToCrew = PluginProvider.isNonProductionServer()) {
//            "Playing scene $id to ${car.team}? ${sceneMap[id] != null}"
//        }
        val scene = sceneMap[id] ?: return
        scene.startFor(setOf(car))
    }

    fun startScene(id: String, cars: Set<TracklessRideCar>) {
//        logcat(logToCrew = PluginProvider.isNonProductionServer()) {
//            "Playing scene $id to ${car.team}? ${sceneMap[id] != null}"
//        }
        val scene = sceneMap[id] ?: return
        scene.startFor(cars)
    }

    fun stopScene(id: String) {
//        logcat(logToCrew = PluginProvider.isNonProductionServer()) {
//            "Stopping scene $id? ${sceneMap[id] != null}"
//        }
        val scene = sceneMap[id] ?: return
        scene.stop()
    }

    fun createTeam(players: Set<Player>): Team {
//        Logger.debug("Creating team with $players")
        val team = Team(players.map { Team.PlayerData(it) }.toMutableSet())
        teams.add(team)
        return team
    }

    fun removeTeam(team: Team) {
//        Logger.debug("Removing team $team")
        teams.remove(team)

        team.players.forEach { player ->
            MessageBarManager.remove(player.player, ChatUtils.ID_RIDE)
        }
    }
}