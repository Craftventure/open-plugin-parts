package net.craftventure.core.npc

import com.comphenix.packetwrapper.*
import com.comphenix.protocol.wrappers.WrappedGameProfile
import com.destroystokyo.paper.profile.CraftPlayerProfile
import com.destroystokyo.paper.profile.PlayerProfile
import com.mojang.authlib.GameProfile
import com.mojang.datafixers.util.Pair
import io.netty.buffer.ByteBufAllocator
import io.papermc.paper.adventure.PaperAdventure
import net.craftventure.bukkit.ktx.extension.packAllReflection
import net.craftventure.bukkit.ktx.extension.sendPacket
import net.craftventure.bukkit.ktx.extension.sendPacketIgnoreError
import net.craftventure.bukkit.ktx.extension.withV2Marker
import net.craftventure.core.async.executeSync
import net.craftventure.core.ktx.util.Logger.capture
import net.craftventure.core.ktx.util.Reflections
import net.craftventure.core.npc.EntityMetadata.Interactable
import net.craftventure.core.npc.tracker.NpcEntityTracker
import net.craftventure.core.utils.LookAtUtil
import net.craftventure.core.utils.SmoothCoastersHelper
import net.craftventure.database.bukkit.extensions.toPlayerProfile
import net.craftventure.database.generated.cvdata.tables.pojos.CachedGameProfile
import net.craftventure.database.type.ItemType
import net.craftventure.hasFlag
import net.craftventure.minusFlag
import net.craftventure.withFlag
import net.kyori.adventure.text.Component
import net.minecraft.core.Rotations
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.protocol.game.*
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.entity.projectile.ThrownEgg
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.Vec3
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld
import org.bukkit.craftbukkit.v1_20_R1.block.data.CraftBlockData
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack
import org.bukkit.entity.*
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.abs

class NpcEntity : Interactable {
    var id: String
    val uuid: UUID = UUID.randomUUID().withV2Marker()
    val entityId: Int
    val entityType: EntityType
    private var lastMount: Int? = null

    var profileOverridenDisplayName: net.minecraft.network.chat.Component? = null

    constructor(id: String = UUID.randomUUID().toString(), player: Player, location: Location = player.location) : this(
        id,
        EntityType.PLAYER,
        location,
    ) {
        setGameProfile(player.playerProfile.let { CraftPlayerProfile.asAuthlib(it) }.let { source ->
            GameProfile(uuid, source.name).also {
                it.properties.putAll(source.properties)
            }
        })
    }

    @JvmOverloads
    constructor(
        id: String = UUID.randomUUID().toString(),
        entityType: EntityType,
        location: Location,
        profile: GameProfile? = null
    ) {
        this.id = id
        this.entityType = entityType
        this.entityId = Bukkit.getUnsafe().nextEntityId()
        this.location = location.clone()
        this.headYaw = this.location.yaw
        this.entityData = generateSynchedEntityData()
        setGameProfile(profile)

//        validateEntity()
    }

    // For players
    constructor(
        id: String = UUID.randomUUID().toString(),
        entityType: EntityType,
        location: Location,
        profile: CachedGameProfile?
    ) : this(
        id,
        entityType,
        location,
    ) {
        setGameProfile(profile)
    }

    // For players
    constructor(
        id: String = UUID.randomUUID().toString(),
        entityType: EntityType,
        location: Location,
        profile: PlayerProfile?
    ) : this(
        id,
        entityType,
        location,
    ) {
        setGameProfile(profile)
    }

    private fun validateEntity() {
        if (entityType == EntityType.PLAYER && profile == null) {
//            Logger.severe("Tried to create a player entity without a cachedgameprofile");
            IllegalStateException("Tried to create a player entity without a cachedgameprofile").printStackTrace()
        }
    }

    val teamEntry get() = if (entityType == EntityType.PLAYER) profile?.name ?: "?" else uuid.toString()

    private val metaLock = ReentrantLock()
    private var npcEntityTracker: NpcEntityTracker? = null
    private var profile: GameProfile? = null

    var playerSpecificGameProfileProvider: ((Player) -> GameProfile)? = null

    private val wrapperPlayServerEntityTeleport by lazy { WrapperPlayServerEntityTeleport() }
    private val wrapperPlayServerEntityHeadRotation by lazy { WrapperPlayServerEntityHeadRotation() }
    private val wrapperPlayServerRelEntityMove by lazy { WrapperPlayServerRelEntityMove() }
    private val wrapperPlayServerRelEntityMoveLook by lazy { WrapperPlayServerRelEntityMoveLook() }

    private val location: Location
    private val entityData: SynchedEntityData
    var headYaw = 0f
        private set

    private var isInMetadataTransaction: Boolean = false

    var tag: Any? = null

    var blockData: BlockData? = null
        private set

    // Data for entity spawning like itemframe orientation or falling block fallback data
    private var data = 0

    // To track the amount of movements before we need to teleport to resync
    private var movePacketIndex = 0

    var helmet: ItemStack? = null
        private set
    var leggings: ItemStack? = null
        private set
    var boots: ItemStack? = null
        private set
    var chestplate: ItemStack? = null
        private set
    var held: ItemStack? = null
        private set
    var heldOffhand: ItemStack? = null
        private set

    private var headVector: Rotations? = null
    private var bodyVector: Rotations? = null
    private var leftArmVector: Rotations? = null
    private var rightArmVector: Rotations? = null
    private var leftLegVector: Rotations? = null
    private var rightLegVector: Rotations? = null

    var forceTeleport = false

    val name: String?
        get() = profile?.name

    fun setEntityTracker(npcEntityTracker: NpcEntityTracker?) {
        this.npcEntityTracker = npcEntityTracker
    }

    fun setGameProfile(gameProfile: CachedGameProfile?) {
        setGameProfile(gameProfile?.toPlayerProfile(uuid))
    }

    fun setGameProfile(gameProfile: PlayerProfile?) {
        setGameProfile(if (gameProfile != null) CraftPlayerProfile.asAuthlib(gameProfile) else null)
    }

    fun setGameProfile(gameProfile: GameProfile?) {
        this.profile = gameProfile
    }

    fun getLocation() = location
    fun getLocationCopy() = location.clone()

    fun velocity(vector: Vector) {
        velocity(vector.x, vector.y, vector.z)
    }

    fun velocity(x: Double, y: Double, z: Double) {
        val packet = ClientboundSetEntityMotionPacket(entityId, Vec3(x, y, z))
        val npcEntityTracker = npcEntityTracker ?: return
        for (player in npcEntityTracker.players) {
            try {
                player.sendPacket(packet)
            } catch (e: Exception) {
                capture(e)
            }
        }
    }

    fun allYaws(yaw: Float) {
        move(location.x, location.y, location.z, yaw, location.pitch, yaw)
    }

    fun yaw(yaw: Float) {
        move(location.x, location.y, location.z, yaw, location.pitch)
    }

    fun pitch(pitch: Float) {
        move(location.x, location.y, location.z, location.yaw, pitch)
    }

    fun move(location: Location) {
        move(location.x, location.y, location.z, location.yaw, location.pitch, location.yaw)
    }

    fun moveVisually(
        x: Double = location.x,
        y: Double = location.y,
        z: Double = location.z,
        yaw: Float = location.yaw,
        pitch: Float = location.pitch,
        headYaw: Float = this.headYaw,
        hasMoved: Boolean = true,
        hasChangedYawOrPitch: Boolean = true,
        headYawChanged: Boolean = true,
        forceTeleport: Boolean = this.forceTeleport,
        visibleTo: Collection<Player>,
    ) {
        if (visibleTo.isEmpty()) return
        val isExternal = visibleTo !== npcEntityTracker?.players

        if (headYawChanged) {
            sendHeadYawPacket(visibleTo, headYaw.toDouble())
        }

        if (hasMoved || forceTeleport && hasChangedYawOrPitch) {
            val shouldTeleport = abs(location.x - x) >= 8 ||
                    abs(location.y - y) >= 8 ||
                    abs(location.z - z) >= 8
            if (shouldTeleport || forceTeleport) {
                val wrapperPlayServerEntityTeleport =
                    if (isExternal) WrapperPlayServerEntityTeleport() else wrapperPlayServerEntityTeleport
                wrapperPlayServerEntityTeleport.entityID = entityId
                wrapperPlayServerEntityTeleport.x = x
                wrapperPlayServerEntityTeleport.y = y
                wrapperPlayServerEntityTeleport.z = z
                wrapperPlayServerEntityTeleport.yaw = yaw
                wrapperPlayServerEntityTeleport.pitch = pitch
                wrapperPlayServerEntityTeleport.onGround = false
                for (player in visibleTo) {
                    try {
                        wrapperPlayServerEntityTeleport.sendPacket(player)
                    } catch (e: java.lang.Exception) {
                        capture(e)
                    }
                }
            } else {
                if (hasChangedYawOrPitch) {
                    val wrapperPlayServerRelEntityMoveLook =
                        if (isExternal) WrapperPlayServerRelEntityMoveLook() else wrapperPlayServerRelEntityMoveLook
                    wrapperPlayServerRelEntityMoveLook.entityID = entityId
                    wrapperPlayServerRelEntityMoveLook.dx = x - location.x
                    wrapperPlayServerRelEntityMoveLook.dy = y - location.y
                    wrapperPlayServerRelEntityMoveLook.dz = z - location.z
                    wrapperPlayServerRelEntityMoveLook.yaw = yaw
                    wrapperPlayServerRelEntityMoveLook.pitch = pitch
                    for (player in visibleTo) {
                        try {
                            wrapperPlayServerRelEntityMoveLook.sendPacket(player)
                        } catch (e: java.lang.Exception) {
                            capture(e)
                        }
                    }
                } else {
                    val wrapperPlayServerRelEntityMove =
                        if (isExternal) WrapperPlayServerRelEntityMove() else wrapperPlayServerRelEntityMove
                    wrapperPlayServerRelEntityMove.entityID = entityId
                    wrapperPlayServerRelEntityMove.dx = x - location.x
                    wrapperPlayServerRelEntityMove.dy = y - location.y
                    wrapperPlayServerRelEntityMove.dz = z - location.z
                    for (player in visibleTo) {
                        try {
                            wrapperPlayServerRelEntityMove.sendPacket(player)
                        } catch (e: java.lang.Exception) {
                            capture(e)
                        }
                    }
                }
            }
        } else if (hasChangedYawOrPitch) {
            val wrapperPlayServerRelEntityMoveLook =
                if (isExternal) WrapperPlayServerRelEntityMoveLook() else wrapperPlayServerRelEntityMoveLook
            wrapperPlayServerRelEntityMoveLook.entityID = entityId
            wrapperPlayServerRelEntityMoveLook.dx = 0.0
            wrapperPlayServerRelEntityMoveLook.dy = 0.0
            wrapperPlayServerRelEntityMoveLook.dz = 0.0
            wrapperPlayServerRelEntityMoveLook.yaw = yaw
            wrapperPlayServerRelEntityMoveLook.pitch = pitch
            wrapperPlayServerRelEntityMoveLook.onGround = false
            for (player in visibleTo) {
                try {
                    wrapperPlayServerRelEntityMoveLook.sendPacket(player)
                } catch (e: java.lang.Exception) {
                    capture(e)
                }
            }
        }
    }

    @JvmOverloads
    fun move(
        x: Double = location.x,
        y: Double = location.y,
        z: Double = location.z,
        yaw: Float = location.yaw,
        pitch: Float = location.pitch,
        headYaw: Float = this.headYaw,
    ) {
        val players = if (npcEntityTracker != null) npcEntityTracker!!.players else null
        if (players == null || players.isEmpty()) {
            location.x = x
            location.y = y
            location.z = z
            location.yaw = yaw
            location.pitch = pitch
            this.headYaw = headYaw
            return
        }

        val hasMoved = Math.abs(location.x - x) > 0.01 || Math.abs(location.y - y) > 0.01 || Math.abs(
            location.z - z
        ) > 0.01
        val hasChangedYawOrPitch = location.yaw != yaw || location.pitch != pitch
        val headYawChanged = Math.abs(this.headYaw - headYaw) > 0.01

//        customNameVisible(true)
//        customName("${x.format(2)} ${y.format(2)} ${z.format(2)} $hasMoved $hasChangedYawOrPitch $headYawChanged $movePacketIndex")


        var shouldTeleport = false
        movePacketIndex++
        if (movePacketIndex > 20) {
            movePacketIndex = 0
            shouldTeleport = true
        }

        moveVisually(
            x = x,
            y = y,
            z = z,
            yaw = yaw,
            pitch = pitch,
            headYaw = headYaw,
            hasMoved = hasMoved,
            hasChangedYawOrPitch = hasChangedYawOrPitch,
            headYawChanged = headYawChanged,
            visibleTo = players,
            forceTeleport = shouldTeleport || forceTeleport,
        )

        if (hasMoved || (forceTeleport && hasChangedYawOrPitch)) {
            location.x = x
            location.y = y
            location.z = z
        }

        location.yaw = yaw
        location.pitch = pitch
        this.headYaw = headYaw
    }

    fun lookAtResult(x: Double, y: Double, z: Double, eyeHeight: Double = 0.0): LookAtUtil.YawPitch {
        val dx = x - location.x
        val dy = y - location.y + eyeHeight
        val dz = z - location.z
        val r = Math.sqrt(dx * dx + dy * dy + dz * dz)
        var yaw = -Math.atan2(dx, dz) / Math.PI * 180
        if (yaw < 0) yaw = 360 + yaw
        val pitch = -Math.asin(dy / r) / Math.PI * 180

        return LookAtUtil.YawPitch(yaw, pitch)
    }

    fun lookAt(x: Double, y: Double, z: Double, eyeHeight: Double = 0.0) {
//        Logger.info("LookAt %s %s %s", false, x, y, z);
        val result = lookAtResult(x, y, z, eyeHeight)
        move(
            location.x,
            location.y,
            location.z,
            result.yaw.toFloat(),
            result.pitch.toFloat(),
            result.yaw.toFloat(),
        )
    }

    fun lookAt(x: Double, y: Double, z: Double, eyeHeight: Double = 0.0, visibleTo: Collection<Player>) {
//        Logger.info("LookAt %s %s %s", false, x, y, z);
        val result = lookAtResult(x, y, z, eyeHeight)

//        logcat { "yaw=${yaw.format(2)} pitch=${pitch.format(2)}" }
        moveVisually(
            yaw = result.yaw.toFloat(),
            pitch = result.pitch.toFloat(),
            headYaw = result.yaw.toFloat(),
            visibleTo = visibleTo
        )
    }

    private fun broadcastPacket(abstractPacket: AbstractPacket) {
        if (npcEntityTracker != null) {
            for (player in npcEntityTracker!!.players) {
                try {
                    abstractPacket.sendPacket(player)
                } catch (e: java.lang.Exception) {
                    capture(e)
                }
            }
        }
    }

    fun moveHead(headYaw: Float) {
//        DecimalFormat decimalFormat = new DecimalFormat("000");
//        customName("2 Headyaw " + decimalFormat.format(headYaw) + " Yaw " + decimalFormat.format(location.getYawRadian()) + " Pitch " + decimalFormat.format(location.getPitchRadian()));
        if (this.headYaw != headYaw && entityType != EntityType.ARMOR_STAND) {
//            if (entityType.usesSpawnPlayer())
//                Logger.info("Move head (yaw) to %s, %s", false, headYaw, npcEntityTracker != null);
            sendHeadYawPacket()
        }
        this.headYaw = headYaw
    }

    private fun sendHeadYawPacket(
        to: Collection<Player> = npcEntityTracker!!.players,
        value: Double = headYaw.toDouble(),
    ) {
        val isExternal = to !== npcEntityTracker?.players
        val wrapperPlayServerEntityHeadRotation =
            if (isExternal) WrapperPlayServerEntityHeadRotation() else wrapperPlayServerEntityHeadRotation
        wrapperPlayServerEntityHeadRotation.entityID = entityId
        wrapperPlayServerEntityHeadRotation.setHeadYaw(value)
        for (player in to) {
            try {
                wrapperPlayServerEntityHeadRotation.sendPacket(player)
                //                    Logger.debug("sendHeadYawPacket");
            } catch (e: java.lang.Exception) {
                capture(e)
            }
        }
    }

    fun spawn(player: Player) {
//        Logger.debug("Spawning NPC to %s with id %d", false, player.getName(), getEntityId());
        if (entityType == EntityType.PLAYER) {
            if (profile == null) return
            sendPlayerInfo(player)

//            val byteBuf = ByteBufAllocator.DEFAULT.buffer()
//            val buf = FriendlyByteBuf(byteBuf)
//            buf.writeVarInt(entityId)
//            buf.writeUUID(uuid)
//            buf.writeDouble(this.location.x)
//            buf.writeDouble(this.location.y)
//            buf.writeDouble(this.location.z)
//            buf.writeByte(((this.headYaw * 256.0F / 360.0F).toInt()))
//            buf.writeByte(((this.location.pitch * 256.0F / 360.0F).toInt()))
//             ClientboundAddPlayerPacket(buf)

            val wrapperPlayServerNamedEntitySpawn = WrapperPlayServerNamedEntitySpawn()
            wrapperPlayServerNamedEntitySpawn.entityID = entityId
            wrapperPlayServerNamedEntitySpawn.playerUUID = uuid
            wrapperPlayServerNamedEntitySpawn.x = location.x
            wrapperPlayServerNamedEntitySpawn.y = location.y
            wrapperPlayServerNamedEntitySpawn.z = location.z
            wrapperPlayServerNamedEntitySpawn.yaw = location.yaw
            wrapperPlayServerNamedEntitySpawn.pitch = location.pitch
            try {
                wrapperPlayServerNamedEntitySpawn.sendPacket(player)
            } catch (e: java.lang.Exception) {
                capture(e)
            }
            sendHeadYawPacket(to = listOf(player))
            //            Logger.info("Spawning player to %s (uuid=%s, name=%s, location=%s)", false, player.getName(), getUuid(), getName(), getLocation());

//            destroyPlayerInfo(player);
        } else {
            val spawn = ClientboundAddEntityPacket(
                entityId,
                uuid,
                location.x,
                location.y,
                location.z,
                location.pitch,
                location.yaw,
                NmsEntityTypes.entityTypeToClassMap[entityType]!!.type!!,
                data,
                Vec3.ZERO,
                headYaw.toDouble(),
            )
            try {
                player.sendPacketIgnoreError(spawn)
            } catch (e: java.lang.Exception) {
                capture(e)
            }
        }
        sendAllEquipment(to = setOf(player))
        sendMetadata(player)

        SmoothCoastersHelper.api.setEntityLerpTicks(null, player, entityId, 1)
    }

    fun destroy(player: Player) {
//        Logger.info("Destroying NpcEntity to " + player.getName());
        try {
            player.sendPacket(ClientboundRemoveEntitiesPacket(entityId))
            //            Logger.debug("destroy");
        } catch (e: java.lang.Exception) {
            capture(e)
        }
        destroyPlayerInfo(player)
    }

    private fun sendMetadata(player: Player) {
        metaLock.withLock {
//            entityData.all?.forEach {
//                entityData.markDirty(it.accessor)
//                handleEntityMetadataChange()
//            }
//            if (true)
//                return
            val packet = ClientboundSetEntityDataPacket(entityId, entityData.packAllReflection())
//            packet.unpackedData?.forEach {
//                logcat {
//                    val valueString = when (val value = it.value) {
//                        is Vector3f -> "V3f ${value.x().format(2)} ${value.y().format(2)} ${value.z().format(2)}"
//                        is Rotations -> "Rot ${value.x.format(2)} ${value.y.format(2)} ${value.z.format(2)}"
//                        else -> value.toString()
//                    }
//                    "- entity ${entityId} ${it.accessor.id} with value $valueString"
//                }
//            }
            player.sendPacketIgnoreError(packet)
//            if (entityId == 503)
//                logcat { "Sending left arm should=${leftArmVector?.x} actual=${getMetadata(EntityMetadata.ArmorStand.leftArmPose)?.x}" }
        }
    }

    private fun generateAddPlayerPacket(player: Player): ClientboundPlayerInfoUpdatePacket {
        val profile = playerSpecificGameProfileProvider?.let { it(player) }?: profile!!

        val byteBuf = ByteBufAllocator.DEFAULT.buffer()
        val buf = FriendlyByteBuf(byteBuf)

        buf.writeEnumSet(
            EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER),
            ClientboundPlayerInfoUpdatePacket.Action::class.java
        )
        val entry = ClientboundPlayerInfoUpdatePacket.Entry(
            uuid,
            profile,
            false,
            0,
            GameType.ADVENTURE,
            profileOverridenDisplayName,
            null
        )
        val property = Reflections.getField(ClientboundPlayerInfoUpdatePacket.Action::class.java, "h")!!
        buf.writeCollection(listOf(entry)) { buf2: FriendlyByteBuf, entry: ClientboundPlayerInfoUpdatePacket.Entry ->
            buf2.writeUUID(entry.profileId)
            //https://nms.screamingsandals.org/1.19.4/net/minecraft/network/protocol/game/ClientboundPlayerInfoUpdatePacket$Action.html > writer
            (property.get(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER) as ClientboundPlayerInfoUpdatePacket.Action.Writer).write(
                buf2,
                entry
            )
        }

        return ClientboundPlayerInfoUpdatePacket(buf)
    }

    private fun sendPlayerInfo(player: Player) {
        if (entityType != EntityType.PLAYER) return
        if (profile == null) return
//            Logger.info("Sending playerinfo to " + player.getName());
        player.sendPacketIgnoreError(generateAddPlayerPacket(player))

        executeSync(20) {
            destroyPlayerInfo(player)
        }
//        sendHideInfo(player)
    }

//    private fun sendHideInfo(player: Player) {
//        if (entityType != EntityType.PLAYER) return
//        if (profile == null) return
////            Logger.info("Sending playerinfo to " + player.getName());
//        val packet = ClientboundPlayerInfoPacket(
//            ClientboundPlayerInfoPacket.Action.UPDATE_DISPLAY_NAME,
//            emptyList()
//        )
//        packet.entries.add(
//            ClientboundPlayerInfoPacket.PlayerUpdate(
//                profile!!,
//                0,
//                null,
//                null
//            )
//        )
//        player.sendPacketIgnoreError(packet)
//    }

    private fun destroyPlayerInfo(player: Player) {
        if (entityType != EntityType.PLAYER) return
        if (profile == null) return
//            Logger.info("Destroying playerinfo to " + player.getName());

        val packet = ClientboundPlayerInfoRemovePacket(listOf(uuid))
        player.sendPacketIgnoreError(packet)
    }

    @Deprecated("Send in batch")
    private fun sendEquipment(
        itemStack: ItemStack,
        slot: EquipmentSlot,
        to: Set<Player> = npcEntityTracker?.players ?: emptySet(),
    ) {
        if (to.isEmpty()) return
        sendEquipment(
            listOf(
                Pair(
                    net.minecraft.world.entity.EquipmentSlot.values()[slot.ordinal],
                    CraftItemStack.asNMSCopy(itemStack) ?: net.minecraft.world.item.ItemStack.EMPTY
                )
            ),
            to = to,
        )
    }

    private fun sendBukkitEquipment(
        items: List<kotlin.Pair<EquipmentSlot, ItemStack?>>,
        to: Set<Player> = npcEntityTracker?.players ?: emptySet(),
    ) {
        if (to.isEmpty()) return
        sendEquipment(
            items.map {
                Pair(
                    net.minecraft.world.entity.EquipmentSlot.values()[it.first.ordinal],
                    it.second?.let { CraftItemStack.asNMSCopy(it) } ?: net.minecraft.world.item.ItemStack.EMPTY
                )
            },
            to = to,
        )
    }

    private fun sendEquipment(
        items: List<Pair<net.minecraft.world.entity.EquipmentSlot, net.minecraft.world.item.ItemStack>>,
        to: Set<Player> = npcEntityTracker?.players ?: emptySet(),
    ) {
        if (to.isEmpty()) return
        val packet = ClientboundSetEquipmentPacket(entityId, items)
        to.sendPacket(packet)
    }

    private fun sendAllEquipment(
        to: Set<Player> = npcEntityTracker?.players ?: emptySet(),
    ) {
        if (to.isEmpty()) return
        sendBukkitEquipment(
            listOfNotNull(
                EquipmentSlot.HAND to held,
                EquipmentSlot.OFF_HAND to heldOffhand,
                EquipmentSlot.HEAD to helmet,
                EquipmentSlot.FEET to boots,
                EquipmentSlot.CHEST to chestplate,
                EquipmentSlot.LEGS to leggings,
            ),
            to = to,
        )
    }

    private fun <T> requireValue(field: EntityDataAccessor<T>, defaultValue: T): T {
        metaLock.withLock {
            return try {
                entityData.get(field)
            } catch (e: Exception) {
                entityData.define(field, defaultValue)
                entityData.get(field)
            }
        }
    }

    fun setByteFlag(field: EntityMetadata.Interactor<Byte>, flag: Byte, value: Boolean) {
        setByteFlag(field.accessor, flag, value)
    }

    fun setByteFlag(field: EntityDataAccessor<Byte>, flag: Byte, value: Boolean) {
        val currentValue = requireValue(field, 0)
        val hasFlag = currentValue hasFlag flag
        if (hasFlag != value) {
            val newValue = if (value) currentValue withFlag flag else currentValue minusFlag flag
            metaLock.withLock {
                entityData.set(field, newValue)
            }
//            Logger.debug("Setting flag ${field.id} to ${newValue} (was $currentValue)")
            handleEntityMetadataChange()
        }
    }

    fun getByteFlag(field: EntityMetadata.Interactor<Byte>, flag: Byte): Boolean {
        return getByteFlag(field.accessor, flag)
    }

    fun getByteFlag(field: EntityDataAccessor<Byte>, flag: Byte): Boolean {
        return try {
            entityData.get(field).hasFlag(flag)
        } catch (e: Exception) {
            return false
        }
    }

    fun <T> getMetadata(field: EntityMetadata.Interactor<T>): T? {
        return getMetadata(field.accessor)
    }

    fun <T> getMetadata(field: EntityDataAccessor<T>): T? {
        return try {
            entityData.get(field)
        } catch (e: Exception) {
            return null
        }
    }

    override fun <T> applyInteractor(interactor: EntityMetadata.Interactor<T>, data: T) {
        setMetadata(interactor, data)
    }

    fun <T> setMetadata(field: EntityMetadata.Interactor<T>, value: T) {
        setMetadata(field.accessor, value, field.defaultValue)
    }

    fun <T> setMetadata(field: EntityDataAccessor<T>, value: T, defaultValue: T) {
//        if (entityId == 503) {
//            val valueString = when (value) {
//                is Vector3f -> "V3f ${value.x().format(2)} ${value.y().format(2)} ${value.z().format(2)}"
//                is Rotations -> "Rot ${value.x.format(2)} ${value.y.format(2)} ${value.z.format(2)}"
//                else -> value.toString()
//            }
//            logcat {
//                "Set meta ${field.id} to $valueString ${if (value != null) value!!::class.java.name else "?"} ${
//                    miniTrace(
//                        15
//                    )
//                }"
//            }
//        }
        metaLock.withLock {
            try {
                entityData.set(field, value)
            } catch (e: Exception) {
                entityData.define(field, defaultValue)
                entityData.set(field, value)
            }
        }
        handleEntityMetadataChange()
    }

    fun <T> setPlayerSpecificMetadata(field: EntityMetadata.Interactor<T>, value: T, players: List<Player>) {
        setPlayerSpecificMetadata(field.accessor, value, field.defaultValue, players)
    }

    fun <T> setPlayerSpecificMetadata(field: EntityDataAccessor<T>, value: T, defaultValue: T, players: List<Player>) {
        val entityData = generateSynchedEntityData()
        try {
            entityData.set(field, value)
        } catch (e: Exception) {
            entityData.define(field, defaultValue)
            entityData.set(field, value)
        }

        val packet = ClientboundSetEntityDataPacket(entityId, entityData.packAllReflection())
        players.sendPacketIgnoreError(packet)
    }

    @Deprecated("This should eventually be called automatically by tick()")
    private fun handleEntityMetadataChange() {
        if (isInMetadataTransaction) return
        metaLock.withLock {
            val players = npcEntityTracker?.players?.takeIf { it.isNotEmpty() }
            if (players == null) {
                // Used to be clearDirty()
                entityData.packDirty()
                return
            }
            if (entityData.isDirty) {
                val packet = ClientboundSetEntityDataPacket(entityId, entityData.packDirty() ?: emptyList())

//                logcat {
//                    val metas = packet.unpackedData
//                    "Setting meta ${metas?.size} (of ${entityData?.all?.size}) datas ${
//                        metas?.map {
//                            val valueString = when (val value = it.value) {
//                                is Vector3f -> "V3f ${value.x().format(2)} ${value.y().format(2)} ${
//                                    value.z().format(2)
//                                }"
//
//                                is Rotations -> "Rot ${value.x.format(2)} ${value.y.format(2)} ${value.z.format(2)}"
//                                else -> value.toString()
//                            }
//                            "${it.accessor?.id}=${valueString}(${it.isDirty})"
//                        }?.joinToString(", ")
//                    }"
//                }
//            Logger.debug("Broadcasting ${packet.unpackedData?.size} metadata changes")
                players.sendPacketIgnoreError(packet)
            }
        }
    }

    fun updateMetadata(block: NpcEntity.() -> Unit) {
        try {
            isInMetadataTransaction = true
            block(this)
        } finally {
            isInMetadataTransaction = false
        }
        handleEntityMetadataChange()
    }

    fun tick() {
        handleEntityMetadataChange()
    }

    fun setBlockData(blockData: BlockData): NpcEntity {
        this.blockData = blockData
        data = Block.getId((blockData as CraftBlockData).state)
        return this
    }

    private fun getProfile(): WrappedGameProfile {
        return WrappedGameProfile.fromHandle(profile)
    }

    /** =======================================
     *
     *      Flags
     *
    =========================================== */

    fun noGravity(noGravity: Boolean) {
        setMetadata(EntityMetadata.Entity.noGravity, noGravity)
    }

    fun invisible(invisible: Boolean): NpcEntity {
        setByteFlag(
            EntityMetadata.Entity.sharedFlags,
            EntityBitFlags.EntityState.INVISIBLE,
            invisible
        )
        return this
    }

    fun invisible(): Boolean {
        return getByteFlag(
            EntityMetadata.Entity.sharedFlags,
            EntityBitFlags.EntityState.INVISIBLE
        )
    }

    fun setSlot(slot: EquipmentSlot, itemStack: ItemStack?) {
        if (slot == EquipmentSlot.HAND) {
            held(itemStack)
        } else if (slot == EquipmentSlot.OFF_HAND) {
            heldOffHand(itemStack)
        } else if (slot == EquipmentSlot.FEET) {
            boots(itemStack)
        } else if (slot == EquipmentSlot.LEGS) {
            leggings(itemStack)
        } else if (slot == EquipmentSlot.CHEST) {
            chestplate(itemStack)
        } else if (slot == EquipmentSlot.HEAD) {
            helmet(itemStack)
        }
    }

    fun setSlot(slot: ItemType, itemStack: ItemStack?) {
        when (slot) {
            ItemType.HELMET -> helmet(itemStack)
            ItemType.CHESTPLATE -> chestplate(itemStack)
            ItemType.LEGGINGS -> leggings(itemStack)
            ItemType.BOOTS -> boots(itemStack)
            ItemType.WEAPON -> held(itemStack)
            else -> {}
        }
    }

    fun held(held: ItemStack?): NpcEntity {
        if (this.held != held /*|| this.held?.getColor() != held?.getColor()*/) {
            this.held = held
            sendEquipment(held ?: ItemStack(Material.AIR), EquipmentSlot.HAND)
        }
        return this
    }

    fun heldOffHand(heldOffhand: ItemStack?): NpcEntity {
        if (this.heldOffhand != heldOffhand /*|| this.heldOffhand?.getColor() != heldOffhand?.getColor()*/) {
            this.heldOffhand = heldOffhand
            sendEquipment(heldOffhand ?: ItemStack(Material.AIR), EquipmentSlot.OFF_HAND)
        }
        return this
    }

    fun helmet(helmet: ItemStack?): NpcEntity {
        if (this.helmet != helmet /*|| this.helmet?.getColor() != helmet?.getColor()*/) {
            this.helmet = helmet
            sendEquipment(helmet ?: ItemStack(Material.AIR), EquipmentSlot.HEAD)
        }
        return this
    }

    fun boots(boots: ItemStack?): NpcEntity {
        if (this.boots != boots /*|| this.boots?.getColor() != boots?.getColor()*/) {
            this.boots = boots
            sendEquipment(boots ?: ItemStack(Material.AIR), EquipmentSlot.FEET)
        }
        return this
    }

    fun chestplate(chestplate: ItemStack?): NpcEntity {
        if (this.chestplate != chestplate /*|| this.chestplate?.getColor() != chestplate?.getColor()*/) {
            this.chestplate = chestplate
            sendEquipment(chestplate ?: ItemStack(Material.AIR), EquipmentSlot.CHEST)
        }
        return this
    }

    fun leggings(leggings: ItemStack?): NpcEntity {
        if (this.leggings != leggings /*|| this.leggings?.getColor() != leggings?.getColor()*/) {
            this.leggings = leggings
            sendEquipment(leggings ?: ItemStack(Material.AIR), EquipmentSlot.LEGS)
        }
        return this
    }

    fun armorstandSmall(value: Boolean): NpcEntity {
        setByteFlag(
            EntityMetadata.ArmorStand.flags,
            EntityBitFlags.ArmorStandState.SMALL,
            value
        )
        return this
    }

    fun armorstandShowArms(value: Boolean): NpcEntity {
        setByteFlag(EntityMetadata.ArmorStand.flags, EntityBitFlags.ArmorStandState.ARMS, value)
        return this
    }

    fun hideBasePlate(value: Boolean): NpcEntity {
        setByteFlag(
            EntityMetadata.ArmorStand.flags,
            EntityBitFlags.ArmorStandState.HIDE_BASEPLATE,
            value
        )
        return this
    }

    fun marker(value: Boolean): NpcEntity {
        setByteFlag(
            EntityMetadata.ArmorStand.flags,
            EntityBitFlags.ArmorStandState.MARKER,
            value
        )
        return this
    }


    private fun shouldUpdate(source: Rotations?, newX: Float, newY: Float, newZ: Float): Boolean {
//        if (!(source == null || source.getX() != newX || source.getY() != newY || source.getZ() != newZ))
//            Logger.console("Not updating!");
        return source == null || source.x != newX || source.y != newY || source.z != newZ
    }

    fun body(x: Float, y: Float, z: Float): NpcEntity {
        if (entityType == EntityType.ARMOR_STAND && shouldUpdate(bodyVector, x, y, z))
            setMetadata(
                EntityMetadata.ArmorStand.bodyPose,
                Rotations.createWithoutValidityChecks(x, y, z).also {
                    bodyVector = it
                },
            )
        return this
    }

    fun head(x: Float, y: Float, z: Float): NpcEntity {
        if (entityType == EntityType.ARMOR_STAND && shouldUpdate(headVector, x, y, z))
            setMetadata(
                EntityMetadata.ArmorStand.headPose,
                Rotations.createWithoutValidityChecks(x, y, z).also {
                    headVector = it
                },
            )
        //        applyInteractor(EntityMetadataInteractors.ArmorStandInteractors.INSTANCE.getHeadPose(), headVector = Rotations.createWithoutValidityChecks(x, y, z) );
        return this
    }

    fun leftArm(x: Float, y: Float, z: Float): NpcEntity {
        if (entityType == EntityType.ARMOR_STAND && shouldUpdate(leftArmVector, x, y, z))
            setMetadata(
                EntityMetadata.ArmorStand.leftArmPose,
                Rotations.createWithoutValidityChecks(x, y, z).also {
                    leftArmVector = it
                },
            )
        return this
    }

    fun rightArm(x: Float, y: Float, z: Float): NpcEntity {
        if (entityType == EntityType.ARMOR_STAND && shouldUpdate(rightArmVector, x, y, z))
            setMetadata(
                EntityMetadata.ArmorStand.rightArmPose,
                Rotations.createWithoutValidityChecks(x, y, z).also {
                    rightArmVector = it
                },
            )
        return this
    }

    fun leftLeg(x: Float, y: Float, z: Float): NpcEntity {
        if (entityType == EntityType.ARMOR_STAND && shouldUpdate(leftLegVector, x, y, z))
            setMetadata(
                EntityMetadata.ArmorStand.leftLegPose,
                Rotations.createWithoutValidityChecks(x, y, z).also {
                    leftLegVector = it
                },
            )
        return this
    }

    fun rightLeg(x: Float, y: Float, z: Float): NpcEntity {
        if (entityType == EntityType.ARMOR_STAND && shouldUpdate(rightLegVector, x, y, z))
            setMetadata(
                EntityMetadata.ArmorStand.rightLegPose,
                Rotations.createWithoutValidityChecks(x, y, z).also {
                    rightLegVector = it
                },
            )
        return this
    }

    fun customName(name: Component?): NpcEntity {
        val vanillaComponent = name?.let { PaperAdventure.asVanilla(it) }
        setMetadata(
            EntityMetadata.Entity.customName,
            Optional.ofNullable(vanillaComponent)
        )
        return this
    }

    fun customName(name: Component?, to: List<Player>): NpcEntity {
        val vanillaComponent = name?.let { PaperAdventure.asVanilla(it) }
        setPlayerSpecificMetadata(
            EntityMetadata.Entity.customName.accessor,
            Optional.ofNullable(vanillaComponent),
            EntityMetadata.Entity.customName.defaultValue,
            to
        )
        return this
    }

    fun customNameVisible(value: Boolean): NpcEntity {
        setMetadata(EntityMetadata.Entity.customNameVisible, value)
        return this
    }

    fun batHanging(value: Boolean): NpcEntity {
        setByteFlag(EntityMetadata.Bat.flags, EntityBitFlags.BatState.HANGING, value)
        return this
    }


    fun zombieBaby(value: Boolean): NpcEntity {
        setMetadata(EntityMetadata.Zombie.baby, value)
        return this
    }


    fun parrotVariant(value: Parrot.Variant): NpcEntity {
        setMetadata(EntityMetadata.Parrot.variantId, value.ordinal)
        return this
    }

    fun parrotVariant(value: Int): NpcEntity {
        setMetadata(EntityMetadata.Parrot.variantId, value)
        return this
    }

    fun itemstack(item: ItemStack?): NpcEntity {
        if (entityType.entityClass?.isAssignableFrom(ThrowableProjectile::class.java) != true &&
            entityType.entityClass?.isAssignableFrom(Item::class.java) != true
        ) {
//            logcat { "Entity ${entityType} does not support item" }
            return this
        }
        setMetadata(
            EntityMetadata.ItemEntity.item,
            item?.let { CraftItemStack.asNMSCopy(it) } ?: net.minecraft.world.item.ItemStack.EMPTY
        )
        return this
    }

    fun onFire(onFire: Boolean): NpcEntity? {
        setByteFlag(EntityMetadata.Entity.sharedFlags, EntityBitFlags.EntityState.ON_FIRE, onFire)
        return this
    }

    fun guardianMoving(value: Boolean): NpcEntity {
        if (isEntitySubtype(EntityType.GUARDIAN))
            setMetadata(EntityMetadata.Guardian.moving, value)
        return this
    }

    fun mount(otherEntity: Int) {
        lastMount = otherEntity

        val packet = WrapperPlayServerMount()
        packet.entityID = otherEntity
        packet.passengerIds = intArrayOf(entityId)
        npcEntityTracker?.players?.forEach {
            try {
                packet.sendPacket(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun unmount() {
        val lastMount = lastMount ?: return
        val packet = WrapperPlayServerMount()
        packet.entityID = lastMount
        packet.passengerIds = intArrayOf()
        npcEntityTracker?.players?.forEach {
            try {
                packet.sendPacket(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun swingMainArm() {
        val packet = WrapperPlayServerAnimation()
        packet.entityID = entityId
        packet.animation = ClientboundAnimatePacket.SWING_MAIN_HAND
        broadcastPacket(packet)
    }

    fun swingOffHand() {
        val packet = WrapperPlayServerAnimation()
        packet.entityID = entityId
        packet.animation = ClientboundAnimatePacket.SWING_OFF_HAND
        broadcastPacket(packet)
    }

    private fun requireEntitySubtype(type: EntityType) {
        if (!isEntitySubtype(type)) throw IllegalStateException("Entity subtype of $type required for type ${this.entityType}")
    }

    private fun isEntitySubtype(type: EntityType): Boolean {
        return true
    }

    fun generateSynchedEntityData(): SynchedEntityData = generateSynchedEntityData(location)

    companion object {
        private var cachedFakeEntity: ThrownEgg? = null

        fun generateSynchedEntityData(location: Location): SynchedEntityData {
            return SynchedEntityData(getFakeEntity(location))
        }

        fun getFakeEntity(location: Location): ThrownEgg {
            if (cachedFakeEntity != null) return cachedFakeEntity!!
            val world = (location.world as CraftWorld).handle
            cachedFakeEntity = ThrownEgg(world, 0.0, 0.0, 0.0)
            return cachedFakeEntity!!
        }
    }
}