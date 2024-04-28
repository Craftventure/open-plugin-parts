package net.craftventure.core.metadata

import net.craftventure.bukkit.ktx.entitymeta.BaseEntityMetadata
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.core.feature.kart.Kart
import net.craftventure.core.feature.minigame.Minigame
import net.craftventure.core.ride.RideInstance
import net.kyori.adventure.text.Component
import org.bukkit.entity.Entity


class TypedInstanceOwnerMetadata @JvmOverloads constructor(
    var ride: RideInstance? = null,
    var kart: Kart? = null,
    var minigame: Minigame? = null,
) : BaseEntityMetadata() {
    fun update(updater: TypedInstanceOwnerMetadata.() -> Unit): TypedInstanceOwnerMetadata {
        updater(this)
        return this
    }

    override fun debugComponent() =
        Component.text("ride=${ride?.id} kart=${kart != null} minigame=${minigame?.internalName}")

    companion object {
        fun Entity.isOwnedByRide(ride: RideInstance) = getMetadata<TypedInstanceOwnerMetadata>()?.ride === ride
        fun Entity.isOwnedByRide() = getMetadata<TypedInstanceOwnerMetadata>()?.ride != null
        fun Entity.isOwnedByKart() = getMetadata<TypedInstanceOwnerMetadata>()?.kart != null
        fun Entity.isOwnedByMinigame() = getMetadata<TypedInstanceOwnerMetadata>()?.minigame != null

        fun Entity.setOwner(ride: RideInstance) = getOrCreateMetadata { TypedInstanceOwnerMetadata() }.update {
            this.ride = ride
        }

        fun Entity.setOwner(kart: Kart) = getOrCreateMetadata { TypedInstanceOwnerMetadata() }.update {
            this.kart = kart
        }

        fun Entity.setOwner(minigame: Minigame) = getOrCreateMetadata { TypedInstanceOwnerMetadata() }.update {
            this.minigame = minigame
        }
    }
}