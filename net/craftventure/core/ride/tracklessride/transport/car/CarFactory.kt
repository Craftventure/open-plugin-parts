package net.craftventure.core.ride.tracklessride.transport.car

import net.craftventure.core.ride.tracklessride.BaseTagContainer
import net.craftventure.core.ride.tracklessride.TagContainer
import net.craftventure.core.ride.tracklessride.TracklessRide
import net.craftventure.core.ride.tracklessride.config.CarGroupCarConfig
import net.craftventure.core.ride.tracklessride.config.CarGroupConfig
import net.craftventure.core.ride.tracklessride.navigation.PathPosition

abstract class CarFactory {
    abstract fun produce(
        position: PathPosition,
        groupId: Int,
        groupConfig: CarGroupConfig,
        carId: Int,
        carConfig: CarGroupCarConfig,
        trackedRide: TracklessRide,
        tagContainer: TagContainer = BaseTagContainer(),
    ): TracklessRideCar
}