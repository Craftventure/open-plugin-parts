package net.craftventure.core.npc.json

import com.squareup.moshi.JsonClass
import net.craftventure.core.ktx.extension.toOptional
import net.craftventure.core.npc.EntityMetadata
import net.minecraft.network.chat.Component
import java.util.*

@JsonClass(generateAdapter = true)
data class JsonOptionalChatComponentConverter(val value: String) : EntityInteractorJson<Optional<Component>>() {
    override fun apply(
        interactable: EntityMetadata.Interactable,
        interactor: EntityMetadata.Interactor<Optional<Component>>
    ) {
        interactable.applyInteractor(
            interactor,
            Component.Serializer.fromJson(value).toOptional()
        )
    }
}