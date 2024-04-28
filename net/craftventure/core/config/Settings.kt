package net.craftventure.core.config

import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.area.Area
import net.craftventure.bukkit.ktx.area.CombinedArea
import net.craftventure.bukkit.ktx.extension.isCrew
import net.craftventure.core.ktx.json.MoshiBase
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.io.File
import java.util.concurrent.TimeUnit


@JsonClass(generateAdapter = true)
data class Settings(
    val tebexSecret: String,
    val apiMaxifotoRoot: String? = null,
    val aegisWebhookUrl: String? = null,
    val coinBoosterWebhookUrl: String? = null,
    val errorWebhook: String,
    val afkTimeout: Int = 180,
    val afkWalkTimeout: Int = 480,
    val afkOperatorTimeout: Int = 120,
    val databaseIp: String,
    val databasePort: String,
    val databaseName: String,
    val databaseUser: String,
    val databasePassword: String,
    var maxGuestsOverride: Int = 0,
    val vipReservedSlots: Int = 0,
    val afkFreeSlots: Int = 0,
    val crewReservedSlots: Int = 0,
    val borderConfig: Array<BorderConfig>? = null,
    val audioServerWebsite: String? = null,
    val audioServerSocket: String? = null,
    val coasterTickTime: Int = 50,
    val isDebugCoasterBogies: Boolean = false,
    val isDebugCoasterSeatLocations: Boolean = false,
    val isDebugCoasterOrientation: Boolean = false,
    val sentryDsn: String? = null
) {
    fun borderFor(player: Player) = if (player.isCrew()) crewBorder else border

    val crewBorder: Area by lazy {
        CombinedArea(*borderConfig!!.map { it.area }.toTypedArray())
    }

    val border: Area by lazy {
        CombinedArea(*borderConfig!!.filter { !it.crewOnly }.map { it.area }.toTypedArray())
    }

    /*Math.min(*//*, Bukkit.getMaxPlayers())*/ val maxGuestCount: Int
        get() = if (maxGuestsOverride > 0) {
            maxGuestsOverride
        } else Bukkit.getMaxPlayers()

    fun getAfkTimeout(asUnit: TimeUnit): Long {
        return asUnit.convert(afkTimeout.toLong(), TimeUnit.SECONDS)
    }

    fun getAfkWalkTimeout(asUnit: TimeUnit): Long {
        return asUnit.convert(afkWalkTimeout.toLong(), TimeUnit.SECONDS)
    }

    fun getAfkOperatorTimeout(asUnit: TimeUnit): Long {
        return asUnit.convert(afkOperatorTimeout.toLong(), TimeUnit.SECONDS)
    }

    companion object {
        fun fromFile(file: File) = MoshiBase.moshi.adapter(Settings::class.java).fromJson(file.readText())/*.apply {
            logcat { this.toString() }
        }*/
    }
}
