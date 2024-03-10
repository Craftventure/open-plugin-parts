package net.craftventure.audioserver.packet

enum class PacketID(
    val id: Int,
    val direction: Direction = Direction.OUT
) {
    LOGIN(1, Direction.IN),
    KICK(2),
    CLIENT_ACCEPTED(3),
    PING(8, Direction.BOTH),

    RELOAD(14),
    AREA_DEFINITION(4),
    AREA_STATE(5),
    SYNC(6),

    KEY_VALUE(7),
    VOLUME(13, Direction.BOTH),
    LOCATION_UPDATE(15),

    DISPLAY_AREA_ENTER(18, Direction.IN),

    OPERATOR_DEFINITION(19),
    OPERATOR_CONTROL_UPDATE(20),
    OPERATOR_SLOT_UPDATE(21),
    OPERATOR_CONTROL_CLICK(22, Direction.IN),
    OPERATOR_RIDE_UPDATE(34),

    PARK_PHOTO(23),

    PLAYER_LOCATIONS(27),

    ADD_MAP_LAYERS(28),
    REMOVE_MAP_LAYERS(35),

    MARKER_ADD(36),
    MARKER_REMOVE(37),

    POLYGON_OVERLAY_ADD(38),
    POLYGON_OVERLAY_REMOVE(39),

    SPATIAL_AUDIO_DEFINITION(29),
    SPATIAL_AUDIO_UPDATE(30),
    SPATIAL_AUDIO_REMOVE(31),

    AREA_REMOVE(32),
    BATCH_PACKET(33);

    enum class Direction(
        val allowsSend: Boolean,
        val allowsReceive: Boolean
    ) {
        IN(false, true),
        OUT(true, false),
        BOTH(true, true)
    }

    companion object {
        fun fromId(id: Int) = values().firstOrNull { it.id == id }
    }
}
