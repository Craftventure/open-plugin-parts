package net.craftventure.core.serverevent

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.inventory.ItemStack


class ProvideLeaveInfoEvent(
    val player: Player
) : Event(!Bukkit.isPrimaryThread()) {
    val data = mutableListOf<Entry>()

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }

    data class Entry(
        val category: Category,
        val description: String,
        val representation: ItemStack,
        val priority: Int = Priority.Default,
        val action: () -> Boolean,
    )

    object Priority {
        const val Default = 1000
    }

    enum class Category(private val category: String) {
        Kart("kart"),
        Minigame("minigame"),
        RideQueue("ridequeue"),
    }
}
