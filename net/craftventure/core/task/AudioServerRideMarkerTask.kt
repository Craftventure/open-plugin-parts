package net.craftventure.core.task

import net.craftventure.audioserver.AudioServer
import net.craftventure.audioserver.packet.AudioPacketPoint
import net.craftventure.audioserver.packet.PacketMarkerAdd
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.ride.trackedride.TrackedRideManager
import java.util.concurrent.TimeUnit

object AudioServerRideMarkerTask {
    private var initialised = false
    fun init() {
        if (initialised) return
        initialised = true

        CraftventureCore.getScheduledExecutorService().scheduleAtFixedRate({
            executeAsync {
                update()
            }
        }, 1, 1, TimeUnit.SECONDS)
    }

    private fun update() {
        val parkTrainTrains = TrackedRideManager.getTrackedRide("parktrain")!!.rideTrains
        val markers = parkTrainTrains.map { rideTrain ->
            val location = rideTrain.cars.first().location
            PacketMarkerAdd.MapMarker(
                id = "parktrain_${rideTrain.trainId}",
                group = "parktrain",
                x = location.x,
                z = location.z,
                popupName = "Park Train",
                type = "twemoji",
                url = "\uD83D\uDE82",
                size = AudioPacketPoint(32.0, 32.0),
                anchor = AudioPacketPoint(16.0, 16.0),
                popupAnchor = AudioPacketPoint(0.0, -16.0),
                zIndex = location.y.toInt()
            )
        }
        val packet = PacketMarkerAdd(PacketMarkerAdd.Mode.SET, "parktrain", markers.toSet())
        AudioServer.broadcast(packet)
    }
}