package net.craftventure.bukkit.ktx.extension

import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld

//fun <T> ResourceKey<Registry<T>>.getInstanceByMainWorld() =
//    (org.bukkit.Bukkit.getWorlds().first() as CraftWorld).handle.registryAccess().registryOrThrow(this)