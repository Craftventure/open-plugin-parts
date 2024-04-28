package net.craftventure.core.npc.json

import com.squareup.moshi.JsonClass
import net.craftventure.core.npc.EntityMetadata
import net.minecraft.world.entity.Pose

@JsonClass(generateAdapter = true)
data class JsonPoseConverter(val pose: org.bukkit.entity.Pose) : EntityInteractorJson<Pose>() {
    override fun apply(interactable: EntityMetadata.Interactable, interactor: EntityMetadata.Interactor<Pose>) {
        interactable.applyInteractor(interactor, Pose.values()[pose.ordinal])
    }
}