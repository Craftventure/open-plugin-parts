package net.craftventure.temporary

import net.craftventure.bukkit.ktx.entitymeta.MetaAnnotations
import net.craftventure.bukkit.ktx.manager.TitleManager
import net.craftventure.bukkit.ktx.manager.TitleManager.displayTitle
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.chat.bungee.extension.asPlainText
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.api.CvApi
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeMain
import net.craftventure.core.extension.toName
import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.core.metadata.ManagedSideScoreBoard
import net.craftventure.core.task.ActiveMoneyRewardTask
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.describe
import net.craftventure.database.generated.cvdata.tables.pojos.ActiveServerCoinBooster
import net.craftventure.database.repository.BaseIdRepository
import net.craftventure.database.type.BankAccountType
import net.kyori.adventure.text.Component
import okhttp3.MultipartBody
import okhttp3.Request
import org.bukkit.Bukkit

class ActiveServerCoinBoosterActivationListener : BaseIdRepository.Listener<ActiveServerCoinBooster>() {
    override fun onMerge(item: ActiveServerCoinBooster) {
        super.onMerge(item)
        onInsert(item)
    }

    override fun onInsert(item: ActiveServerCoinBooster) {
        ActiveMoneyRewardTask.onActivated(item)

        val booster = MainRepositoryProvider.coinBoosterRepository.findCached(item.boosterId!!) ?: return
        val duration = booster.duration

        val uuid = item.activator
        val name = uuid!!.toName()
        val message = Component.text(
            "\nA serverwide coin booster ${booster.describe()} was activated by ${uuid.toName()} that will last for ${
                DateUtils.format(
                    duration!!.toLong(),
                    "?"
                )
            }\n ",
            CVTextColor.serverNotice
        )

        Bukkit.getServer().sendMessage(message)

        executeMain {
            for (player in Bukkit.getOnlinePlayers()) {
                MetaAnnotations.getMetadata(player, ManagedSideScoreBoard::class.java)
                    ?.updateCoinDisplay(BankAccountType.VC)
                if (!player.isInsideVehicle)
                    player.displayTitle(
                        TitleManager.TitleData.ofTicks(
                            id = "server_coin_booster",
                            type = TitleManager.Type.CoinBooster,
                            title = Translation.COINBOOSTER_SERVER_ACTIVATED_TITLE.getTranslation(player),
                            subtitle = Translation.COINBOOSTER_SERVER_ACTIVATED_SUBTITLE.getTranslation(
                                player, name,
                                booster.describe()
                            ),
                            fadeInTicks = 20,
                            stayTicks = 5 * 20,
                            fadeOutTicks = 20,
                        ),
                        replace = true,
                    )
            }
        }

        if (!PluginProvider.isTestServer())
            try {
                val call = CvApi.okhttpClient.newCall(
                    Request.Builder()
                        .url(CraftventureCore.getSettings().coinBoosterWebhookUrl!!)
                        .post(
                            MultipartBody.Builder()
                                .setType(MultipartBody.FORM)
                                .addFormDataPart("content", message.asPlainText().trim())
                                .build()
                        )
                        .build()
                )
                executeAsync { call.execute().close() }
            } catch (e: Exception) {
            }
    }
}