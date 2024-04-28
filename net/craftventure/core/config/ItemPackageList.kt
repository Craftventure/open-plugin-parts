package net.craftventure.core.config

import com.google.gson.annotations.Expose

data class ItemPackageList(
    @Expose
    val packages: List<ItemPackage> = listOf()
)
