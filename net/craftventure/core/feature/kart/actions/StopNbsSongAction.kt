package net.craftventure.core.feature.kart.actions

import net.craftventure.core.feature.kart.Kart
import net.craftventure.core.feature.kart.KartAction
import org.bukkit.entity.Player

class StopNbsSongAction() : KartAction {
    override fun execute(kart: Kart, type: KartAction.Type, target: Player?) {
        kart.metadata["nbssong"]?.onDestroy()
    }
}
