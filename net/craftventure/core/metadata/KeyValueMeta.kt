package net.craftventure.core.metadata

import net.craftventure.annotationkit.GenerateService
import net.craftventure.bukkit.ktx.entitymeta.BaseMetadata
import net.craftventure.bukkit.ktx.entitymeta.PlayerMetaFactory
import net.craftventure.bukkit.ktx.entitymeta.getMetadata
import net.craftventure.bukkit.ktx.entitymeta.getOrCreateMetadata
import net.craftventure.core.async.executeAsync
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.repository.PlayerKeyValueRepository
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player


class KeyValueMeta(
    val player: Player
) : BaseMetadata() {
    private val keyValues = HashMap<String, String>()

    override fun debugComponent() = Component.text("keys=${keyValues.entries.joinToString { "${it.key}=${it.value}" }}")

    init {
        executeAsync {
            MainRepositoryProvider.playerKeyValueRepository
                .getValue(player.uniqueId, PlayerKeyValueRepository.KEY_ADMIN_CHAT)?.let { value ->
                    keyValues[PlayerKeyValueRepository.KEY_ADMIN_CHAT] = value
                }
            MainRepositoryProvider.playerKeyValueRepository
                .getValue(player.uniqueId, PlayerKeyValueRepository.KEY_MESSAGE_SPY)?.let { value ->
                    keyValues[PlayerKeyValueRepository.KEY_MESSAGE_SPY] = value
                }
        }
    }

    fun setKeyValue(key: String, value: String?) {
        if (value == null)
            keyValues.remove(key)
        else
            keyValues[key] = value
    }

    fun getKeyValue(key: String): String? {
        return keyValues[key]
    }

    override fun onDestroy() {
        super.onDestroy()
        keyValues.clear()
    }

    @GenerateService
    class Generator : PlayerMetaFactory() {
        override fun create(player: Player) = player.getOrCreateMetadata { KeyValueMeta(player) }
    }

    companion object {
        fun get(player: Player) = player.getMetadata<KeyValueMeta>()
    }
}
