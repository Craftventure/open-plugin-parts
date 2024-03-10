package net.craftventure.core.ride.tracklessride.transport.car

import java.io.File

abstract class CarConfig {
    abstract fun createFactory(directory: File): CarFactory
}