package net.craftventure.core.feature.finalevent

import net.craftventure.bukkit.ktx.coroutine.executeSync
import net.craftventure.core.npc.EntityMetadata
import net.craftventure.core.npc.NpcEntity
import net.minecraft.world.entity.Pose
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.event.player.PlayerAnimationType
import org.bukkit.inventory.EquipmentSlot

class PlayerRecordingPlayback(
    val entity: NpcEntity,
    val recording: PlayerRecordingDto,
) {
    private var task: Int? = null

    var lastLocation: Location =
        recording.initialActions.filterIsInstance<PlayerRecordingActionDto.Location>().first().location

    fun start(startAtMillis: Long = 0) {
        if (task != null) return
        val start = System.currentTimeMillis() - startAtMillis
        var lastTime = 0L

        recording.initialActions.forEach { execute(it) }

        task = executeSync(0, 1) {
            val currentTime = System.currentTimeMillis() - start
//            logcat { "Execute $lastTime to ${currentTime}" }
            recording.actions.forEach {
                if (it.at in lastTime..<currentTime) {
                    execute(it.action)
                }
            }
            lastTime = currentTime
        }
    }

    fun stop() {
        task?.let { Bukkit.getScheduler().cancelTask(it) }
        task = null
    }

    private fun execute(action: PlayerRecordingActionDto) {
//        logcat { "Execute ${action.toJson()}" }
        when (action) {
            is PlayerRecordingActionDto.Location -> {
                entity.move(action.location)
                lastLocation = action.location
            }

            is PlayerRecordingActionDto.Pose -> entity.setMetadata(EntityMetadata.Entity.pose, action.pose.toNms())
            is PlayerRecordingActionDto.Item -> when (action.slot) {
                EquipmentSlot.HAND -> entity.held(action.item)
                EquipmentSlot.OFF_HAND -> entity.heldOffHand(action.item)
                EquipmentSlot.FEET -> entity.boots(action.item)
                EquipmentSlot.LEGS -> entity.leggings(action.item)
                EquipmentSlot.CHEST -> entity.chestplate(action.item)
                EquipmentSlot.HEAD -> entity.helmet(action.item)
            }

            is PlayerRecordingActionDto.PlayerAnimation -> when (action.animation) {
                PlayerAnimationType.ARM_SWING -> entity.swingMainArm()
                PlayerAnimationType.OFF_ARM_SWING -> entity.swingOffHand()
            }
        }
    }

    private fun org.bukkit.entity.Pose.toNms() = when (this) {
        org.bukkit.entity.Pose.STANDING -> Pose.STANDING
        org.bukkit.entity.Pose.FALL_FLYING -> Pose.FALL_FLYING
        org.bukkit.entity.Pose.SLEEPING -> Pose.SLEEPING
        org.bukkit.entity.Pose.SWIMMING -> Pose.SWIMMING
        org.bukkit.entity.Pose.SPIN_ATTACK -> Pose.SPIN_ATTACK
        org.bukkit.entity.Pose.SNEAKING -> Pose.CROUCHING
        org.bukkit.entity.Pose.LONG_JUMPING -> Pose.LONG_JUMPING
        org.bukkit.entity.Pose.DYING -> Pose.DYING
        org.bukkit.entity.Pose.CROAKING -> Pose.CROAKING
        org.bukkit.entity.Pose.USING_TONGUE -> Pose.USING_TONGUE
        org.bukkit.entity.Pose.SITTING -> Pose.SITTING
        org.bukkit.entity.Pose.ROARING -> Pose.ROARING
        org.bukkit.entity.Pose.SNIFFING -> Pose.SNIFFING
        org.bukkit.entity.Pose.EMERGING -> Pose.EMERGING
        org.bukkit.entity.Pose.DIGGING -> Pose.DIGGING
    }
}