package net.craftventure.core.npc.json

import com.squareup.moshi.JsonClass
import net.craftventure.core.npc.EntityMetadata
import net.minecraft.network.chat.Component

@JsonClass(generateAdapter = true)
data class JsonChatComponentConverter(val value: String) : EntityInteractorJson<Component>() {
    override fun apply(
        interactable: EntityMetadata.Interactable,
        interactor: EntityMetadata.Interactor<Component>
    ) {
        interactable.applyInteractor(
            interactor,
            /*PaperAdventure.asVanilla(PlainTextComponentSerializer.plainText().deserialize(value))
                ?: */Component.Serializer.fromJson(value)!!
        )
    }
}