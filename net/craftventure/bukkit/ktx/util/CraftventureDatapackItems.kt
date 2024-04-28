package net.craftventure.bukkit.ktx.util

import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import org.bukkit.World
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld

object CraftventureDatapackItems {
    fun getCvBiomeNames(world: World) =
        (world as CraftWorld).handle.registryAccess().registryOrThrow(Registries.BIOME)
            .keySet().filter { it.namespace == "craftventure" }

    fun getCvBiome(world: World, name: String) =
        (world as CraftWorld).handle.registryAccess().registryOrThrow(Registries.BIOME)
            .get(ResourceLocation.of("craftventure:$name", ':'))
}