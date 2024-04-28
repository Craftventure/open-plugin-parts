package net.craftventure.core.script.armature

import net.craftventure.bukkit.ktx.util.EntityConstants
import net.craftventure.core.ktx.util.BackgroundService
import net.craftventure.core.animation.armature.ArmatureAnimator
import net.craftventure.core.animation.armature.Joint
import net.craftventure.core.animation.dae.DaeLoader
import net.craftventure.core.ktx.json.CvMoshi
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.ride.RotationFixer
import net.craftventure.core.script.Script
import net.craftventure.core.script.ScriptControllerException
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemStack
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.max

class ArmatureScript(
    private val scriptFile: File,
    private val groupId: String,
    private val name: String
) : Script(), BackgroundService.Animatable {
    override var repeats: Boolean = false
    private var hasLoaded = false
    private var hasStarted = false

    private var startTime: Long = -1
    private var lastTime: Long = -1
    private var animationTimeMillis: Long = 0

    private var spawnedEntities = arrayOfNulls<JointData>(0)

    private var config: ArmatureConfig? = null
    private var armatures: Array<ArmatureAnimator> = emptyArray()

    private val center = Location(Bukkit.getWorld("world"), 0.0, 0.0, 0.0)

    override val isValid: Boolean
        get() = config != null

    override fun onLoad() {
        super.onLoad()
        try {
            val config =
                CvMoshi.adapter(ArmatureConfig::class.java).fromJson(scriptFile.readText())!!
            this.config = config
//            Logger.debug(this.config.toString())

            val daeFile = File(scriptFile.parentFile, config.animation)
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(daeFile)
            val armatures = DaeLoader.load(doc, "$groupId/$name @${daeFile.name}")
//            Logger.debug("Loaded ${armatures.size} armatures")
            this.armatures = armatures.map { ArmatureAnimator(it) }.toTypedArray()

            val entityCount = armatures.sumOf { armature ->
                armature.joints.flatMap { it.childrenRecursive() + it }.filter { !it.name.startsWith("_") }.size
            }
//            Logger.debug("Entitycount = $entityCount")
            this.spawnedEntities = arrayOfNulls(entityCount)
            var animationLengthSeconds = 0.0
            armatures.forEach { armature ->
                animationLengthSeconds = max(
                    animationLengthSeconds,
                    armature.joints.maxByOrNull { determineAnimationDuration(it) }?.animation?.lastOrNull()?.time ?: 0.0
                )
            }
//            Logger.debug("Loaded DAE ${animationLengthSeconds.format(2)}s")
            this.animationTimeMillis = (animationLengthSeconds * 1000).toLong()
        } catch (e: Exception) {
            e.printStackTrace()
            throw ScriptControllerException("Failed to load the armature script " + scriptFile.path, e)
        }

        hasLoaded = true
    }

    private fun determineAnimationDuration(joint: Joint): Double {
        val childMax =
            joint.childJoints.maxByOrNull { determineAnimationDuration(it) }?.animation?.lastOrNull()?.time ?: 0.0
        val max = joint.animation.maxByOrNull { it.time }?.time ?: 0.0
        return max(childMax, max)
    }

    override fun onStart() {
        super.onStart()
        if (!hasLoaded) {
            scriptController?.stop()
            return
        }

        startTime = System.currentTimeMillis()
        lastTime = startTime

        BackgroundService.remove(this)
        BackgroundService.add(this)

        hasStarted = true
    }

    override fun onStop() {
        super.onStop()
        BackgroundService.remove(this)
        hasStarted = false

        spawnedEntities.filterNotNull().forEach {
            scriptController?.npcEntityTracker?.removeEntity(it.npcEntity)
        }
        spawnedEntities = arrayOfNulls(spawnedEntities.size)
    }

    override fun onAnimationUpdate() {
        val now = System.currentTimeMillis()
//        val previousTime = (lastTime - startTime) / 1000.0
        val time = (now - startTime) / 1000.0

        if (time > animationTimeMillis.toDouble() / 1000.0) {
//            Logger.debug("Finished")
            onStop()
            return
        }
//        Logger.debug("Animate ${time.format(2)}")

        var index = 0
        armatures.forEach { animator ->
            val allJoints = animator.allJoints

            val final = Vector3()

            animator.setTime(time)
            allJoints.forEach jointLoop@{ joint ->
                if (joint.name.startsWith("_")) return@jointLoop
                joint.animatedTransform.transformPoint(final.reset())
//                            drawLine(startLocation, joint.animatedTransform, final, Color.RED)
                val entity = requireEntity(index++, final, joint)
                val data = spawnedEntities[index - 1]!!
                entity.move(final.x, final.y - EntityConstants.ArmorStandHeadOffset, final.z)
//                Logger.debug("Moving entity $index to ${final.x.format(2)} ${final.y.format(2)} ${final.z.format(2)} ")

//                val headPose = TransformUtils.getArmorStandPose(joint.animatedTransform.rotation)
                data.rotationFixer.setNextRotation(joint.animatedTransform.rotation)
                val rotation = data.rotationFixer.getCurrentRotation()
                entity.head(rotation.x.toFloat(), rotation.y.toFloat(), rotation.z.toFloat())
            }
        }
    }

    private fun requireEntity(
        index: Int,
        location: Vector3,
        joint: Joint
    ): NpcEntity {
        val data = spawnedEntities[index]
        var armorStand = data?.npcEntity
        if (armorStand != null) return armorStand
//        Logger.debug("Spawning armorstand for joint ${joint.name}")
//        Logger.debug("Spawning armorstand for joint ${joint.name} at ${joint.transform} ${joint.animatedTransform}")
        armorStand = NpcEntity(
            "armature",
            EntityType.ARMOR_STAND,
            Location(center.world, location.x, location.y - EntityConstants.ArmorStandHeadOffset, location.z)
        )
        scriptController?.npcEntityTracker?.addEntity(armorStand)
        spawnedEntities[index] = JointData(armorStand)
        armorStand.noGravity(true)
        armorStand.invisible(true)
        armorStand.helmet(getModel(joint))

        return armorStand
    }

    private fun getModel(joint: Joint): ItemStack {
        return config?.getModel(jointName = joint.name)?.modelStack ?: ItemStack(Material.AIR)
    }

    data class JointData(
        val npcEntity: NpcEntity,
        val rotationFixer: RotationFixer = RotationFixer()
    )
}