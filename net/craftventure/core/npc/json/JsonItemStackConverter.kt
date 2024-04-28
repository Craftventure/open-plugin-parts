package net.craftventure.core.npc.json

import com.squareup.moshi.JsonClass
import net.craftventure.core.npc.EntityMetadata
import net.craftventure.core.utils.ItemStackUtils
import net.minecraft.world.item.ItemStack
import org.bukkit.Material

@JsonClass(generateAdapter = true)
data class JsonItemStackConverter(
    val itemData: String?,
    val json: String?,
) : EntityInteractorJson<ItemStack>() {
    override fun apply(
        interactable: EntityMetadata.Interactable,
        interactor: EntityMetadata.Interactor<ItemStack>
    ) {
        interactable.applyInteractor(
            interactor,
            json?.let { ItemStackUtils.fromVanillaString(it) }?.let { ItemStack.fromBukkitCopy(it) }
                ?: ItemStack.fromBukkitCopy(org.bukkit.inventory.ItemStack(Material.AIR))
        )
    }
}