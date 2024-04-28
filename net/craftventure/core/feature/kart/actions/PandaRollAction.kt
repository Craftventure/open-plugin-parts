package net.craftventure.core.feature.kart.actions

import net.craftventure.core.feature.kart.Kart
import net.craftventure.core.feature.kart.KartAction
import net.craftventure.core.feature.kart.addon.KartAddon
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.npc.EntityMetadata
import net.craftventure.core.ride.trackedride.CoasterMathUtils
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player

class PandaRollAction : KartAddon(), KartAction {
    private var isRolling = false
        set(value) {
            field = value
            Logger.debug("Rolling $value")
        }
    private val limit = CoasterMathUtils.kmhToBpt(2.0)
    private var hasInitialised = false

    private var lastApply = 0

    override fun onPostUpdate(kart: Kart) {
        super.onPostUpdate(kart)

        val seat = kart.npcs.firstOrNull { it.settings.entityType == EntityType.PANDA } ?: return

        if (!hasInitialised) {
            hasInitialised = true
            seat.entity!!.applyInteractor(
                EntityMetadata.Panda.mainGeneId,
                0x03,
            )
            seat.entity!!.applyInteractor(
                EntityMetadata.Ageable.baby,
                true,
            )
        }

        if (isRolling) {
            if (lastApply == 30) {
                seat.entity!!.applyInteractor(
                    EntityMetadata.Panda.dataIdFlags,
                    0x00,
                )

                if (kart.currentSpeed < limit) {
                    if (isRolling) {
                        isRolling = false
                    }
                }
            } else if (lastApply >= 31) {
                seat.entity!!.applyInteractor(
                    EntityMetadata.Panda.dataIdFlags,
                    0x04,
                )
                lastApply = 0
            }
            lastApply++
        }
    }

    override fun execute(kart: Kart, type: KartAction.Type, target: Player?) {
//        Logger.debug("${kart.currentSpeed.format(2)} > ${limit.format(2)} = ${kart.currentSpeed > limit}")
        if (kart.currentSpeed > limit) {
            if (!isRolling) {
                isRolling = true

                kart.player.leaveVehicle()

                val seat = kart.npcs.firstOrNull { it.settings.entityType == EntityType.PANDA }
                if (seat != null) {
                    lastApply = 0
                    seat.entity!!.applyInteractor(
                        EntityMetadata.Panda.dataIdFlags,
                        0x04,
                    )
                }
            }
        }
    }
}
