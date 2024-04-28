package net.craftventure.core.listener

import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.bukkit.ktx.event.PlayerLocationChangedEvent
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.extension.isAfk
import net.craftventure.core.ktx.extension.random
import net.craftventure.core.utils.TitleUtil.sendTitleWithTicks
import net.craftventure.core.utils.spawnParticleX
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.itemStack
import net.craftventure.database.type.BankAccountType
import net.craftventure.database.type.TransactionType
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.Skull
import org.bukkit.entity.CaveSpider
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*

class ChristmasMinersCave : Listener {
    private var lastOreSpawn = 0L
    private var lastSpiderSpawn = 0L
    private val players = ArrayList<CavePlayer>()
    private val caveArea = SimpleArea("world", )
    private val entities = arrayOfNulls<Monster>(20)
    private val spawnLocations = listOf(
    )
    private val blocks = listOf(
    )

    private var items = listOf(
        MainRepositoryProvider.itemStackDataRepository.findCached("ore1")!!.itemStack!!,
    )
    private var uuids = listOf(
        UUID.fromString("0c2a7f5d-322b-4e6b-b279-d9e988446dc8"),
    )

    init {
//        val min = caveArea.min
//        val max = caveArea.max
//        for (x in min.x.toInt()..max.x.toInt()) {
//            for (y in min.y.toInt()..max.y.toInt()) {
//                for (z in min.z.toInt()..max.z.toInt()) {
//                    val blockLocation = Location(min.world, x.toDouble(), y.toDouble(), z.toDouble())
//                    val block = blockLocation.block
//                    val state = block.state
//                    if (state is Skull) {
//                        val id = state.owningPlayer?.uniqueId?.toString()
//                        Logger.console("Location(\"world\", $x.0, $y.0, $z.0),")
//                    }
//                }
//            }
//        }
        for (block in blocks)
            block.block.type = Material.AIR

        Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), {
            update()
        }, 20L, 20L)
    }

    private fun update() {
        val mobAmount = mobAmount()
        for (i in entities.indices) {
            if (entities[i] == null || !entities[i]!!.isValid || !caveArea.isInArea(entities[i]!!) || i >= mobAmount * 1.05) {
                entities[i]?.remove()
                entities[i] = null

                if (i >= mobAmount)
                    continue

                if (lastSpiderSpawn + 4000 > System.currentTimeMillis())
                    continue

//                Logger.console("Spawning spider")

                lastSpiderSpawn = System.currentTimeMillis()

                val location = generateEntityLocation()

                val entity = location.world!!.spawn(location, CaveSpider::class.java)
                entity.canPickupItems = false
                entity.vehicle?.remove()

                entities[i] = entity
            }
        }
        if (System.currentTimeMillis() > lastOreSpawn + 2000) {
//            Logger.console("Spawning ore")
            lastOreSpawn = System.currentTimeMillis()
            val count = blocks.count { it.block.type == Material.PLAYER_HEAD }

            if (count < 10) {
                var block: Block? = null
                for (i in 0..15) {
                    val potentialBlock = blocks.random()!!.block
                    if (potentialBlock.type == Material.AIR) {
                        block = potentialBlock
                        break
                    }
                }

                if (block != null) {
                    block.type = Material.PLAYER_HEAD
                    val state = block.state
                    if (state is Skull) {
                        val baseItem = items.random()!!.itemMeta as SkullMeta
//                        Logger.console("Owner ${meta.owningPlayer?.uniqueId?.toString()}")

//                        state.owningPlayer = baseItem.owningPlayer ?: Bukkit.getOfflinePlayer("Joeywp")
                        if (baseItem.playerProfile != null)
                            state.setPlayerProfile(baseItem.playerProfile!!)
//                        state.owningPlayer = (baseItem.itemMeta as SkullMeta).owningPlayer
                        state.update(true, true)
                    }
                }
            }
        }
    }

    private fun generateEntityLocation(): Location {
//        for (i in 0..15) {
//            val x = CraftventureCore.getRandom().nextInt((mainArea.loc2.x - mainArea.loc1.x).toInt()) + mainArea.loc1.x + 0.5
//            val z = CraftventureCore.getRandom().nextInt((mainArea.loc2.z - mainArea.loc1.z).toInt()) + mainArea.loc1.z + 0.5
//            val location = Location(this.location.world, x, 35.0, z)
//            val locationGround = Location(this.location.world, x, 34.0, z)
//            if (location.block.type == Material.AIR && locationGround.block.type == Material.GRASS_PATH) {
//                return location
//            }
//        }
        return spawnLocations.random()!!
    }

    private fun mobAmount(): Int = 5 + Math.min(15, 1 * players.size)

    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        if (caveArea.isInArea(event.entity)) {
            event.entity.location.clone().add(0.0, 1.0, 0.0).spawnParticleX(
                Particle.REDSTONE,
                5,
                0.2, 0.1, 0.2
            )

            event.entity.location.clone().add(0.0, 0.1, 0.0).spawnParticleX(
                Particle.DRIP_LAVA,
                5,
                0.1, 0.0, 0.1
            )
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        players.removeAll { it.player === event.player }
        clean(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerLocationChangedEvent) {
        if (event.locationChanged)
            check(event.player)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerInteractEvent(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        if (event.action != Action.LEFT_CLICK_BLOCK) return

        event.clickedBlock?.let { block ->
            if (caveArea.isInArea(block.location)) {
                val state = block.state
                if (state is Skull) {
                    event.isCancelled = true
                    if (event.player.isAfk()) {
                        return
                    }
                    val id = state.owningPlayer?.uniqueId
//                    Logger.console("Id $id")
                    if (event.player.inventory.itemInMainHand.type == Material.STONE_PICKAXE && uuids.any { it == id }) {
                        event.clickedBlock!!.type = Material.AIR
                        val location = event.clickedBlock!!.location

                        location.spawnParticleX(
                            Particle.EXPLOSION_NORMAL,
                            5,
                            extra = 0.2
                        )
                        location.world?.playSound(location, Sound.ENTITY_FIREWORK_ROCKET_BLAST_FAR, 3f, 1f)

                        executeAsync {
                            MainRepositoryProvider.bankAccountRepository.delta(
                                event.player.uniqueId,
                                BankAccountType.WINTERCOIN,
                                1,
                                TransactionType.MINIGAME
                            )
                            event.player.sendMessage(CVTextColor.serverNotice + "You mined 1 WinterCoin")
                        }
                    }
                }
            }
        }
    }

    private fun clean(player: Player) {
        player.removePotionEffect(PotionEffectType.SLOW)
        player.inventory.remove(Material.STONE_PICKAXE)
        if (player.inventory.itemInOffHand.type == Material.STONE_PICKAXE) {
            player.inventory.setItemInOffHand(ItemStack(Material.AIR))
        }
    }

    private fun check(player: Player) {
        val isInRange = caveArea.isInArea(player)
        if (!isInRange) {
            val mazePlayer = players.firstOrNull { it.player === player }
            if (mazePlayer != null) {
                clean(player)
                players.removeAll { it.player === player }

//                    TitleUtil.send(player, 10, 20 * 4, 10,
//                            ChatColor.DARK_RED, "You failed",
//                            ChatColor.RED, "You left the maze")
            }
        } else {
            if (!players.any { it.player === player }) {
                val cavePlayer = CavePlayer(player)
                players.add(cavePlayer)

                cavePlayer.player.gameMode = GameMode.ADVENTURE

                player.sendTitleWithTicks(
                    10, 20 * 4, 10,
                    NamedTextColor.DARK_RED, "Abandoned Ice Cave",
                    NamedTextColor.RED, "Mine ore for WinterCoins"
                )

//                var pick = ItemStack(Material.STONE_PICKAXE, 1, 2.toShort()).addCanDestroyAndClone("minecraft:skull")
//                pick.unbreakable()
//                pick = ItemStackUtils.setDisplayNameAndLore(pick, "§6Cave Bruv", "§7Diggy diggy hole, digging a hole")
//                pick.addUnsafeEnchantment(Enchantment.DAMAGE_ARTHROPODS, 1)
//
//                player.inventory.setItem(6, pick)
                player.addPotionEffect(PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 2, true, false))
            }
        }
    }

    private data class CavePlayer(
        val player: Player
    )
}