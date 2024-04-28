package net.craftventure.core.listener

//import net.craftventure.core.CraftventureCore
//import net.craftventure.core.async.executeAsync
//import net.craftventure.core.database.PlayerKeyValueDatabase
//import net.craftventure.core.extension.*
//import net.craftventure.chat.bungee.util.CVChatColor
//import net.craftventure.core.ktx.util.DateUtils
//import net.craftventure.bukkit.ktx.util.SoundUtils
//import net.craftventure.database.MainRepositoryProvider
//import net.craftventure.database.type.BankAccountType
//import net.craftventure.database.type.TransactionType
//import net.craftventure.chat.bungee.extension.plus
//import org.bukkit.*
//import org.bukkit.entity.EntityType
//import org.bukkit.entity.Firework
//import org.bukkit.event.EventHandler
//import org.bukkit.event.EventPriority
//import org.bukkit.event.Listener
//import org.bukkit.event.player.PlayerInteractEvent
//import org.bukkit.inventory.EquipmentSlot
//import java.util.*
//import java.util.concurrent.locks.ReentrantLock
//import kotlin.concurrent.withLock


class ChristmasListener /*: Listener */ {
//    private val dailyLocation = Location(Bukkit.getWorld("world"), 89.00, 42.00, -724.00)
//    private val wishingWellLocation = Location(Bukkit.getWorld("world"), 89.00, 75.00, -587.00)
//    private val lock = ReentrantLock()
//
//    init {
////        Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), {
////            val loc = Location(Bukkit.getWorld("world"), 74.0, 43.0, -620.0)
////            loc.world.playSound(loc, SoundUtils.ACIS, SoundCategory.AMBIENT, 1f, 1f)
////        }, 20 * 60 * 2, 20 * 60 * 2)
//    }
//
//    @EventHandler(priority = EventPriority.LOWEST)
//    fun onPlayerInteractEvent(event: PlayerInteractEvent) {
//        if (event.hand != EquipmentSlot.HAND) return
////        if (!DateUtils.isWinter2018) return
//
//        val player = event.player
//        event.clickedBlock?.let { block ->
//            //            Logger.info("BLock ${block} ${block.type}")
//            if (block.type.isSign()) {
//                if (block.location.compareBlockLocation(wishingWellLocation)) {
//                    event.isCancelled = true
//                    executeAsync {
//                        if (player.isCrew() && !player.isOwner()) {
//                            player.sendMessage(CVChatColor.COMMAND_GENERAL + "As crew you have an unfair advantage, so you can't play this game")
//                            return@executeAsync
//                        }
//                        val playerKeyValueDatabase = MainRepositoryProvider.playerKeyValueRepository
//                        val bankDatabase = CraftventureCore.getBankAccountDatabase()
//                        val balance = bankDatabase[player.uniqueId, BankAccountType.VC]?.balance ?: 0
//                        if (balance < 25) {
//                            player.sendMessage(CVChatColor.COMMAND_GENERAL + "You don't have enough ${BankAccountType.VC.pluralName} to do that")
//                            return@executeAsync
//                        }
//
////                        playerKeyValueDatabase.syncWith {
//                        val key = PlayerKeyValueDatabase.currentWishingWellKey()
//                        val currentValue = (playerKeyValueDatabase.getValue(player.uniqueId, key)?.toLongOrNull()
//                            ?: 0) + 25
//
//                        if (bankDatabase.delta(
//                                player.uniqueId,
//                                BankAccountType.VC,
//                                -25,
//                                TransactionType.WISHING_WELL
//                            )
//                        ) {
//                            playerKeyValueDatabase.createOrUpdate(player.uniqueId, key, currentValue.toString())
//                            player.sendMessage(CVChatColor.COMMAND_GENERAL + "You spend a total of $currentValue ${BankAccountType.VC.pluralName} on wishes")
//                            executeAsync {
//                                MainRepositoryProvider.achievementProgressRepository
//                                    .reward(player.uniqueId, "winter2018_bestwishes")
//                            }
//                            return@executeAsync
//                        }
//                        return@executeAsync
////                        }
//                    }
//                }
//            } else if (block.type == Material.TRAPPED_CHEST || block.type == Material.CHEST || block.type == Material.ENDER_CHEST) {
//                if (block.location.compareBlockLocation(dailyLocation)) {
//                    event.isCancelled = true
//                    executeAsync {
//                        val database = MainRepositoryProvider.playerKeyValueRepository
//                        lock.withLock {
//                            val now = GregorianCalendar.getInstance()
//                            val todayValue = "${now.get(Calendar.MONTH) + 1}_${now.get(Calendar.DAY_OF_MONTH)}"
//
//                            val value = database.getValue(event.player.uniqueId, "winter2018_dailychest")
//                            val canUse = value != todayValue
////                            Logger.console("${event.player.name} can use daily chest? $canUse $value")
//                            if (canUse) {
//                                val amount = 25L
//                                CraftventureCore.getBankAccountDatabase().delta(
//                                    event.player.uniqueId,
//                                    BankAccountType.VC,
//                                    amount,
//                                    TransactionType.DAILY_WINTER_CHEST
//                                )
//                                database.createOrUpdate(event.player.uniqueId, "winter2018_dailychest", todayValue)
//                                event.player.playSound(
//                                    event.player.location,
//                                    SoundUtils.MONEY,
//                                    SoundCategory.AMBIENT,
//                                    10f,
//                                    1f
//                                )
//
//                                event.player.sendMessage(CVChatColor.COMMAND_GENERAL + "You received your daily chest bonus of $amount VentureCoins! Come back tomorrow for more!")
//
//                                val blockLocation = block.location
//                                val location = blockLocation.clone().add(0.5, 0.5, 0.5)
//                                event.player.playSound(location, Sound.BLOCK_CHEST_OPEN, 1f, 1f)
//
//                                MainRepositoryProvider.achievementProgressRepository
//                                    .increaseCounter(event.player.uniqueId, "winter2018_dailychest")
//                                Bukkit.getScheduler().scheduleSyncDelayedTask(CraftventureCore.getInstance(), {
//                                    blockLocation.openChest(true)
//                                }, 2)
//                                Bukkit.getScheduler().scheduleSyncDelayedTask(CraftventureCore.getInstance(), {
//                                    val fw = location.world!!.spawnEntity(location, EntityType.FIREWORK) as Firework
//                                    val fwm = fw.fireworkMeta
//                                    fwm.power = 0
//
//                                    val effect = FireworkEffect.builder()
//                                        .flicker(true)
//                                        .withColor(Color.fromRGB(0x0c5915))
//                                        .withFade(Color.fromRGB((0x0c5915 * 0.5).toInt()))
//                                        .with(FireworkEffect.Type.STAR)
//                                        .trail(true)
//                                        .build()
//                                    fwm.addEffect(effect)
//
//                                    fw.fireworkMeta = fwm
//
//                                    Bukkit.getScheduler().scheduleSyncDelayedTask(CraftventureCore.getInstance(), {
//                                        fw.detonate()
//                                    }, 2)
//                                }, 15)
//                                for (i in 1..8)
//                                    Bukkit.getScheduler().scheduleSyncDelayedTask(CraftventureCore.getInstance(), {
//                                        blockLocation.openChest(true)
//                                    }, i * 5L)
//                                Bukkit.getScheduler().scheduleSyncDelayedTask(CraftventureCore.getInstance(), {
//                                    blockLocation.openChest(false)
//                                }, 45)
//                            } else {
//                                val midnight = GregorianCalendar()
//
//                                midnight.set(Calendar.HOUR_OF_DAY, 0)
//                                midnight.set(Calendar.MINUTE, 0)
//                                midnight.set(Calendar.SECOND, 0)
//                                midnight.set(Calendar.MILLISECOND, 0)
//
//                                midnight.add(Calendar.DAY_OF_MONTH, 1)
//
//                                val between = midnight.time.time - Date().time
//
//                                event.player.sendMessage(
//                                    "§cYou already used your daily chest today! Come back in ${
//                                        DateUtils.format(
//                                            between,
//                                            "?"
//                                        )
//                                    }"
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
}