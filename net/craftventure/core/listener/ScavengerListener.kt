package net.craftventure.core.listener

import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVChatColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.extension.isAfk
import net.craftventure.core.ktx.extension.random
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.utils.spawnParticleX
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.itemStack
import net.craftventure.database.type.BankAccountType
import net.craftventure.database.type.TransactionType
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.Skull
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.util.*

// Finally the official list of scavenger hunt spawns

class ScavengerListener : Listener {
    private var lastOreSpawn = 0L
    private var uuids = type.heads.map { (it.itemMeta as SkullMeta).playerProfile!!.id }

    companion object {
        val type: EventType = EventType.WINTER
    }

    init {
        for (block in type.blocks)
            block.block.type = Material.AIR

        Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), {
            update()
        }, 20L, 20L)
    }

    private fun update() {
        if (!type.active()) return
        if (type.blocks.isEmpty()) return
        if (System.currentTimeMillis() > lastOreSpawn + 15000) {
//            Logger.info("Spawning easter scavenger item")
            lastOreSpawn = System.currentTimeMillis()
            val count = type.blocks.count { it.block.type == Material.PLAYER_HEAD }

            if (count < 10) {
                var block: Block? = null
                for (i in 0..15) {
                    val potentialBlock = type.blocks.random()!!.block
                    if (potentialBlock.type == Material.AIR) {
                        block = potentialBlock
                        break
                    }/* else if (potentialBlock.type != Material.AIR) {
                        Logger.info("BLock is of type ${potentialBlock.type}")
                    }*/
                }

                if (block != null) {
                    block.type = Material.PLAYER_HEAD
                    val state = block.state
                    if (state is Skull) {
                        val baseItem = type.heads.random()!!.itemMeta as SkullMeta
//                        Logger.console("Owner ${meta.owningPlayer?.uniqueId?.toString()}")

//                        state.owningPlayer = baseItem.owningPlayer ?: Bukkit.getOfflinePlayer("Joeywp")
                        if (baseItem.playerProfile != null)
                            state.setPlayerProfile(baseItem.playerProfile!!)
//                        state.owningPlayer = (baseItem.itemMeta as SkullMeta).owningPlayer
                        state.update(true, true)
//                        Logger.info("Egg spawned")
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerInteractEvent(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        if (event.action != Action.LEFT_CLICK_BLOCK && event.action != Action.RIGHT_CLICK_BLOCK) return

        event.clickedBlock?.let {
            if (handleClick(event.player, it)) {
                event.isCancelled = true
            }
        }
    }

    fun handleClick(player: Player, block: Block): Boolean {
        if (player.isAfk()) {
            return false
        }

        val state = block.state
        if (state is Skull) {
            val id = state.owningPlayer?.uniqueId ?: return false
//                Logger.info("Id $id")
            if (uuids.any { it == id }) {
                block.type = Material.AIR
                val location = block.location

                type.handler(player, id, location.add(0.5, 0.5, 0.5))
                return true
            }
        }
        return false
    }

    enum class EventType(
        val heads: Array<ItemStack>,
        val blocks: Array<Location>,
        val handler: (player: Player, uuid: UUID, location: Location) -> Unit,
        val active: () -> Boolean
    ) {
        NONE(emptyArray(), emptyArray(), { _, _, _ -> }, { false }),
        EASTER(
            arrayOf(
                MainRepositoryProvider.itemStackDataRepository.findCached("egg_blue")!!.itemStack!!,
                MainRepositoryProvider.itemStackDataRepository.findCached("egg_green")!!.itemStack!!,
                MainRepositoryProvider.itemStackDataRepository.findCached("egg_pink")!!.itemStack!!,
                MainRepositoryProvider.itemStackDataRepository.findCached("egg_red")!!.itemStack!!,
                MainRepositoryProvider.itemStackDataRepository.findCached("egg_creeper")!!.itemStack!!,
                MainRepositoryProvider.itemStackDataRepository.findCached("egg_question")!!.itemStack!!,
                MainRepositoryProvider.itemStackDataRepository.findCached("egg_easter_pink")!!.itemStack!!
            ),
            arrayOf(
                // Singapore
                Location(Bukkit.getWorld("world"), -15.0, 48.0, -386.0),
                Location(Bukkit.getWorld("world"), -140.0, 50.0, -394.0),
                Location(Bukkit.getWorld("world"), -9.0, 43.0, -472.0),
                Location(Bukkit.getWorld("world"), -97.0, 47.0, -553.0),
                Location(Bukkit.getWorld("world"), -151.0, 45.0, -448.0),
                Location(Bukkit.getWorld("world"), 16.0, 44.0, -468.0),
                Location(Bukkit.getWorld("world"), -51.0, 41.0, -507.0),
                // Viking
                Location(Bukkit.getWorld("world"), -81.0, 47.0, -651.0),
                Location(Bukkit.getWorld("world"), -46.0, 47.0, -662.0),
                Location(Bukkit.getWorld("world"), -28.0, 43.0, -587.0),
                Location(Bukkit.getWorld("world"), -149.0, 44.0, -685.0),
                Location(Bukkit.getWorld("world"), -56.0, 43.0, -640.0),
                Location(Bukkit.getWorld("world"), -219.0, 47.0, -558.0),
                Location(Bukkit.getWorld("world"), -132.0, 37.0, -615.0),
                // CCR
                Location(Bukkit.getWorld("world"), 150.0, 41.0, -409.0),
                Location(Bukkit.getWorld("world"), 171.0, 41.0, -456.0),
                Location(Bukkit.getWorld("world"), 186.0, 43.0, -435.0),
                Location(Bukkit.getWorld("world"), 224.0, 43.0, -432.0),
                Location(Bukkit.getWorld("world"), 262.0, 42.0, -524.0),
                Location(Bukkit.getWorld("world"), 297.0, 44.0, -484.0),
                Location(Bukkit.getWorld("world"), 253.0, 43.0, -494.0),
                // Mexico
                Location(Bukkit.getWorld("world"), 247.0, 43.0, -625.0),
                Location(Bukkit.getWorld("world"), 288.0, 42.0, -694.0),
                Location(Bukkit.getWorld("world"), 332.0, 46.0, -636.0),
                Location(Bukkit.getWorld("world"), 349.0, 47.0, -582.0),
                Location(Bukkit.getWorld("world"), 375.0, 45.0, -586.0),
                Location(Bukkit.getWorld("world"), 406.0, 41.0, -621.0),
//                // Mainstreet
//                Location(Bukkit.getWorld("world"), 121.00, 47.00, -979.00),
//                Location(Bukkit.getWorld("world"), 51.00, 48.00, -988.00),
//                Location(Bukkit.getWorld("world"), 128.00, 42.00, -821.00),
//                Location(Bukkit.getWorld("world"), 141.00, 46.00, -730.00),
//                Location(Bukkit.getWorld("world"), 92.00, 41.00, -801.00),
//                Location(Bukkit.getWorld("world"), 108.00, 47.00, -496.00),
//                Location(Bukkit.getWorld("world"), 6.00, 43.00, -723.00),
//                Location(Bukkit.getWorld("world"), 115.00, 45.00, -891.00),
                // Discovery
                Location(Bukkit.getWorld("world"), 162.00, 38.00, -801.00),
                Location(Bukkit.getWorld("world"), 275.00, 45.00, -802.00),
                Location(Bukkit.getWorld("world"), 233.00, 39.00, -916.00),
                Location(Bukkit.getWorld("world"), 228.00, 42.00, -722.00),
                Location(Bukkit.getWorld("world"), 183.00, 44.00, -848.00),
                Location(Bukkit.getWorld("world"), 257.00, 43.00, -843.00)
            ),
            { player: Player, uuid: UUID, location: Location ->
                val creeper = uuid.toString() == "20726b71-7df4-4380-a5b7-311ac1aeaa29"
                val question = uuid.toString() == "f38b81cd-1bd8-4857-a858-9a9b3292a821"

//                    Logger.info("$creeper $question")

                location.spawnParticleX(
                    Particle.EXPLOSION_NORMAL,
                    5
                )

                when {
                    creeper -> {
                        location.spawnParticleX(
                            Particle.EXPLOSION_HUGE,
                            5
                        )

                        location.world?.playSound(location, Sound.ENTITY_CREEPER_HURT, 3f, 1f)
                    }

                    question -> {
                        location.spawnParticleX(
                            Particle.EXPLOSION_NORMAL,
                            5
                        )

                        location.world?.playSound(location, Sound.ENTITY_ENDER_DRAGON_HURT, 3f, 1f)
                    }

                    else -> {
                        location.spawnParticleX(
                            Particle.EXPLOSION_NORMAL,
                            5
                        )

                        location.world?.playSound(location, Sound.ENTITY_FIREWORK_ROCKET_BLAST_FAR, 3f, 1f)
                    }
                }

                executeAsync {
                    when {
                        creeper -> {
                            MainRepositoryProvider.bankAccountRepository.delta(
                                player.uniqueId,
                                BankAccountType.VC,
                                -5,
                                TransactionType.MINIGAME
                            )
                            player.sendMessage(CVChatColor.serverNotice + "That egg exploded, you lost 5 VentureCoins by the explosion")
                            MainRepositoryProvider.achievementProgressRepository
                                .reward(player.uniqueId, "easteregg_creeper")
                        }

                        question -> {
                            val reward = 5 - CraftventureCore.getRandom().nextInt(10)
                            if (!player.isCrew())
                                MainRepositoryProvider.bankAccountRepository.delta(
                                    player.uniqueId,
                                    BankAccountType.VC,
                                    reward.toLong(),
                                    TransactionType.MINIGAME
                                )
                            when {
                                reward > 0 -> player.sendMessage(CVChatColor.serverNotice + "That egg contained $reward VentureCoins")
                                reward < 0 -> player.sendMessage(CVChatColor.serverNotice + "That egg stole $reward VentureCoins from you!")
                                else -> {
                                    player.sendMessage(CVChatColor.serverNotice + "There is nothing in this egg...")
                                }
                            }
                            MainRepositoryProvider.achievementProgressRepository
                                .reward(player.uniqueId, "easteregg_question")
                        }

                        else -> {
                            MainRepositoryProvider.bankAccountRepository.delta(
                                player.uniqueId,
                                BankAccountType.VC,
                                1,
                                TransactionType.MINIGAME
                            )
                            player.sendMessage(CVChatColor.serverNotice + "That egg contained 1 VentureCoin")
                            MainRepositoryProvider.achievementProgressRepository
                                .reward(player.uniqueId, "easteregg_default")
                        }
                    }
                    MainRepositoryProvider.achievementProgressRepository.reward(
                        player.uniqueId,
                        "easteregg_${java.time.LocalDateTime.now().year}"
                    )
                }
            }, DateUtils::isEastern
        ),
        WINTER(
            arrayOf(
                MainRepositoryProvider.itemStackDataRepository.findCached("gift1")!!.itemStack!!,
                MainRepositoryProvider.itemStackDataRepository.findCached("gift2")!!.itemStack!!,
                MainRepositoryProvider.itemStackDataRepository.findCached("gift3")!!.itemStack!!,
                MainRepositoryProvider.itemStackDataRepository.findCached("gift4")!!.itemStack!!
            ),
            arrayOf(
//                Location(Bukkit.getWorld("world"), 42.50, 44.00, -267.50),
//                Location(Bukkit.getWorld("world"), 91.50, 43.00, -301.50),
//                Location(Bukkit.getWorld("world"), 88.50, 45.00, -314.50),
//                Location(Bukkit.getWorld("world"), 74.50, 43.00, -308.50),
//                Location(Bukkit.getWorld("world"), 81.50, 44.00, -314.50),
//                Location(Bukkit.getWorld("world"), 116.50, 47.00, -300.50),
//                Location(Bukkit.getWorld("world"), 180.50, 49.00, -331.50),
//                Location(Bukkit.getWorld("world"), 93.50, 40.00, -257.50),
//                Location(Bukkit.getWorld("world"), 62.50, 42.00, -263.50),
//                Location(Bukkit.getWorld("world"), 92.50, 43.00, -239.50),
//                Location(Bukkit.getWorld("world"), 92.50, 43.00, -239.50),
//                Location(Bukkit.getWorld("world"), 127.50, 44.00, -215.50),
//                Location(Bukkit.getWorld("world"), 117.50, 44.00, -204.50),
//                Location(Bukkit.getWorld("world"), 100.50, 46.00, -209.50),
//                Location(Bukkit.getWorld("world"), 69.50, 45.00, -205.50),
//                Location(Bukkit.getWorld("world"), 60.50, 45.00, -219.50),
//                Location(Bukkit.getWorld("world"), 73.50, 46.00, -214.50),
//                Location(Bukkit.getWorld("world"), 34.50, 49.00, -261.50),
//                Location(Bukkit.getWorld("world"), 54.50, 48.00, -259.50),
//                Location(Bukkit.getWorld("world"), 111.50, 48.00, -309.50),
//                Location(Bukkit.getWorld("world"), 107.50, 46.00, -295.50),
//                Location(Bukkit.getWorld("world"), 103.50, 42.00, -287.50),
            ),
            { player: Player, uuid: UUID, location: Location ->
                val block = location.block
                block.setType(Material.AIR, true)

                val location = block.location.clone().add(0.5, 0.5, 0.5)
                location.spawnParticleX(
                    Particle.EXPLOSION_NORMAL,
                    5,
                    extra = 0.2
                )
                location.world?.playSound(location, Sound.ENTITY_FIREWORK_ROCKET_BLAST_FAR, 3f, 1f)

                executeAsync {
                    val amount = 1 + (Math.random() * 2).toLong()
                    if (!player.isCrew())
                        MainRepositoryProvider.bankAccountRepository.delta(
                            player.uniqueId,
                            BankAccountType.VC,
                            amount,
                            TransactionType.SCAVENGER_PACKET
                        )
                    player.sendMessage(CVChatColor.serverNotice + "You received $amount ${BankAccountType.VC.pluralName} from the gift")
                    MainRepositoryProvider.achievementProgressRepository
                        .increaseCounter(player.uniqueId, "winter2021_present")

                    val chance = CraftventureCore.getRandom().nextDouble()
                    if (chance < 0.01) {
                        val gotShiny = MainRepositoryProvider.playerOwnedItemRepository.createOneLimited(
                            player.uniqueId,
                            "hat_santa_shiny",
                            -1
                        )
                    }/* else if (chance < 0.10) {
                            CraftventureCore.getBankAccountDatabase().delta(player.uniqueId, BankAccountType.WINTER_TICKETS, 1, TransactionType.MINIGAME)
                            player.sendMessage(CVChatColor.COMMAND_GENERAL + "You received 1 WinterTicket")
                        }*/
                }
            }, { DateUtils.isWinter }
        )
    }
}