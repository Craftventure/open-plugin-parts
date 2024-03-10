package net.craftventure.core.ride.tracklessride.transport

import net.craftventure.core.ride.tracklessride.BaseTagContainer
import net.craftventure.core.ride.tracklessride.TagContainer
import net.craftventure.core.ride.tracklessride.scene.TracklessRideScene
import net.craftventure.core.ride.tracklessride.transport.car.TracklessRideCar

class TracklessRideCarGroup(
    val groupId: Int,
    cars: Array<TracklessRideCar>,
    val tagContainer: TagContainer = BaseTagContainer(),
    var currentScene: TracklessRideScene
) : TagContainer by tagContainer {
    var cars = cars
        private set
    var exitToOverrides: List<String>? = null
    var allowPrematureSceneSwitching: Boolean = false

    private val postUpdateCalls = mutableListOf<() -> Unit>()

    init {
        cars.forEach { it.group = this }
    }

    fun queuePostUpdateCall(call: () -> Unit) {
        postUpdateCalls += call
    }

    fun executePostUpdateCalls() {
        postUpdateCalls.forEach { it.invoke() }
        postUpdateCalls.clear()
    }

    fun reorder(newOrder: Array<Int>) {
        require(newOrder.size == cars.size) { "Order of size ${newOrder.size} doesn't match car size ${cars.size}" }
        val oldIds = cars.map { it.idInGroup }
        val reorderedCars = newOrder.map { order -> cars.first { car -> car.idInGroup == order } }
        reorderedCars.forEachIndexed { index, car -> car.idInGroup = oldIds[index] }
        cars = reorderedCars.toTypedArray()

//        Logger.debug("$oldIds > ${newOrder.map { it }} > ${cars.map { it.idInGroup }}")
    }

    val playerPassengers get() = cars.flatMap { it.playerPassengers }

    open fun destroy() {
        cars.forEach { car ->
            car.destroy()
        }
    }
}