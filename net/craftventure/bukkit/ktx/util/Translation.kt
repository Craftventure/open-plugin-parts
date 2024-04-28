package net.craftventure.bukkit.ktx.util

import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import java.util.*

enum class Translation private constructor(val value: String) {
    ACHIEVEMENT_REWARD("achievement.reward"),
    ACHIEVEMENT_REWARD_WITHOUT_DESCRIPTION("achievement.reward_without_description"),
    AUDIOSERVER_CONNECTED("audioserver.connected"),
    AUDIOSERVER_DISCONNECTED("audioserver.disconnected"),
    AUDIOSERVER_REQUIRES_CONNECTION("audioserver.requires_connection"),
    AUDIOSERVER_VOLUME_CHANGED("audioserver.volume_changed"),
    AUDIOSERVER_VOLUME_CURRENT("audioserver.volume_current"),
    AUDIOSERVER_VOLUME_FORMAT("audioserver.volume_format"),
    AUDIOSERVER_VOLUME_USAGE("audioserver.volume_usage"),
    BALLOONS_TIMEOUT("balloons.timeout"),
    CASINO_SLOTMACHINE_BROKEN("casino.slotmachine.broken"),
    CASINO_SLOTMACHINE_FAILED_TO_PAY_MACHINE("casino.slotmachine.failed_to_pay_machine"),
    CASINO_SLOTMACHINE_LOST("casino.slotmachine.lost"),
    CASINO_SLOTMACHINE_MACHINE_IN_USE("casino.slotmachine.machine_in_use"),
    CASINO_SLOTMACHINE_NOT_ENOUGH_VENTURECOINS("casino.slotmachine.not_enough_venturecoins"),
    CASINO_SLOTMACHINE_SPENT("casino.slotmachine.spent"),
    CASINO_SLOTMACHINE_WAIT_UNTIL_RESET("casino.slotmachine.wait_until_reset"),
    CASINO_SLOTMACHINE_WON("casino.slotmachine.won"),
    CASINO_WOF_INPUT("casino.wof.input"),
    CASINO_WOF_INPUT_WINTER("casino.wof.input_winter"),
    CASINO_WOF_JOIN("casino.wof.join"),
    CASINO_WOF_LEAVE("casino.wof.leave"),
    CASINO_WOF_LOST("casino.wof.lost"),
    CASINO_WOF_MAX_INPUT("casino.wof.max_input"),
    CASINO_WOF_NOT_ENOUGH_VENTURECOINS("casino.wof.not_enough_venturecoins"),
    CASINO_WOF_PROCESSED("casino.wof.processed"),
    CASINO_WOF_SPINNING("casino.wof.spinning"),
    CASINO_WOF_WON("casino.wof.won"),
    CHAT_ADVERTISEMENT("chat.advertisement"),
    CHAT_BLOCKED("chat.blocked"),
    CHAT_CHAT_TOO_FAST("chat.chat_too_fast"),
    CHAT_COMMAND_TOO_FAST("chat.command_too_fast"),
    CHAT_DISABLED("chat.disabled"),
    CHAT_MESSAGE_OTHER_AFK("chat.message.other_afk"),
    CHAT_MESSAGE_OTHER_MUTED("chat.message.other_muted"),
    CHAT_MUTED("chat.muted"),
    COINBOOSTER_SERVER_ACTIVATED_SUBTITLE("coinbooster.server.activated.subtitle"),
    COINBOOSTER_SERVER_ACTIVATED_TITLE("coinbooster.server.activated.title"),
    COINBOOSTER_SERVER_REMINDER_MESSAGE("coinbooster.server.reminder_message"),
    COMMAND_GENERAL_NO_PERMISSION("command.general.no_permission"),
    COMMAND_GENERAL_PLAYER_NOT_FOUND("command.general.player_not_found"),
    COMMAND_MESSAGE_FROM_TO_YOU("command.message.from_to_you"),
    COMMAND_MESSAGE_NO_TARGET("command.message.no_target"),
    COMMAND_MESSAGE_YOU_TO_OTHER("command.message.you_to_other"),
    COMMAND_RESOURCEPACK_ACTION_DOWNLOAD_MANUALLY("command.resourcepack.action_download_manually"),
    COMMAND_RESOURCEPACK_SENT("command.resourcepack.sent"),
    COMMAND_STUCK_FAIL("command.stuck.fail"),
    COMMAND_STUCK_SUCCESS("command.stuck.success"),
    COMMAND_WARPS_GENERIC_ERROR("command.warps.generic_error"),
    COMMAND_WARPS_NOT_A_NUMBER("command.warps.not_a_number"),
    COMMAND_WARPS_NOT_FOUND("command.warps.not_found"),
    COMMAND_WARPS_USAGE_WARPS("command.warps.usage_warps"),
    GUI_SCOREBOARD_GUESTS_ONLINE("gui.scoreboard.guests_online"),
    GUI_SCOREBOARD_TITLE("gui.scoreboard.title"),
    GUI_SCOREBOARD_VC_PER_MINUTE("gui.scoreboard.vc_per_minute"),
    GUI_SCOREBOARD_VENTURECOINS("gui.scoreboard.venturecoins"),
    ITEM_RECEIVED_TITLE("item.received.title"),
    ITEM_RECEIVED_TITLE_SUBTITLE("item.received.title.subtitle"),
    JOIN_BUILDER("join.builder"),
    JOIN_CREW("join.crew"),
    JOIN_FIRST("join.first"),
    JOIN_GUEST("join.guest"),
    JOIN_LEFT("join.left"),
    JOIN_OWNER("join.owner"),
    KART_ALREADY_IN_KART("kart.already_in_kart"),
    KART_INVALID_SPAWN_LOCATION("kart.invalid_spawn_location"),
    KART_NOT_FOUND("kart.not_found"),
    MENU_COINBOOSTER_ACTIVATED("menu.coinbooster.activated"),
    MENU_COINBOOSTER_ACTIVATING("menu.coinbooster.activating"),
    MENU_COINBOOSTER_ACTIVATION_FAILED("menu.coinbooster.activation_failed"),
    MENU_COINBOOSTER_ACTIVE_BOOSTER_LIMITED("menu.coinbooster.active_booster_limited"),
    MENU_ITEMS_EQUIPMENT_FAILED("menu.items.equipment_failed"),
    MENU_ITEMS_EQUIPPED("menu.items.equipped"),
    MENU_ITEMS_EQUIPPING("menu.items.equipping"),
    MENU_REALMS_NON_EXISTING_WARP("menu.realms.non_existing_warp"),
    MENU_SHOP_ATTEMPTING_BUY("menu.shop.attempting_buy"),
    MENU_SHOP_BUY_ERROR("menu.shop.buy_error"),
    MENU_SHOP_BUY_FAILED("menu.shop.buy_failed"),
    MENU_SHOP_EQUIP("menu.shop.equip"),
    MENU_SHOP_NOT_ENOUGH_VENTURECOINS("menu.shop.not_enough_venturecoins"),
    MENU_WARDROBE_CHANGING("menu.wardrobe.changing"),
    MINIGAME_ENTRY_TIMED("minigame.entry.timed"),
    MINIGAME_LOBBY_ALREADY_JOINED("minigame.lobby.already_joined"),
    MINIGAME_LOBBY_ALREADY_JOINED_OTHER("minigame.lobby.already_joined_other"),
    MINIGAME_LOBBY_FULL("minigame.lobby.full"),
    MINIGAME_LOBBY_PLAYER_JOINED("minigame.lobby.player_joined"),
    MINIGAME_LOBBY_PLAYER_LEFT("minigame.lobby.player_left"),
    MINIGAME_LOBBY_STARTING_IN("minigame.lobby.starting_in"),
    MINIGAME_LOBBY_VEHICLE_BANNED("minigame.lobby.vehicle_banned"),
    MINIGAME_NAME_LASERGAME("minigame.name.lasergame"),
    MINIGAME_NON_EXISTING("minigame.non_existing"),
    MINIGAME_PLAYER_LEFT("minigame.player_left"),
    MINIGAME_PUT_ON_STANDBY("minigame.put_on_standby"),
    MINIGAME_RUNNING("minigame.running"),
    MINIGAME_TOO_FEW_PLAYERS("minigame.too_few_players"),
    MINIGAME_WIN_ENTRY("minigame.win.entry"),
    MINIGAME_WIN_FOOTER("minigame.win.footer"),
    MINIGAME_WIN_HEADER("minigame.win.header"),
    MOTD_ACTIONBAR("motd.actionbar"),
    MOTD_LOGIN("motd.login"),
    NOFLYZONE_BLOCKED("noflyzone.blocked"),
    OPERATOR_CONTROL_NO_PERMISSION("operator.control.no_permission"),
    PARK_PICTURE_TAKEN("park.picture_taken"),
    RESOURCEPACK_SENDING("resourcepack.sending"),
    RIDE_COUNTER_INCREASED("ride.counter_increased"),
    RIDE_DISPATCH_IN_X_SECONDS("ride.dispatch_in_x_seconds"),
    RIDE_GONBAO_PRESHOW_WAITING_FOR_RIDE("ride.gonbao.preshow_waiting_for_ride"),
    RIDE_PUKE("ride.puke"),
    RIDE_STATE_CLOSED("ride.state.closed"),
    RIDE_STATE_VIP_ONLY("ride.state.vip_only"),
    RIDE_TELEPORT_AWAY("ride.teleport_away"),
    RIDE_WAITING_FOR_AUTO_DISPATCH("ride.waiting_for_auto_dispatch"),
    RIDE_WAITING_FOR_OPERATOR_DISPATCH("ride.waiting_for_operator_dispatch"),
    RIDE_WAITING_SYNC_STATION("ride.waiting_sync_station"),
    RIDESTATE_CLOSED("ridestate.closed"),
    RIDESTATE_HISTORIC("ridestate.historic"),
    RIDESTATE_MAINTENANCE("ridestate.maintenance"),
    RIDESTATE_OPEN("ridestate.open"),
    RIDESTATE_OPEN_NEW("ridestate.open_new"),
    RIDESTATE_SECRET("ridestate.secret"),
    RIDESTATE_SECRET_CLOSED("ridestate.secret_closed"),
    RIDESTATE_UNDER_CONSTRUCTION("ridestate.under_construction"),
    RIDESTATE_VIP_OPEN("ridestate.vip_open"),
    ROAMING_LEGS_HURT("roaming.legs_hurt"),
    SHUTDOWN_EMPTYING_RIDES("shutdown.emptying_rides"),
    SHUTDOWN_IN_X_SECONDS("shutdown.in_x_seconds"),
    SHUTDOWN_KICKED("shutdown.kicked"),
    SHUTDOWN_PREPARING("shutdown.preparing"),
    SHUTDOWN_START_TITLE("shutdown.start_title"),
    SHUTDOWN_STOP_TITLE("shutdown.stop_title"),
    SUBTITLE_JOIN("subtitle.join"),
    TITLE_JOIN("title.join");

    val translation: Component?
        get() = TranslationUtil.getTranslation(value)

    fun getRawTranslation(player: Player?): String? {
        return TranslationUtil.getRawTranslation(player, value)
    }

    fun getTranslation(player: Player?, vararg params: Any?): Component? {
        return TranslationUtil.getTranslation(player, value, *params)
    }

    companion object {

        fun getTranslationByKey(key: String): Translation? {
            var key = key
            key = key.lowercase(Locale.getDefault())
            for (translation in values()) {
                if (translation.value == key) {
                    return translation
                }
            }
            return null
        }
    }
}
