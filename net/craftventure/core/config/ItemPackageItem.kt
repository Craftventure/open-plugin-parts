package net.craftventure.core.config

import com.google.gson.annotations.Expose

data class ItemPackageItem(
    @Expose
    val id: String? = null,
    @Expose
    val amount: Int = 1
)
