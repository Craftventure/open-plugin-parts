package net.craftventure.core.map.holder

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.core.CraftventureCore
import net.craftventure.core.map.renderer.MapManager
import net.craftventure.core.ride.shooter.ShooterRideContext
import net.craftventure.core.ride.tracklessride.programpart.InvalidateImageHolderProgramPart
import net.craftventure.core.ride.tracklessride.transport.TracklessRideCarGroup
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.File
import java.net.URL
import javax.imageio.ImageIO
import kotlin.math.roundToInt

class MatScoreImageHolder(
    override val id: String,
    private val teamId: Int,
) : MapManager.ImageHolder, InvalidateImageHolderProgramPart.TrainImageHolder {
    override var lastUpdate: Long = System.currentTimeMillis()
    private val main = BufferedImage(128, 256, BufferedImage.TYPE_4BYTE_ABGR)
    private var rendered = false
    private var lock = Any()
    private val retrieveSemaphore = Semaphore(1)
    override suspend fun retrieveRenderedImage(): BufferedImage {
        retrieveSemaphore.withPermit {
            if (background == null)
                background = loadBackground()
            if (backgroundRecruit == null)
                backgroundRecruit = loadBackgroundRecruit()
            if (!rendered)
                runBlocking { render() }
        }
        return main
    }

    private var background: MapManager.ImageHolder? = null
    private var backgroundRecruit: MapManager.ImageHolder? = null
    private val g2d = main.createGraphics()
    private val forwardFont = Font.createFont(
        Font.TRUETYPE_FONT,
        File(PluginProvider.getInstance().dataFolder, "data/font/fff_forward.ttf"),
    ).deriveFont(Font.PLAIN, 22f)
    private val freedomFont = Font.createFont(
        Font.TRUETYPE_FONT,
        File(PluginProvider.getInstance().dataFolder, "data/font/fff_freedom.ttf"),
    ).deriveFont(Font.PLAIN, 12f)

    private suspend fun loadBackground() = MapManager.instance.getOrCreateImageHolder(
        File(
            CraftventureCore.getInstance().dataFolder,
            "data/maps/highscore/mat_background.png"
        )
    )

    private suspend fun loadBackgroundRecruit() = MapManager.instance.getOrCreateImageHolder(
        File(
            CraftventureCore.getInstance().dataFolder,
            "data/maps/highscore/mat_background_recruit.png"
        )
    )

    override var group: TracklessRideCarGroup? = null

    override fun invalidateSources() {
        background = null
        lastUpdate = System.currentTimeMillis()
    }

    override fun invalidateRender() {
        rendered = false
        lastUpdate = System.currentTimeMillis()
        g2d.clearRect(0, 0, main.width, main.height)
    }

    private suspend fun render() {
        val group = this.group
        val cars = group?.cars
        val team = cars?.getOrNull(teamId)?.team /*?: ShooterRideContext.Team(
            mutableSetOf(
                ShooterRideContext.Team.PlayerData(Bukkit.getOnlinePlayers().first()).apply {
                    hit(10)
                }
            ),
            setOf(Bukkit.getOnlinePlayers().first().uniqueId),
        )*/
        if (team == null || team.playerIds.isEmpty()) {
            val background = backgroundRecruit?.retrieveRenderedImage()
            if (background != null)
                g2d.drawImage(background, 0, 0, background.width, background.height, null)
            else {
                g2d.background = Color(0, 0, 0, 0)
                g2d.clearRect(0, 0, main.width, main.height)
            }
            return
        }
        g2d.background = Color(0, 0, 0, 0)
        g2d.clearRect(0, 0, main.width, main.height)
        val background = background?.retrieveRenderedImage()
        if (background != null)
            g2d.drawImage(background, 0, 0, background.width, background.height, null)

        val otherTeams = cars.filterIndexed { index, tracklessRideCar -> index != teamId }.mapNotNull { it.team } /*?: listOf(
                ShooterRideContext.Team(
                    mutableSetOf(
                        ShooterRideContext.Team.PlayerData(Bukkit.getOnlinePlayers().first()).apply {
                            hit(5)
                        }
                    ),
                    setOf(Bukkit.getOnlinePlayers().first().uniqueId),
                )
            )*/

        val avatar = team.playerIds.firstOrNull()?.let {
            withTimeout(5000) {
                kotlin.runCatching {
                    ImageIO.read(URL("https://crafatar.com/avatars/${it.toString().replace("-", "")}?size=70&overlay"))
                }
            }
        }

        avatar?.getOrNull()?.let { image ->
            g2d.drawImage(image, 29, 72, image.width, image.height, null)
        }

        val scoreScore = team.score.toString()

        g2d.paint = Color.decode("#191919")

        g2d.font = forwardFont
        g2d.drawString(scoreScore, 64 - (g2d.fontMetrics.stringWidth(scoreScore) * 0.5).roundToInt(), 186)

        val validTitles = titles.filter { it.predicate != null && it.predicate.invoke(team, otherTeams) }
        val highestPrio = validTitles.maxOfOrNull { it.priority }
        val title = validTitles.filter { it.priority == highestPrio }.randomOrNull()
            ?: titles.filter { it.predicate == null }.random()
        var y = 230 - (title.title.size * 0.5 * 14).toInt()


        g2d.paint = Color.decode("#ad4339")
        g2d.font = freedomFont
        title.title.forEach {
            g2d.drawString(it, 64 - (g2d.fontMetrics.stringWidth(it) * 0.5).roundToInt(), y)
            y += 14
        }

//        if (totalScore > 0) {
//            val name = "Team score of $totalScore"
//            g2d.font = rockwellFont.deriveFont(Font.PLAIN, 30f)
//            g2d.drawString(name, 320 - (g2d.fontMetrics.stringWidth(name) * 0.5).roundToInt(), 220)
//        }
//        Logger.debug("Updating score holder")
        lastUpdate = System.currentTimeMillis()
        rendered = true
    }

    fun finalize() {
        g2d.dispose()
    }

    data class MatScoreTitle(
        val title: List<String>,
        val priority: Int = 1,
        val predicate: ((team: ShooterRideContext.Team, otherTeams: List<ShooterRideContext.Team>) -> Boolean)? = null,
    )

    companion object {
        private val titles = listOf(
            MatScoreTitle(listOf("Clumsy Baker"), priority = Int.MAX_VALUE) { team, otherTeams ->
                val value = otherTeams.maxOfOrNull { it.hitRatio } ?: return@MatScoreTitle false
                return@MatScoreTitle value > team.hitRatio
            },
            MatScoreTitle(listOf("Glazed Glory"), priority = Int.MAX_VALUE) { team, otherTeams ->
                val value = otherTeams.maxOfOrNull { it.hitRatio } ?: return@MatScoreTitle false
                return@MatScoreTitle value < team.hitRatio
            },
            MatScoreTitle(listOf("Mice Mincer"), priority = Int.MAX_VALUE) { team, otherTeams ->
                val value = otherTeams.maxOfOrNull { it.hits } ?: return@MatScoreTitle false
                return@MatScoreTitle value < team.hits
            },
            MatScoreTitle(listOf("Secret", "Infiltrant"), priority = Int.MAX_VALUE) { team, otherTeams ->
                val value = otherTeams.maxOfOrNull { it.hits } ?: return@MatScoreTitle false
                return@MatScoreTitle value > team.hits
            },
            MatScoreTitle(listOf("Most", "Valuable", "Exterminator")) { team, otherTeams ->
                return@MatScoreTitle (otherTeams.maxOfOrNull { it.score } ?: Int.MAX_VALUE) < team.score
            },
            MatScoreTitle(listOf("Rat Cannon"), priority = Int.MAX_VALUE) { team, otherTeams ->
                val value = otherTeams.maxOfOrNull { it.shots } ?: return@MatScoreTitle false
                return@MatScoreTitle value < team.shots
            },
            MatScoreTitle(listOf("Rodent", "Exterminator"), priority = 2) { team, otherTeams -> team.hits > 20 },
            MatScoreTitle(listOf("Pest Control")),
        )
    }
}