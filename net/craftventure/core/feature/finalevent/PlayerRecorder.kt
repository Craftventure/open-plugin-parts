package net.craftventure.core.feature.finalevent

import net.craftventure.bukkit.ktx.extension.takeIfNotAir
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.core.async.executeSync
import net.craftventure.core.ktx.json.CvMoshi
import net.craftventure.extension.encodeAsBase64ToString
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerAnimationEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.io.File

class PlayerRecorder(
    private val player: Player,
) : Listener {
    private val initialActions = mutableListOf<PlayerRecordingActionDto>()
    private val actions = mutableListOf<TimedActionDto>()
    private var recordingTask: Int? = null

    private var startAt = 0L

    private val equippedItems = hashMapOf<EquipmentSlot, ItemStack>()

    fun start() {
        var lastLocation = player.location
        var lastPose = player.pose
        initialActions.add(PlayerRecordingActionDto.Location(lastLocation))
        initialActions.add(PlayerRecordingActionDto.Pose(lastPose))

        EquipmentSlot.entries.forEach { slot ->
            val item = player.equipment.getItem(slot)
            equippedItems[slot] = item
            initialActions.add(
                PlayerRecordingActionDto.Item(
                    slot,
                    item.takeIfNotAir()?.serializeAsBytes()?.encodeAsBase64ToString()
                )
            )
        }

        Bukkit.getServer().pluginManager.registerEvents(this, PluginProvider.getInstance())

        startAt = System.currentTimeMillis()
        recordingTask = executeSync(1, 1) {
            val now = getCurrentNow()

            if (player.location != lastLocation) {
                lastLocation = player.location
                actions.add(TimedActionDto(now, PlayerRecordingActionDto.Location(lastLocation)))
            }

            if (player.pose != lastPose) {
                lastPose = player.pose
                actions.add(TimedActionDto(now, PlayerRecordingActionDto.Pose(lastPose)))
            }

            EquipmentSlot.entries.forEach { slot ->
                val item = player.equipment.getItem(slot)
                if (equippedItems[slot] != item) {
                    actions.add(
                        TimedActionDto(
                            now, PlayerRecordingActionDto.Item(
                                slot,
                                item.takeIfNotAir()?.serializeAsBytes()?.encodeAsBase64ToString()
                            )
                        )
                    )
                }
            }
        }
    }

    private fun getCurrentNow() = System.currentTimeMillis() - startAt

    @EventHandler(priority = EventPriority.MONITOR)
    fun onAnimation(event: PlayerAnimationEvent) {
        if (event.isCancelled) return
        if (event.player !== player) return
        actions.add(TimedActionDto(getCurrentNow(), PlayerRecordingActionDto.PlayerAnimation(event.animationType)))
    }

    fun stop() {
        recordingTask?.let { Bukkit.getScheduler().cancelTask(it) }
        recordingTask = null
        HandlerList.unregisterAll(this)
    }

    fun save(file: File) {
        val data = PlayerRecordingDto(initialActions, actions)
        file.writeText(CvMoshi.adapter(PlayerRecordingDto::class.java).toJson(data))
    }
}