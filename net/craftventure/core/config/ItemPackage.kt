package net.craftventure.core.config

import com.google.gson.annotations.Expose

data class ItemPackage(
    @Expose
    val id: Int = 0,
    @Expose
    val items: List<ItemPackageItem> = listOf(),
    @Expose
    val mail: String? = null
)
