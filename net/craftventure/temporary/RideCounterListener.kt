package net.craftventure.temporary

import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.core.map.renderer.MapManager
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.tables.pojos.Ride
import net.craftventure.database.generated.cvdata.tables.pojos.RideCounter
import net.craftventure.database.repository.BaseIdRepository
import net.craftventure.database.repository.RideCounterRepository
import org.bukkit.Bukkit
import java.util.*
import kotlin.math.max

class RideCounterListener : BaseIdRepository.Listener<RideCounter>() {
    override fun onMerge(item: RideCounter) {
        handle(item)
    }

    override fun onInsert(item: RideCounter) {
        handle(item)
    }

    override fun onUpdate(item: RideCounter) {
        handle(item)
    }

    private fun handle(item: RideCounter) {
        val ride = MainRepositoryProvider.rideRepository.findCached(item.rideId!!)
        if (ride != null)
            MapManager.instance.invalidateRide(ride.name)
        executeSync {
            handle(item.uuid!!, ride, item)
        }
    }

    private fun handle(
        uuid: UUID,
        ride: Ride?,
        rideCounter: RideCounter
    ) {
        if (ride?.name != "canoes" && rideCounter.count!! >= 31) {
            MainRepositoryProvider.achievementProgressRepository.reward(uuid, "ride_marathon")
        }

        val player = Bukkit.getPlayer(uuid)
        if (player != null && ride != null && rideCounter != null) {
            player.sendMessage(
                Translation.RIDE_COUNTER_INCREASED.getTranslation(
                    player,
                    ride.displayName,
                    rideCounter.count
                )!!
            )
        }
        if (ride?.name == "vogelrok" && uuid != RideCounterRepository.uuidNico) {
            val count = rideCounter.count!!
            if (count >= RideCounterRepository.counterNico - 1) {
                val newScore = max(count + 1, RideCounterRepository.counterNico)
                if (newScore >= RideCounterRepository.counterNico) {
                    RideCounterRepository.counterNico = newScore
                    executeAsync {
                        val vogelRok = MainRepositoryProvider.rideRepository.getByName("vogelrok")
                        if (vogelRok != null)
                            MainRepositoryProvider.rideCounterRepository.setCounter(
                                RideCounterRepository.uuidNico,
                                vogelRok.id!!,
                                newScore
                            )
                    }
                }
            }
        }

        if (ride?.name != "canoes" && ride?.name != null) {
            rideCounter.count?.let {
                if (it >= 500) {
                    executeAsync {
                        MainRepositoryProvider.playerOwnedItemRepository.createOneLimited(uuid, "title_nolifer_500", 0)
                    }
                }
                if (it >= 1000) {
                    executeAsync {
                        MainRepositoryProvider.playerOwnedItemRepository.createOneLimited(uuid, "title_nolifer_1k", 0)
                    }
                }
                if (it >= 10_000) {
                    executeAsync {
                        MainRepositoryProvider.playerOwnedItemRepository.createOneLimited(uuid, "title_nolifer_10k", 0)
                    }
                }
                if (it >= 20_000) {
                    executeAsync {
                        MainRepositoryProvider.playerOwnedItemRepository.createOneLimited(uuid, "title_nolifer_20k", 0)
                    }
                }
                if (it >= 50_000) {
                    executeAsync {
                        MainRepositoryProvider.playerOwnedItemRepository.createOneLimited(uuid, "title_nolifer_50k", 0)
                    }
                }
                if (it >= 100_000) {
                    executeAsync {
                        MainRepositoryProvider.playerOwnedItemRepository.createOneLimited(uuid, "title_nolifer_100k", 0)
                    }
                }
                if (it >= 150_000) {
                    executeAsync {
                        MainRepositoryProvider.playerOwnedItemRepository.createOneLimited(uuid, "title_nolifer_150k", 0)
                    }
                }
                if (it >= 200_000) {
                    executeAsync {
                        MainRepositoryProvider.playerOwnedItemRepository.createOneLimited(uuid, "title_nolifer_200k", 0)
                    }
                }
            }
        }
    }
}