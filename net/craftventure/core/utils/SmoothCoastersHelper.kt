package net.craftventure.core.utils

import me.m56738.smoothcoasters.api.SmoothCoastersAPI
import net.craftventure.core.CraftventureCore

// No it's not enabled on the server. Did have a branch where I replaced all trackedrides from vector to matrices, but it never made it to live

object SmoothCoastersHelper {
    val api by lazy { SmoothCoastersAPI(CraftventureCore.getInstance()) }

    fun init() {
        api
    }
}