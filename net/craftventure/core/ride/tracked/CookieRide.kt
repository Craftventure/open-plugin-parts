package net.craftventure.core.ride.tracked

import net.craftventure.audioserver.api.AudioServerApi
import net.craftventure.bukkit.ktx.MaterialConfig
import net.craftventure.bukkit.ktx.area.CombinedArea
import net.craftventure.bukkit.ktx.area.SimpleArea
import net.craftventure.bukkit.ktx.extension.open
import net.craftventure.bukkit.ktx.manager.MessageBarManager
import net.craftventure.bukkit.ktx.manager.MessageBarManager.Message
import net.craftventure.bukkit.ktx.manager.MessageBarManager.display
import net.craftventure.bukkit.ktx.util.ChatUtils
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeSync
import net.craftventure.core.effect.EffectManager
import net.craftventure.core.effect.ItemEmitter
import net.craftventure.core.feature.maxifoto.MaxiFoto
import net.craftventure.core.ktx.util.TimeUtils
import net.craftventure.core.ride.trackedride.*
import net.craftventure.core.ride.trackedride.car.DynamicSeatedRideCar
import net.craftventure.core.ride.trackedride.car.seat.ArmorStandSeat
import net.craftventure.core.ride.trackedride.ride.OperableCoasterTrackedRide
import net.craftventure.core.ride.trackedride.segment.StationSegment
import net.craftventure.core.ride.trackedride.segment.TransportSegment
import net.craftventure.core.ride.trackedride.train.CoasterRideTrain
import net.craftventure.core.script.ScriptManager
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds


class CookieRide private constructor(trackedRide: TrackedRide) {
    companion object {
        private var cookieRide: CookieRide? = null
        private val CAR_LENGTH = 2.8
        private val SEAT_SIDE_OFFSET = 0.35
        private val SEAT_HEIGHT_OFFSET = -0.85 + 1.5

        fun get(): CookieRide {
            if (cookieRide == null) {
                val machineEmitters = arrayOf(
                    Location(Bukkit.getWorld("world"), ),
                    Location(Bukkit.getWorld("world"), ),
                    Location(Bukkit.getWorld("world"), ),
                    Location(Bukkit.getWorld("world"), ),
                    Location(Bukkit.getWorld("world"), )
                )
                val trainEmitters = arrayOf(
                    Location(Bukkit.getWorld("world"), ),
                    Location(Bukkit.getWorld("world"), ),
                    Location(Bukkit.getWorld("world"), )
                )
                for ((index, machineEmitter) in machineEmitters.withIndex()) {
                    EffectManager.add(
                        ItemEmitter(
                            "cookiemachine_machine_emitter_$index",
                            machineEmitter,
                            ItemStack(Material.COOKIE),
                            randomOffset = Vector(0.2, 0.0, 0.2),
                            startVelocity = Vector(0.0, 0.4, 0.0),
                            randomVelocity = Vector(0.2, 0.05, 0.2),
                            spawnRate = -0.1f,
                            lifeTimeTicksMin = 30,
                            lifeTimeTicksMax = 40
                        )
                    )
                }
                for ((index, trainEmitter) in trainEmitters.withIndex()) {
                    EffectManager.add(
                        ItemEmitter(
                            "cookiemachine_train_emitter_$index",
                            trainEmitter,
                            ItemStack(Material.COOKIE),
                            randomOffset = Vector(0.05, 0.0, 0.05),
                            startVelocity = Vector(0.0, 0.01, 0.0),
                            randomVelocity = Vector(0.05, 0.0, 0.05),
                            lifeTimeTicksMin = 5,
                            lifeTimeTicksMax = 10,
                            spawnRate = -0.2f
                        )
                    )
                }

                val show = ShowController()
                Bukkit.getScheduler().scheduleSyncRepeatingTask(CraftventureCore.getInstance(), { show.update() }, 1, 1)

                val coasterArea = SimpleArea("world", -22.0, 30.0, -394.0, 58.0, 69.0, -295.0)
                val trackedRide = OperableCoasterTrackedRide(
                    "cookiefactory",
                    coasterArea,
                    Location(Bukkit.getWorld("world"), 35.35, 42.00, -346.60, 235.05f, 0.15f),
                    "ride_cookiefactory",
                    "cookiefactory"
                )

                trackedRide.setOperatorArea(SimpleArea("world", 19.0, 41.0, -359.0, 30.0, 48.0, -347.0))
                initTrack(trackedRide)

                var segment = trackedRide.getSegmentById("track")!!
                var distance = segment.length - 10
                val beginSegment = segment
                for (i in 0 until 5) {
                    val rideTrain = CoasterRideTrain(beginSegment, distance)

                    val dynamicSeatedRideCar = DynamicSeatedRideCar("cookiefactory", CAR_LENGTH)
                    dynamicSeatedRideCar.carFrontBogieDistance = -1.0
                    dynamicSeatedRideCar.carRearBogieDistance = -CAR_LENGTH + 0.5

                    dynamicSeatedRideCar.addSeat(ArmorStandSeat(0.0, 0.5, -1.6, false, "cookiefactory").apply {
                        setModel(MaterialConfig.COOKIE_RIDE)
                    })

                    dynamicSeatedRideCar.addSeat(
                        ArmorStandSeat(
                            SEAT_SIDE_OFFSET,
                            SEAT_HEIGHT_OFFSET - 0.2,
                            -1.8,
                            true,
                            "cookiefactory"
                        )
                    )
                    dynamicSeatedRideCar.addSeat(
                        ArmorStandSeat(
                            -SEAT_SIDE_OFFSET,
                            SEAT_HEIGHT_OFFSET - 0.2,
                            -1.8,
                            true,
                            "cookiefactory"
                        )
                    )

                    dynamicSeatedRideCar.addSeat(
                        ArmorStandSeat(
                            SEAT_SIDE_OFFSET,
                            SEAT_HEIGHT_OFFSET,
                            -2.85,
                            true,
                            "cookiefactory"
                        )
                    )
                    dynamicSeatedRideCar.addSeat(
                        ArmorStandSeat(
                            -SEAT_SIDE_OFFSET,
                            SEAT_HEIGHT_OFFSET,
                            -2.85,
                            true,
                            "cookiefactory"
                        )
                    )

                    rideTrain.addCar(dynamicSeatedRideCar)
                    trackedRide.addTrain(rideTrain)

                    distance -= CAR_LENGTH
                    while (distance < 0) {
                        segment = segment.previousTrackSegment!!
                        distance += segment.length
                    }
                }

                trackedRide.initialize()
                trackedRide.pukeRate = 0.0
                cookieRide = CookieRide(trackedRide)
            }
            return cookieRide!!
        }

        private fun initTrack(trackedRide: TrackedRide) {
            val speed = 5.0
            val station = StationSegment(
                "station",
                "Station",
                trackedRide,
                speed,
                speed,
                2.1
            )
            station.leaveMode = TrackSegment.LeaveMode.LEAVE_TO_SEAT_WHEN_CAN_ENTER
            station.holdDistance = 7.5 + (CAR_LENGTH / 2.0)
            station.setKeepRollingTime(30.seconds)
            station.setDispatchIntervalTime(25.seconds)
            station.setAutoDispatchTime(60.seconds)
            station.setOnStationGateListener { open ->
                Location(trackedRide.area.world, 26.0, 42.0, -355.0).block.open(open)
                Location(trackedRide.area.world, 25.0, 42.0, -356.0).block.open(open)
            }
            station.add(
            )
            station.setOnStationStateChangeListener { newState, oldState ->
                if (newState == StationSegment.StationState.DISPATCHING) {
                    val rideTrain = station.anyRideTrainOnSegment
                    rideTrain?.setOnboardSynchronizedAudio("cookiefactory_onride", System.currentTimeMillis())
                }
            }

            val track1 = TransportSegment(
                "track",
                "Track",
                trackedRide,
                CoasterMathUtils.kmhToBpt(speed),
                CoasterMathUtils.kmhToBpt(1.8),
                CoasterMathUtils.kmhToBpt(speed),
                CoasterMathUtils.kmhToBpt(1.8)
            )
            track1.blockSection(true)
            track1.setBlockType(TrackSegment.BlockType.CONTINUOUS, 0.0, 0.0)
            track1.add(object : TrackSegment.DistanceListener(215.0) {
                override fun onTargetHit(rideCar: RideCar) {
                    rideCar.attachedTrain.eject()
                }
            })
            track1.add(
            )

            track1.add(object : TrackSegment.DistanceListener(175.0) {
                var car = 0

                override fun onTargetHit(triggeringCar: RideCar) {
                    val names = arrayOfNulls<Player>(4)
                    for (i in names.indices)
                        names[i] = null

                    var index = 0
                    for (rideCar in triggeringCar.attachedTrain.cars) {
                        for (entity in rideCar.getMaxifotoPassengerList()) {
                            if (index < names.size) {
                                if (entity is Player) {
                                    names[index] = entity
                                }
                                index++
                            }
                        }
                    }

                    var onlyNulls = true
                    for (name in names) {
                        if (name != null) {
                            onlyNulls = false
                            break
                        }
                    }

                    val renderSettings = MaxiFoto.RenderSettings("cookiefactory", names)
                    if (!onlyNulls) {
                        renderSettings.offset = car
                        MaxiFoto.render(renderSettings)
                        car++
                        if (car >= 2) {
                            car = 0
                        }
                    }
                }
            })


            station.setNextTrackSegmentRetroActive(track1)
                .setNextTrackSegmentRetroActive(station)

            trackedRide.addTrackSections(station, track1)
        }
    }

    class ShowController {
        private val area1 = SimpleArea("world", )
        private val area2 = SimpleArea("world", )
        private val preshowExitLocation = Location(Bukkit.getWorld("world"), )
        private val preshowArea = CombinedArea(area1, area2)
        private var state = ShowState.IDLE
            set(value) {
                if (field != value) {
                    field = value
                    //                Logger.consoleAndIngame("GonBao preshow state " + newState.name());
                    if (this.state == ShowState.IDLE) {
                        ScriptManager.stop("cookiefactory", "preshow")
                        ScriptManager.stop("cookiefactory", "preshow_lights")
                        stop()
                    }
                    inStateSince = System.currentTimeMillis()

                    openEntrance(state == ShowState.IDLE)
                    openExit(state == ShowState.EXIT_PERIOD)

                    if (state == ShowState.SHOW) {
                        executeSync((20 * 14.5).toLong()) {
                            ScriptManager.start("cookiefactory", "preshow")
                        }
                        ScriptManager.start("cookiefactory", "preshow_lights")
                    }
                }
            }
        private var inStateSince = System.currentTimeMillis()
        private var gatheringPlayerStartingTime: Long = 0

        private val doorEntrance1 = Location(Bukkit.getWorld("world"), )
        private val doorEntrance2 = Location(Bukkit.getWorld("world"), )

        private val doorExitLeft1 = Location(Bukkit.getWorld("world"), )
        private val doorExitLeft2 = Location(Bukkit.getWorld("world"), )

        private val doorExitRight1 = Location(Bukkit.getWorld("world"), )
        private val doorExitRight2 = Location(Bukkit.getWorld("world"), )

        private var shouldStartAudio = false

        init {
            openEntrance(true)
            openExit(false)
        }

        private fun openEntrance(open: Boolean) {
            doorEntrance1.block.open(open)
            doorEntrance2.block.open(open)
        }

        private fun openExit(open: Boolean) {
            doorExitLeft1.block.open(open)
            doorExitLeft2.block.open(open)
            doorExitRight1.block.open(open)
            doorExitRight2.block.open(open)
        }

        fun update() {
            if (state == ShowState.IDLE) {
                var containsPlayers = false
                for (player in Bukkit.getOnlinePlayers()) {
                    val inArea = preshowArea.isInArea(player)
                    if (inArea) {
                        display(
        player,
        Message(
            id = ChatUtils.ID_RIDE,
            text = Component.text(
                                "Please wait a few seconds for the man, the god, the legend Joeywp to introduce himself...",
                                CVTextColor.serverNotice
                            ),
            type = MessageBarManager.Type.RIDE,
            untilMillis = TimeUtils.secondsFromNow(1.0),
        ),
        replace = true,
    )
                        containsPlayers = true
                    }
                }
                if (containsPlayers) {
                    if (gatheringPlayerStartingTime == -1L) {
                        gatheringPlayerStartingTime = System.currentTimeMillis()
                    }
                    if (!shouldStartAudio) {
                        AudioServerApi.enable("cookiefactory_preshow")
                        AudioServerApi.sync("cookiefactory_preshow", System.currentTimeMillis())
                        shouldStartAudio = true
                    }
                } else {
                    gatheringPlayerStartingTime = -1
                    shouldStartAudio = false
                }
                if (gatheringPlayerStartingTime > 0) {
                    if (gatheringPlayerStartingTime < System.currentTimeMillis() - 10000) {
                        //                        Logger.console("Starting gonbao because a player was inside the area for 5 secs");
                        state = ShowState.SHOW
                    }
                }
            } else if (state == ShowState.SHOW) {
                if (inStateSince < System.currentTimeMillis() - 60000) {
                    state = ShowState.EXIT_PERIOD
                }
            } else if (state == ShowState.EXIT_PERIOD) {
                if (inStateSince < System.currentTimeMillis() - 10000) {
                    //                    Logger.console("GonBao preshow resetting, has ride started? " + GonBaoCastle.this.isRunning());
                    state = ShowState.IDLE
                }
            }
        }

        private fun stop() {
            for (player in Bukkit.getOnlinePlayers()) {
                if (preshowArea.isInArea(player)) {
                    player.teleport(preshowExitLocation, PlayerTeleportEvent.TeleportCause.PLUGIN)
                }
            }

            AudioServerApi.disable("cookiefactory_preshow")
        }

        private enum class ShowState {
            IDLE, SHOW, EXIT_PERIOD
        }
    }
}
