package net.craftventure.core.listener

import io.papermc.paper.event.player.AsyncChatEvent
import net.craftventure.audioserver.event.AudioServerConnectedEvent
import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.bukkit.ktx.util.PermissionChecker
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.async.executeAsync
import net.craftventure.core.serverevent.OwnedItemUseEvent
import net.craftventure.database.MainRepositoryProvider
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import java.util.*


class AchievementListener : Listener {
    private val mercFlower = Location(Bukkit.getWorld("world"), )
    private val mercFlower2 = Location(Bukkit.getWorld("world"), )
    private val cakeIsALieCookieMuseum = Location(Bukkit.getWorld("world"), )
    private val winterBeerTime = Location(Bukkit.getWorld("world"), )

    private val robbieRottenArea = SimpleArea("world",)

    @EventHandler(ignoreCancelled = true)
    fun onOwnedItemUse(event: OwnedItemUseEvent) {

    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onAsyncPlayerChat(event: AsyncChatEvent) {
        if (event.player !in robbieRottenArea) return
//        Logger.debug("In area")
        val message = PlainTextComponentSerializer.plainText().serialize(event.message())
        if (message.contains("he was number one", ignoreCase = true) ||
            message.contains("you were our number one", ignoreCase = true)
        ) {
//            Logger.debug("Message contains")
            executeAsync {
                if (MainRepositoryProvider.playerOwnedItemRepository
                        .createOneLimited(event.player.uniqueId, "costume_robbie", -1)
                ) {
                    event.player.sendMessage(CVTextColor.serverNotice + "You've received a new item for honoring the #1")
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerInteractEvent(event: PlayerInteractEvent) {
        val block = event.clickedBlock
        if (block != null) {
            if (checkMower(block, event.player)) {
                event.isCancelled = true
                return
            }

            if (block.location == mercFlower || block.location == mercFlower2) {
                if (event.item != null && (event.item?.type == Material.DANDELION || event.item?.type == Material.POPPY)) {
                    event.isCancelled = true
                    executeAsync {
                        MainRepositoryProvider.achievementProgressRepository.reward(
                            event.player.uniqueId,
                            "marc_flower"
                        )
                    }
                }
            }
            if (block.location == winterBeerTime) {
                event.isCancelled = true
                executeAsync {
                    MainRepositoryProvider.achievementProgressRepository
                        .reward(event.player.uniqueId, "winter_2019_beer_time")
                }
            } else if (block.location == cakeIsALieCookieMuseum) {
                event.isCancelled = true
                executeAsync {
                    MainRepositoryProvider.achievementProgressRepository
                        .reward(event.player.uniqueId, "cookie_museum_cake_lie")
                }
            }
        }
    }

    private fun checkMower(block: Block?, player: Player): Boolean {
        if (block != null && (player.gameMode == GameMode.SURVIVAL || player.gameMode == GameMode.ADVENTURE)) {
            val material = block.type
            if (material == Material.TALL_GRASS ||
                material == Material.GRASS ||
                material == Material.CHORUS_FLOWER ||
                material == Material.DANDELION ||
                material == Material.BLUE_ORCHID ||
                material == Material.ALLIUM ||
                material == Material.AZURE_BLUET ||
                material == Material.ORANGE_TULIP ||
                material == Material.PINK_TULIP ||
                material == Material.RED_TULIP ||
                material == Material.WHITE_TULIP ||
                material == Material.OXEYE_DAISY ||
                material == Material.POPPY ||
                material == Material.DEAD_BUSH
            ) {
                awardAchievement(player.uniqueId, "mower")
                return true
            }
        }
        return false
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onBlockBreak(event: BlockBreakEvent) {
        if (checkMower(event.block, event.player)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onAudioServerConnected(event: AudioServerConnectedEvent) {
        awardAchievement(event.player.uniqueId, "audio")
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    fun onEntityDamageByEntityEvent(event: EntityDamageByEntityEvent) {
        if (event.damager is Player && event.entity is Player) {
            checkSlap(event.damager as Player, event.entity as Player)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerInteract(event: PlayerInteractEntityEvent) {
        if (event.rightClicked is Player && event.hand == EquipmentSlot.HAND) {
            checkSlap(event.player, event.rightClicked as Player)
        }
    }

    private fun checkSlap(damager: Player, target: Player) {
//        Logger.info("${damager.name} slapped ${target.name}")
        if (PermissionChecker.isOwner(target)) {
            awardAchievement(damager.uniqueId, "slap_owner")
        }
        if (PermissionChecker.isVIP(target)) {
            awardAchievement(damager.uniqueId, "slap_vip")
        }
        awardAchievement(damager.uniqueId, "slap_player")
    }

    private fun awardAchievement(uuid: UUID, achievement: String) {
        executeAsync {
            MainRepositoryProvider.achievementProgressRepository.reward(uuid, achievement)
        }
    }
}
