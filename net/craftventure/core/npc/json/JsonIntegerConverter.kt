package net.craftventure.core.npc.json

import com.squareup.moshi.JsonClass
import net.craftventure.core.npc.EntityMetadata

@JsonClass(generateAdapter = true)
data class JsonIntegerConverter(
    val value: Int,
    val any: List<Int>? = null,
) : EntityInteractorJson<Int>() {
    override fun apply(interactable: EntityMetadata.Interactable, interactor: EntityMetadata.Interactor<Int>) {
        if (!any.isNullOrEmpty()) {
            interactable.applyInteractor(interactor, any.random())
        } else
            interactable.applyInteractor(interactor, value)
    }
}