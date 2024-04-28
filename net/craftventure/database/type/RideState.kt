package net.craftventure.database.type

import net.craftventure.core.ktx.util.Permissions
import org.jooq.impl.EnumConverter


enum class RideState constructor(
    val isOpen: Boolean = false,
    val permission: String? = null,
    val showInMenu: Boolean = false,
    val isOperable: Boolean = false,
    val stateTranslationTypeName: String
) {
    VIP_PREVIEW(
        isOpen = true,
        permission = Permissions.PAYED_VIP,
        showInMenu = true,
        stateTranslationTypeName = "RIDESTATE_VIP_OPEN",
        isOperable = true
    ),
    OPEN(
        isOpen = true,
        showInMenu = true,
        isOperable = true,
        stateTranslationTypeName = "RIDESTATE_OPEN"
    ),
    CLOSED(
        showInMenu = true,
        stateTranslationTypeName = "RIDESTATE_CLOSED"
    ),
    MAINTENANCE(
        showInMenu = true,
        stateTranslationTypeName = "RIDESTATE_MAINTENANCE"
    ),
    UNDER_CONSTRUCTION(
        showInMenu = true,
        stateTranslationTypeName = "RIDESTATE_UNDER_CONSTRUCTION"
    ),
    SECRET(
        isOpen = true,
        stateTranslationTypeName = "RIDESTATE_SECRET"
    ),
    SECRET_CLOSED(
        stateTranslationTypeName = "RIDESTATE_SECRET_CLOSED"
    ),
    HISTORIC(
        stateTranslationTypeName = "RIDESTATE_HISTORIC"
    );

    companion object {
        class Converter : EnumConverter<String, RideState>(String::class.java, RideState::class.java)
    }
}
