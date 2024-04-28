package net.craftventure.core.extension

import com.comphenix.packetwrapper.WrapperPlayServerPosition
import me.leoko.advancedban.manager.PunishmentManager
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.inventory.InventoryMenu
import net.craftventure.core.ktx.extension.clamp
import net.craftventure.core.metadata.CvMetadata
import net.craftventure.core.metadata.EquippedItemsMeta.Companion.equippedItemsMeta
import net.craftventure.core.metadata.GenericPlayerMeta
import net.craftventure.core.metadata.InventoryTrackingMeta
import net.craftventure.database.MainRepositoryProvider
import net.minecraft.network.protocol.game.ClientboundGameEventPacket
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.command.CommandSender
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.entity.Pose
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.util.*

//fun Player.hasInventoryOpen() = (player as? CraftPlayer)?.handle?.activeContainer != null

fun Player.getCvMetaData(): CvMetadata? = getMetadata<CvMetadata>()

fun Player.isOperatingSomewhere(): Boolean = CraftventureCore.getOperatorManager().isOperatingSomewhere(this)

fun Player.getOperatingRide() = CraftventureCore.getOperatorManager().getOperatingRide(this)

fun Player.isAfk(): Boolean = getCvMetaData()?.afkStatus?.isAfk ?: false

fun Player.isMuted(): Boolean = PunishmentManager.get().let {
    it.isMuted(uniqueId.toString()) || it.isMuted(uniqueId.toString().replace("-", ""))
}

fun UUID.isMuted(): Boolean = PunishmentManager.get().let {
    it.isMuted(this.toString()) || it.isMuted(this.toString().replace("-", ""))
}

fun Player.eyeOffset(): Vector {
    val location = location
    val eyeLocation = eyeLocation
    return Vector(
        eyeLocation.x - location.x,
        eyeLocation.y - location.y,
        eyeLocation.z - location.z,
    )
}

fun Player.getMatrix(): Matrix4x4 {
    val matrix = Matrix4x4()
    val location = location
    matrix.translateRotate(location.x, location.y, location.z, location.pitch, location.yaw)
    return matrix
}

fun Player.getEyeMatrix(): Matrix4x4 {
    val matrix = Matrix4x4()
    val location = eyeLocation
    matrix.translateRotate(location.x, location.y - 0.23, location.z, location.pitch, location.yaw)
    return matrix
}

fun Player.getSafeEyeMatrix(): Matrix4x4? {
    val pose = this.pose
    if (pose != Pose.STANDING && pose != Pose.SNEAKING) return null
//    when(pose) {
//        Pose.STANDING -> TODO()
//        Pose.FALL_FLYING -> TODO()
//        Pose.SLEEPING -> TODO()
//        Pose.SWIMMING -> TODO()
//        Pose.SPIN_ATTACK -> TODO()
//        Pose.SNEAKING -> TODO()
//        Pose.LONG_JUMPING -> TODO()
//        Pose.DYING -> TODO()
//        Pose.CROAKING -> TODO()
//        Pose.USING_TONGUE -> TODO()
//        Pose.ROARING -> TODO()
//        Pose.SNIFFING -> TODO()
//        Pose.EMERGING -> TODO()
//        Pose.DIGGING -> TODO()
//    }
    val matrix = Matrix4x4()
    val location = eyeLocation
    matrix.translateRotate(location.x, location.y - 0.23, location.z, location.pitch, location.yaw)
    return matrix
}

fun UUID.isBanned(): Boolean = PunishmentManager.get().let {
    it.isBanned(this.toString()) || it.isBanned(
        this.toString().replace("-", "")
    ) || Bukkit.getServer().bannedPlayers.any { it.uniqueId == this }
}

fun Player.canUseChat(sendMessage: Boolean = false): Boolean {
    val cvMetaData = getMetadata<GenericPlayerMeta>()
    if (!hasPermission("craftventure.chat.timeout.bypass"))
        if (cvMetaData != null) {
            if (System.currentTimeMillis() < 1000 + cvMetaData.lastChatTime) {
                if (sendMessage)
                    sendMessage(Translation.CHAT_CHAT_TOO_FAST.getTranslation(player)!!)
                return false
            }
            cvMetaData.updateLastChatTime()
        }
    return true
}

fun Player.canUseCommand(sendMessage: Boolean = false): Boolean {
    val cvMetaData = getMetadata<GenericPlayerMeta>()
    if (!hasPermission("craftventure.chat.timeout.bypass"))
        if (cvMetaData != null) {
            if (System.currentTimeMillis() < 1000 + cvMetaData.lastChatTime) {
                if (sendMessage)
                    sendMessage(Translation.CHAT_COMMAND_TOO_FAST.getTranslation(player)!!)
                return false
            }
            cvMetaData.updateLastChatTime()
        }
    return true
}

fun Player.displayNoFurther() {
    val particleLocation = eyeLocation.clone().add(
        location.direction.normalize().multiply(0.9)
    )
    spawnParticle(Particle.BLOCK_MARKER, particleLocation, 1, Material.BARRIER.createBlockData())
}

fun CommandSender.isPlayerOrConsole() = this is Player || this === Bukkit.getConsoleSender()
fun CommandSender.isConsole() = this === Bukkit.getConsoleSender()

private val teleportFlags = setOf(
    WrapperPlayServerPosition.PlayerTeleportFlag.X,
    WrapperPlayServerPosition.PlayerTeleportFlag.Y,
    WrapperPlayServerPosition.PlayerTeleportFlag.Z,
    WrapperPlayServerPosition.PlayerTeleportFlag.X_ROT,
    WrapperPlayServerPosition.PlayerTeleportFlag.Y_ROT
)

fun Player.isWearingGoPro() = equippedItemsMeta()?.appliedEquippedItems?.helmetItem?.id == "gopro"

fun Player.followVehicleYawPitch(
    yawFreedom: Double = 20.0,
    pitchFreedom: Double = 20.0,
    percentage: Double = 0.3,
    maxDegreeUpdate: Double = 1.0
) {
    vehicle?.let {
        followDirection(
            yawFreedom,
            pitchFreedom,
            it.location.yaw.toDouble(),
            it.location.pitch.toDouble(),
            percentage = percentage,
            maxDegreeUpdate = maxDegreeUpdate
        )
    }
}

@JvmOverloads
fun Player.followDirection(
    yawFreedom: Double = 20.0,
    pitchFreedom: Double = 20.0,
    yawTarget: Double,
    pitchTarget: Double,
    percentage: Double = 0.3,
    maxDegreeUpdate: Double = 1.0
) {
    val playerYaw = location.yaw % 360
    val vehicleYaw = yawTarget % 360

    var yawDifference = vehicleYaw - playerYaw
//    yawDifference = (yawDifference + 180) % 360 - 180
    while (yawDifference < -180) yawDifference += 360
    while (yawDifference > 180) yawDifference -= 360

    val playerPitch = location.pitch % 360
    val vehiclePitch = pitchTarget % 360

    var pitchDifference = vehiclePitch - playerPitch
//    pitchDifference = (pitchDifference + 180) % 360 - 180
    while (pitchDifference < -180) pitchDifference += 360
    while (pitchDifference > 180) pitchDifference -= 360

//    Logger.info("${playerYaw.format(2)} ${vehicleYaw.format(2)} ${yawDifference.format(2)}")

    val updateYaw = yawDifference > yawFreedom || yawDifference < -yawFreedom
    val updatePitch = pitchDifference > pitchFreedom || pitchDifference < -pitchFreedom
    if (updateYaw || updatePitch) {

        val packet = WrapperPlayServerPosition()
        if (updateYaw) {
            val update = (yawDifference * percentage).clamp(-maxDegreeUpdate, maxDegreeUpdate)/*.let {
                when {
                    it > yawFreedom -> yawFreedom
                    it < -yawFreedom -> -yawFreedom
                    else -> it
                }
            }*/
            if (Math.abs(update) > yawFreedom)
                packet.yaw = update.toFloat()
        }
        if (updatePitch) {
            val update = (pitchDifference * percentage).clamp(-maxDegreeUpdate, maxDegreeUpdate)/*.let {
                when {
                    it > yawFreedom -> yawFreedom
                    it < -yawFreedom -> -yawFreedom
                    else -> it
                }
            }*/
            if (Math.abs(update) > pitchFreedom)
                packet.pitch = update.toFloat()
        }
        packet.flags = teleportFlags
        try {
            packet.sendPacket(this)
        } catch (e: Exception) {
        }
    }
}

fun Player.smokeTrees() {
    try {
        val setupPacket = ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 600f)
        val packet = ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 0.01f)
        try {
            (player as CraftPlayer).handle.connection.send(setupPacket)
            (player as CraftPlayer).handle.connection.send(packet)
        } catch (e: Exception) {
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(CraftventureCore.getInstance()) {
            addPotionEffect(PotionEffect(PotionEffectType.CONFUSION, Integer.MAX_VALUE, 1, true, false))
            addPotionEffect(PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, -1, true, false))
            sendMessage((CVTextColor.serverNotice + "You smoked some trees, skidiki-pap-pap ").append(CVTextColor.subtle + "(relog to make the effect disappear)"))
        }
    } catch (e: Exception) {
        sendMessage(CVTextColor.serverNotice + "The whole forest has burned down, you can't smoke trees right now")
    }
}

fun Player.rewardAchievement(name: String) = MainRepositoryProvider.achievementProgressRepository.reward(uniqueId, name)

fun Player.increaseAchievementCounter(name: String) =
    MainRepositoryProvider.achievementProgressRepository.increaseCounter(uniqueId, name)

fun Player.openMenu(inventoryMenu: InventoryMenu) {
    InventoryTrackingMeta.get(this)?.push(inventoryMenu)
}