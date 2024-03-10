package net.craftventure.core.ride.tracklessride.property

class DoublePropertyAnimator(
    /**
     * @return true when finished
     */
    val animate: (timeDeltaMs: Long, property: DoubleProperty) -> Boolean,
)