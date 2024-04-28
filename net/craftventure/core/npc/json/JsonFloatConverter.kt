package net.craftventure.core.npc.json

import com.squareup.moshi.JsonClass
import net.craftventure.core.npc.EntityMetadata

@JsonClass(generateAdapter = true)
data class JsonFloatConverter(val value: Float) : EntityInteractorJson<Float>() {
    override fun apply(interactable: EntityMetadata.Interactable, interactor: EntityMetadata.Interactor<Float>) {
        interactable.applyInteractor(interactor, value)
    }
}