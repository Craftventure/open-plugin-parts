package net.craftventure.core.animation

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.area.Area
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.core.extension.spawn
import net.craftventure.core.ktx.extension.clampDegrees
import net.craftventure.core.ktx.util.AngleUtils
import net.craftventure.core.utils.EntityUtils
import net.craftventure.core.utils.ItemStackUtils
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import penner.easing.Easing
import java.time.Duration

class ModeledDoor(
    val config: ModeledDoorConfig,
) : Door {
    private var task: BukkitRunnable? = null
    private var state: State = if (config.initiallyOpen) State.Open else State.Closed
        private set(value) {
            field = value
//            logcat { "new state $value" }
        }

    private val entities = config.hinges.map { hinge ->
        HingeData(
            hinge,
            hinge.location.spawn<ArmorStand>().apply {
                isVisible = false
                equipment.helmet = ItemStackUtils.fromString(hinge.item)
                EntityUtils.entityYaw(this, hinge.clampedClosedAngle.toFloat())
            },
        )
    }

    private val animationQueue = ArrayDeque<Animation>()
    private var lastAnimationStart = 0L
    private var currentAnimation: Animation? = null
        private set(value) {
            if (field == value) return
//            logcat { "Setting animation to $value" }
            if (value == null) {
                state = if (field?.open == true) State.Open else State.Closed
            } else {
                state = if (field?.open == true) State.Opening else State.Closing
            }
            field = value
            lastAnimationStart = System.currentTimeMillis()
        }

    fun destroy() {
        cancelUpdateTask()
        entities.forEach { it.entity.remove() }
    }

    private fun scheduleUpdateTask() {
        if (task != null) return
//        logcat { "Scheduling task" }
        task = object : BukkitRunnable() {
            override fun run() {
                doUpdateTask()
            }
        }
        task?.runTaskTimer(PluginProvider.getInstance(), 1, 1)
    }

    private fun cancelUpdateTask() {
//        logcat { "Cancel task" }
        task?.cancel()
        task = null
    }

    private fun doUpdateTask() {
//        logcat { "Update" }
        if (currentAnimation == null) {
            currentAnimation = animationQueue.removeFirstOrNull()
        }
        val currentAnimation = currentAnimation
        if (currentAnimation == null) {
//            logcat { "No animation, ending update task" }
            cancelUpdateTask()
            return
        }

        applyCurrentAnimation()
    }

    private fun applyCurrentAnimation() {
        val animation = currentAnimation ?: return
        val now = System.currentTimeMillis()
        val t = ((now - lastAnimationStart.toDouble()) / animation.millis).coerceIn(0.0, 1.0)

//        logcat { "t=${t.format(2)}" }

        entities.forEach { data ->
            val transformedT = data.hinge.easingParsed.easeInOut(t, 0.0, 1.0, 1.0)
            val location = data.hinge.location
            val offset = data.hinge.openedOffset
            val actualT = if (animation.open) transformedT else 1 - transformedT

            if (!data.entity.isValid) {
                data.entity = location.spawn<ArmorStand>()
            }

            val distance = AngleUtils.distance(data.hinge.clampedClosedAngle, data.hinge.clampedOpenAngle)
            EntityUtils.move(
                data.entity,
                location.x + (offset.x * actualT),
                location.y + (offset.y * actualT),
                location.z + (offset.z * actualT),
                (data.hinge.clampedClosedAngle + (distance * actualT)).toFloat(),
                0f,
            )
        }

        if (t >= 1.0) {
            finishCurrentAnimation()
        }
    }

    private fun finishCurrentAnimation() {
        currentAnimation = null
    }

    private fun startAnimating(open: Boolean, millis: Long) {
//        logcat { "startAnimating? queue=${animationQueue.lastOrNull()?.open} current=${currentAnimation?.open} state=$state open=$open" }
        val targetOpen = animationQueue.lastOrNull()?.open ?: currentAnimation?.open
        if (targetOpen != null && targetOpen != open) {
//            logcat { "Adding animation A state=$state open=$open millis=$millis" }
            animationQueue.add(Animation(millis, open))
        } else if ((state == State.Open && !open) || (state == State.Closed && open)) {
//            logcat { "Adding animation B state=$state open=$open millis=$millis" }
            animationQueue.add(Animation(millis, open))
        }
        scheduleUpdateTask()
    }

    override fun open(duration: Duration) {
        startAnimating(true, duration.toMillis())
    }

    override fun close(duration: Duration) {
        startAnimating(false, duration.toMillis())
    }

    override fun open(ticks: Int) {
        startAnimating(true, ticks * 50L)
    }

    override fun close(ticks: Int) {
        startAnimating(false, ticks * 50L)
    }

    private enum class State {
        Closed,
        Opening,
        Open,
        Closing;
    }

    enum class HingeDirection {
        Clockwise,
        CounterClockwise,
        ShortestAngle,
    }

    @JsonClass(generateAdapter = true)
    data class ModeledDoorConfig(
        val initiallyOpen: Boolean = false,
        val hinges: List<Hinge>,
        val visibilityArea: List<Area.Json>? = null,
    )

    @JsonClass(generateAdapter = true)
    data class Hinge(
        val location: Location,
        val openAngle: Double,
        val closedAngle: Double,
        val direction: HingeDirection = HingeDirection.ShortestAngle,
        val easing: String,
        val item: String,
        val openedOffset: Vector = Vector(),
    ) {
        val easingParsed = Easing.byId(easing)!!

        val clampedOpenAngle = openAngle.clampDegrees()
        val clampedClosedAngle = closedAngle.clampDegrees()
    }

    private data class Animation(
        val millis: Long,
        val open: Boolean,
    )

    private data class HingeData(
        val hinge: Hinge,
        var entity: Entity,
    )
}