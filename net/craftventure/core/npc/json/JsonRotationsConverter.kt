package net.craftventure.core.npc.json

import com.squareup.moshi.JsonClass
import net.craftventure.core.npc.EntityMetadata
import net.minecraft.core.Rotations

@JsonClass(generateAdapter = true)
data class JsonRotationsConverter(val x: Float, val y: Float, val z: Float) : EntityInteractorJson<Rotations>() {
    override fun apply(
        interactable: EntityMetadata.Interactable,
        interactor: EntityMetadata.Interactor<Rotations>
    ) {
        interactable.applyInteractor(
            interactor,
            Rotations.createWithoutValidityChecks(x, y, z)
        )
    }
}