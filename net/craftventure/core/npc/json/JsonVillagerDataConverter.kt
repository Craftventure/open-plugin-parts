package net.craftventure.core.npc.json

import com.squareup.moshi.JsonClass
import net.craftventure.core.npc.EntityMetadata
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.npc.VillagerData
import org.bukkit.entity.Villager

@JsonClass(generateAdapter = true)
data class JsonVillagerDataConverter(
    val villagerType: Villager.Type = Villager.Type.PLAINS,
    val profession: Villager.Profession = Villager.Profession.NONE,
) : EntityInteractorJson<VillagerData>() {
    override fun apply(
        interactable: EntityMetadata.Interactable,
        interactor: EntityMetadata.Interactor<VillagerData>
    ) {
        interactable.applyInteractor(
            interactor,
            VillagerData(
                BuiltInRegistries.VILLAGER_TYPE.byId(villagerType.ordinal),
                BuiltInRegistries.VILLAGER_PROFESSION.byId(profession.ordinal),
                0
            )
        )
    }
}