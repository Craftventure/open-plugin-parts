package net.craftventure.core.map.renderer

import net.craftventure.core.CraftventureCore
import java.io.File

class DefaultBackgroundHolders {
    val single by lazy {
        MapManager.instance.getImageHolder(
            File(
                CraftventureCore.getInstance().dataFolder,
                "data/maps/highscore/background_single.png"
            )
        )
    }
    val top by lazy {
        MapManager.instance.getImageHolder(
            File(
                CraftventureCore.getInstance().dataFolder,
                "data/maps/highscore/background_top.png"
            )
        )
    }
    val center by lazy {
        MapManager.instance.getImageHolder(
            File(
                CraftventureCore.getInstance().dataFolder,
                "data/maps/highscore/background_center.png"
            )
        )
    }
    val bottom by lazy {
        MapManager.instance.getImageHolder(
            File(
                CraftventureCore.getInstance().dataFolder,
                "data/maps/highscore/background_bottom.png"
            )
        )
    }

    fun getFor(page: Int, pageCount: Int) = when {
        pageCount <= 1 -> single
        page == 0 -> top
        page == pageCount - 1 -> bottom
        else -> center
    }
}