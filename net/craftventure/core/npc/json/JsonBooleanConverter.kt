package net.craftventure.core.npc.json

import com.squareup.moshi.JsonClass
import net.craftventure.core.npc.EntityMetadata

@JsonClass(generateAdapter = true)
data class JsonBooleanConverter(val value: Boolean) : EntityInteractorJson<Boolean>() {
    override fun apply(interactable: EntityMetadata.Interactable, interactor: EntityMetadata.Interactor<Boolean>) {
        interactable.applyInteractor(interactor, value)
    }
}