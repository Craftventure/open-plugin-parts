package net.craftventure.core.ride.trackedride.config.addon

import com.squareup.moshi.JsonClass
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.async.executeAsync
import net.craftventure.core.ktx.extension.format
import net.craftventure.core.ride.trackedride.TrackedRide
import net.craftventure.core.ride.trackedride.config.TrackedRideAddOn
import net.craftventure.core.ride.trackedride.segment.StationSegment
import net.craftventure.database.MainRepositoryProvider

@JsonClass(generateAdapter = true)
class DistanceTrackerAddOn(
    val key: String,
) : TrackedRideAddOn() {
    override fun installIn(trackedRide: TrackedRide) {
        trackedRide.addOnRideCompletionListener { player, rideCar ->
            val startSegment = rideCar.attachedTrain.frontCarTrackSegment
            var segment = startSegment
            var distance = 0.0
            do {
                distance += segment.length
                segment = segment.previousTrackSegment
            } while (segment !is StationSegment)

            executeAsync {
                val database = MainRepositoryProvider.playerKeyValueRepository
                var value = database.getValue(
                    player.uniqueId,
                    key
                )?.toDoubleOrNull() ?: 0.0
                value += distance
                player.sendMessage(CVTextColor.serverNotice + "You now traveled a total of ${value.format(2)} meters by ${trackedRide.displayName}")
                database.createOrUpdate(
                    player.uniqueId,
                    key,
                    value.toString()
                )
//                MainRepositoryProvider.achievementProgressRepository
//                    .reward(player.uniqueId, "parktrain_${startSegment.id}")
            }
        }
    }

    companion object {
        const val type = "distance_tracker"
    }
}