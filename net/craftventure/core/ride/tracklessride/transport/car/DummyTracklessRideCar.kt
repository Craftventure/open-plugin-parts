package net.craftventure.core.ride.tracklessride.transport.car

import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.config.CarGroupCarConfig
import net.craftventure.core.ride.tracklessride.config.CarGroupConfig
import net.craftventure.core.ride.tracklessride.navigation.PathPosition
import org.bukkit.entity.Player

class DummyTracklessRideCar(
    position: PathPosition,
    idInGroup: Int,
    tracklessRide: TracklessRide,
    groupConfig: CarGroupConfig,
    carConfig: CarGroupCarConfig,
) : TracklessRideCar(
    position = position,
    idInGroup = idInGroup,
    tracklessRide = tracklessRide,
    groupConfig = groupConfig,
    carConfig = carConfig
) {
    override fun hasPlayers(): Boolean = false

    override val playerPassengers: Collection<Player> = emptySet()
    override fun tryToEnter(player: Player, seatEntityId: Int): Boolean = false
    override fun putPassenger(player: Player): Boolean = false
}