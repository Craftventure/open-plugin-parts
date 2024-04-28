package net.craftventure.bukkit.ktx.manager

import net.craftventure.bukkit.ktx.event.FeatureToggledEvent
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVTextColor
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.*

object FeatureManager {

    private val enabledFeatures = hashSetOf<Feature>()

    fun sendFeatureList(commandSender: CommandSender) {
        var component = Component.text("", CVTextColor.serverNotice)
        Feature.values().forEach { feature ->
            component += addToggle(feature, enabledFeatures.contains(feature))
        }
        commandSender.sendMessage(component)
    }

    @JvmStatic
    fun reload() {
//            Logger.debug(" Reloading feature toggles")
        enabledFeatures.clear()
        val file = File(PluginProvider.getInstance().dataFolder, "features.yml")
        if (file.exists()) {
            try {
                val yaml = YamlConfiguration.loadConfiguration(file)
                Feature.values().forEach { feature ->
                    val enabled = yaml.getBoolean(feature.name.lowercase(Locale.getDefault()), true)
//                    Logger.debug("${feature.displayName} => $enabled")
                    if (enabled)
                        enabledFeatures.add(feature)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                enabledFeatures.addAll(Feature.values())
//                    Logger.debug(" Reloading feature error")
            }
        } else {
//                Logger.debug(" Reloading feature fallback")
            enabledFeatures.addAll(Feature.values())
        }
    }

    private fun addToggle(
        feature: Feature,
        currentlyEnabled: Boolean
    ): Component {
        return Component.text(
            "[${feature.displayName}]  ",
            if (currentlyEnabled) NamedTextColor.GREEN else NamedTextColor.RED
        )
            .hoverEvent(Component.text("Click to toggle", CVTextColor.CHAT_HOVER))
            .clickEvent(ClickEvent.runCommand("/featuretoggle ${feature.name}"))
    }

    fun enableFeature(feature: Feature): Boolean {
        if (enabledFeatures.add(feature)) {
            Bukkit.getPluginManager().callEvent(FeatureToggledEvent(feature, true))
            return true
        }
        return false
    }

    fun disableFeature(feature: Feature): Boolean {
        if (enabledFeatures.remove(feature)) {
            Bukkit.getPluginManager().callEvent(FeatureToggledEvent(feature, false))
            return true
        }
        return false
    }

    fun toggleFeature(feature: Feature): Boolean {
        val enabled = enabledFeatures.contains(feature)
        return if (enabled) {
            disableFeature(feature)
        } else {
            enableFeature(feature)
        }
    }

    fun isFeatureEnabled(feature: Feature) = enabledFeatures.contains(feature)

    enum class Feature(
        val displayName: String
    ) {
        KART_SPAWN_AS_USER("!!Spawn karts (user)"),
        SPATIAL_SOUNDS("Spatial sounds"),
        BALLOON_ACTIVATE("!!Activate balloons"),
        CLOTHING_PARTICLES("!!Clothing effects"),
        SKATES_ENABLED("!!Skates (ice)"),
        VIEW_OTHER_PLAYERS("View other players"),
        MINIGAME_JOIN("Join minigames"),
        AUDIOSERVER_TRACKING("AudioServer (tracking)"),
        AUDIOSERVER_UPDATING("AudioServer (updating areas)"),
        SCENE_ACTION_SCHEMATIC_PASTING("Scenes (pasting schematics)"),
        AUTOMATED_SCHEMATIC_PASTING("All schematic pastes"),
        SHOPS_PRESENTER("Shops (presenter)"),
        JUMP_PUZZLE_JOIN("Jump puzzle (join)"),
        DRESSING_ROOM("Dressing room (track)"),
    }
}