package net.craftventure.core.metadata

import net.craftventure.annotationkit.GenerateService
import net.craftventure.bukkit.ktx.entitymeta.BasePlayerMetadata
import net.craftventure.bukkit.ktx.entitymeta.PlayerMetaFactory
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.bukkit.ktx.nbs.NbsPlayer
import net.craftventure.core.async.executeAsync
import net.craftventure.core.ktx.util.Logger
import net.craftventure.database.MainRepositoryProvider
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


class GenericPlayerMeta(
    val player: Player
) : BasePlayerMetadata(player) {
    var isNitroBoosting: Boolean = false
        private set
    private var lastFart = System.currentTimeMillis()
    private var sneakingBlockedUntil: Long? = null
    private var manualVehicleExitingBlocked: Long? = null

    var lastGameJoinTime = 0L
    var lastChatPartner: CommandSender? = null
    var lastChatTime = 0L
    var lastExitLocation: Location? = null
    var nbsPlayer: NbsPlayer? = null

    init {
        reloadAll()
    }

    override fun onDestroy() {
        super.onDestroy()
        nbsPlayer?.stop()
    }

    fun updateLastChatTime() {
        lastChatTime = System.currentTimeMillis()
    }

    fun resetLastChatTime() {
        lastChatTime = 0L
    }

    val isSneakingBlocked
        get() = sneakingBlockedUntil?.let {
            val blocked = it > System.currentTimeMillis()
            if (!blocked)
                sneakingBlockedUntil = null
            blocked
        } ?: false

    fun blockSneaking(duration: Duration) {
        val now = System.currentTimeMillis()
        sneakingBlockedUntil = max(sneakingBlockedUntil ?: 0L, now + duration.inWholeMilliseconds)
    }

    fun clearSneakBlocking() {
        sneakingBlockedUntil = null
    }

    val isManualVehicleExitingBlocked
        get() = manualVehicleExitingBlocked?.let {
            val blocked = it > System.currentTimeMillis()
            if (!blocked)
                manualVehicleExitingBlocked = null
            blocked
        } ?: false

    fun blockManualVehicleExiting(duration: Duration = 5.seconds) {
        val now = System.currentTimeMillis()
        manualVehicleExitingBlocked = max(manualVehicleExitingBlocked ?: 0L, now + duration.inWholeMilliseconds)
    }

    fun clearManualVehicleExitingBlocking() {
        manualVehicleExitingBlocked = null
    }

    override fun debugComponent() = Component.text("isNitroBoosting=$isNitroBoosting lastFart=$lastFart")

    fun reloadAll() {
        executeAsync {
            reloadNitroBoostingStatus()
        }
    }

    fun tryFart(): Boolean {
//        Logger.info("Try fart with %s %s", false, lastFart, System.currentTimeMillis());
        if (lastFart < System.currentTimeMillis() - 5000) {
            lastFart = System.currentTimeMillis()
            return true
        }
        return false
    }


    private fun reloadNitroBoostingStatus() {
        try {
            val discordLink = MainRepositoryProvider.discordLinkRepository.getByMinecraftUuid(player.uniqueId)
//                Logger.debug("DiscordLink ${discordLink?.key} ${discordLink?.nitroBoosting}")
            isNitroBoosting = discordLink?.nitroBoosting != null
        } catch (e: Exception) {
            Logger.capture(e)
        }
    }

    @GenerateService
    class Generator : PlayerMetaFactory() {
        override fun create(player: Player) = player.getOrCreateMetadata { GenericPlayerMeta(player) }
    }
}
