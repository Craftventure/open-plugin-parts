package net.craftventure.core.npc.json

import com.mojang.brigadier.StringReader
import com.squareup.moshi.JsonClass
import net.craftventure.core.ktx.logging.logException
import net.craftventure.core.npc.EntityMetadata
import net.minecraft.commands.arguments.CompoundTagArgument
import net.minecraft.nbt.CompoundTag

@JsonClass(generateAdapter = true)
data class JsonCompoundConverter(
    val compound: String?,
) : EntityInteractorJson<CompoundTag>() {
    override fun apply(
        interactable: EntityMetadata.Interactable,
        interactor: EntityMetadata.Interactor<CompoundTag>
    ) {
        interactable.applyInteractor(
            interactor,
            compound?.let {
                try {
//                    logcat { "Parsing $it" }
                    CompoundTagArgument.compoundTag().parse(StringReader(it))
                } catch (e: Exception) {
                    logException(e)
                    null
                }
            } ?: CompoundTag()
        )
    }
}