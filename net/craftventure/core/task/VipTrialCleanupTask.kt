package net.craftventure.core.task

import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.executeAsync
import net.craftventure.core.extension.toName
import net.craftventure.core.ktx.util.Logger
import net.craftventure.database.MainRepositoryProvider
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.node.NodeEqualityPredicate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object VipTrialCleanupTask {
    private var initialised = false
    fun init() {
        if (initialised) return
        initialised = true

        CraftventureCore.getScheduledExecutorService().scheduleAtFixedRate({
            executeAsync {
                cleanup()
            }
        }, 1, 60, TimeUnit.MINUTES)
    }

    private fun cleanup() {
        val api = LuckPermsProvider.get()
        val vipGroup = api.nodeBuilderRegistry.forInheritance().group("vip").build()
        val repository = MainRepositoryProvider.vipTrialRepository
        val invites = repository.getAllInvites() ?: return

//        Logger.info("Checking VIP trials...")

        val now = LocalDateTime.now()
        val dateOfExpiration = LocalDateTime.now().minusDays(10)
        for (invite in invites) {
            var remove = false

            if (invite.usedAt == null && invite.createdAt!!.isBefore(dateOfExpiration)) {
                remove = true
            }

            if (invite.usedAt != null) {
                val dateOfResetAfterUsage = invite.usedAt!!.plusDays(90)
                if (dateOfResetAfterUsage.isBefore(now)) {
                    remove = true
                }
            }

            val uuid = invite.invitee
//            Logger.debug("Loading user $uuid")
            val user = api.userManager.getUser(uuid!!) ?: api.userManager.loadUser(uuid).join()
//            Logger.debug("Loaded user $uuid")

            if (user != null) {
                val userIsVip = user.data().contains(vipGroup, NodeEqualityPredicate.EXACT).asBoolean()
                if (userIsVip) {
                    remove = true
                }
            }
//            Logger.debug("Checked if VIP user $uuid")
            if (remove) {
                val removes = repository.removeInvite(invite.sender!!, invite.invitee!!, force = true)
                Logger.info("Remove user ${invite.invitee?.toName()} from VIPtrial = $removes")
            }
        }

//        Logger.info("Checking VIP trials done!")
    }
}