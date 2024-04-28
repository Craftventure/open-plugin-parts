package net.craftventure.core.ride.trackedride.segment

import net.craftventure.core.ride.operator.controls.OperatorControl


interface OperableTrackSegment {
    fun provideControls(): List<OperatorControl>

    fun onOperatorsChanged()
}
