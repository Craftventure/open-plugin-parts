package net.craftventure.core.listener

import net.craftventure.bukkit.ktx.extension.isVIP
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeSync
import net.craftventure.core.extension.getOperatingRide
import net.craftventure.core.manager.GameModeManager
import net.craftventure.core.manager.TeamsManager
import net.craftventure.core.manager.visibility.VisibilityManager
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.event.node.NodeMutateEvent
import net.luckperms.api.event.user.track.UserDemoteEvent
import net.luckperms.api.event.user.track.UserPromoteEvent
import net.luckperms.api.model.user.User
import org.bukkit.Bukkit
import java.util.function.Consumer

object LuckPermsListener {
    private val promoteListener = Consumer<UserPromoteEvent> {
        //        Logger.debug("User promoted ${it.user.username}")
        updateUser(it.user)
    }
    private val demoteListener = Consumer<UserDemoteEvent> {
        //        Logger.debug("User demoted ${it.user.username}")
        updateUser(it.user)
    }
    private val nodeMutateEvent = Consumer<NodeMutateEvent> {
        it.target.apply {
            if (this is User) {
//                Logger.debug("User mutated $username (via ${it.javaClass.name})")
                updateUser(this)
            }
        }
    }

    private fun updateUser(user: User) {
        if (!PluginProvider.isOnMainThread()) {
            executeSync { updateUser(user) }
            return
        }
        val id = user.uniqueId
        val player = Bukkit.getPlayer(id)
        if (player != null) {
            TeamsManager.update(player)

            GameModeManager.setDefaults(player)
            if (!player.isVIP()) {
                val ride = player.getOperatingRide()
                if (ride != null)
                    CraftventureCore.getOperatorManager().cancelOperating(ride, player)
            }
            VisibilityManager.forceUpdate(player)
            player.updateCommands()
        }
    }

    fun register() {
        val api = LuckPermsProvider.get()
        api.eventBus.subscribe(UserPromoteEvent::class.java, promoteListener)
        api.eventBus.subscribe(UserDemoteEvent::class.java, demoteListener)
        api.eventBus.subscribe(NodeMutateEvent::class.java, nodeMutateEvent)
    }
}
