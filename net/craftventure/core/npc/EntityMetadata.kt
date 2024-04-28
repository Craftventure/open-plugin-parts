package net.craftventure.core.npc

import com.comphenix.protocol.wrappers.WrappedDataWatcher
import net.craftventure.core.npc.json.EntityInteractorJson
import net.minecraft.core.Rotations
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.world.entity.*
import net.minecraft.world.entity.animal.Panda.Gene
import net.minecraft.world.entity.npc.VillagerData
import net.minecraft.world.entity.npc.VillagerProfession
import net.minecraft.world.entity.npc.VillagerType
import net.minecraft.world.entity.player.PlayerModelPart
import net.minecraft.world.item.ItemStack
import org.bukkit.entity.EntityType
import org.bukkit.util.BoundingBox
import java.util.*

object EntityMetadata {
    val entityDatawatcherLookup = hashMapOf<Class<net.minecraft.world.entity.Entity>, Set<EntityDataAccessor<*>>>()
    val defaultBoundingBoxes = hashMapOf<EntityType, BoundingBox>()
//    val interactors = hashMapOf<Class<Entity>, Map<String, Interactor<*, *>>>()

    @JvmStatic
    fun prepare() {
    }

    fun getBoundingBox(type: EntityType) = defaultBoundingBoxes[type]

    init {
        EntityType.values().sortedBy { it.name }.forEach { entityType ->
            try {
                val mappedClassData = NmsEntityTypes.entityTypeToClassMap[entityType]
                mappedClassData?.type?.let {
                    val bb = it.getAABB(0.0, 0.0, 0.0)
                    defaultBoundingBoxes[entityType] = BoundingBox(
                        bb.minX,
                        bb.minY,
                        bb.minZ,
                        bb.maxX,
                        bb.maxY,
                        bb.maxZ,
                    )
                }
                val entityClass = mappedClassData?.clazz ?: return@forEach

                val superClasses = getSuperClasses(entityClass)
//                Logger.debug("$entityType Supers ${superClasses.toList().joinToString(", ") { it.simpleName }}")
//                println("object ${entityType}Interactors {")
                superClasses.filter { net.minecraft.world.entity.Entity::class.java.isAssignableFrom(it) && entityDatawatcherLookup[it] == null }
                    .forEach { superClass ->
                        val entityDataAccessorFields = superClass.declaredFields.filter {
                            EntityDataAccessor::class.java.isAssignableFrom(
                                it.type
                            )
                        }
                        entityDataAccessorFields.forEach { field ->
                            field.isAccessible = true
//                            Logger.debug("$entityType ${field.name} ${field.name} (${superClass.name})")
                        }
//                        logcat { "Adding ${superClass.name}" }
                        entityDatawatcherLookup[superClass as Class<net.minecraft.world.entity.Entity>] =
                            entityDataAccessorFields.map { it.get(superClass) as EntityDataAccessor<*> }.toSet()
                    }
//                println("}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
//        logcat { "Initialised EntityMetadata with size=${entityDatawatcherLookup.size}" }
    }

    interface Interactable {
        fun <T> applyInteractor(interactor: Interactor<T>, data: T)
    }

    abstract class InteractorProvider(
        val entityType: Class<out net.minecraft.world.entity.Entity>,
        val internalName: String,
    ) {
        init {
            register(this)
        }

        fun <T> interactor(
            name: String,
            entityTypeSpecificFlagOffset: Int,
            defaultValue: T,
            serializer: EntityDataSerializer<T>,
            flags: Set<ByteFlag> = emptySet(),
        ) = Interactor(entityType, name, entityTypeSpecificFlagOffset, defaultValue, serializer, flags)
    }

    data class ByteFlag(
        val name: String,
        val value: Byte,
    )

    private fun flag(name: String, index: Int) = ByteFlag(name, (1 shl index).toByte())

    object Entity : InteractorProvider(net.minecraft.world.entity.Entity::class.java, "entity") {
        val sharedFlags = interactor(
            "sharedFlags", 0, 0, EntityDataSerializers.BYTE, setOf(
                flag("onFire", 0),
                flag("shiftKeyDown", 1),
                flag("sprinting", 3),
                flag("swimming", 4),
                flag("invisible", 5),
                flag("glowing", 6),
                flag("fallFlying", 7),
            )
        )
        val airSupply = interactor("airSupply", 1, 0, EntityDataSerializers.INT)
        val customName = interactor("customName", 2, Optional.empty(), EntityDataSerializers.OPTIONAL_COMPONENT)
        val customNameVisible = interactor("customNameVisible", 3, false, EntityDataSerializers.BOOLEAN)
        val silent = interactor("silent", 4, false, EntityDataSerializers.BOOLEAN)
        val noGravity = interactor("noGravity", 5, false, EntityDataSerializers.BOOLEAN)
        val pose = interactor("pose", 6, Pose.STANDING, EntityDataSerializers.POSE)
        val frozen = interactor("frozen", 7, 0, EntityDataSerializers.INT)
    }

    object TextDisplay :
        InteractorProvider(net.minecraft.world.entity.Display.TextDisplay::class.java, "text_display") {
        val text = interactor("text", 0, Component.empty(), EntityDataSerializers.COMPONENT)
    }

    object Boat : InteractorProvider(net.minecraft.world.entity.vehicle.Boat::class.java, "boat") {
        val type = interactor("type", 3, 0, EntityDataSerializers.INT)
        val paddleLeft = interactor("paddleLeft", 3, false, EntityDataSerializers.BOOLEAN)
        val paddleRight = interactor("paddleRight", 3, false, EntityDataSerializers.BOOLEAN)
        val bubbleTime = interactor("bubbleTime", 3, 0, EntityDataSerializers.INT)
    }

    object AreaEffectCloud :
        InteractorProvider(net.minecraft.world.entity.AreaEffectCloud::class.java, "areaEffectCloud") {
        val dataRadius = interactor("dataRadius", 0, 0f, EntityDataSerializers.FLOAT)
        val dataColor = interactor("dataColor", 1, 0, EntityDataSerializers.INT)
        val dataWaiting = interactor("dataWaiting", 2, false, EntityDataSerializers.BOOLEAN)
        val dataParticle = interactor("dataParticle", 3, ParticleTypes.ENTITY_EFFECT, EntityDataSerializers.PARTICLE)
    }

    object Mob : InteractorProvider(net.minecraft.world.entity.Mob::class.java, "mob") {
        val flags = interactor(
            "flags", 0, 0, EntityDataSerializers.BYTE, setOf(
                flag("noAi", 0),
                flag("leftHanded", 1),
                flag("aggressive", 2),
            )
        )
    }

    object LivingEntity : InteractorProvider(net.minecraft.world.entity.LivingEntity::class.java, "livingEntity") {
        val flags = interactor(
            "sharedFlags", 0, 0, EntityDataSerializers.BYTE, setOf(
                flag("isUsing", 0),
                flag("offhand", 1),
                flag("spinAttack", 2),
            )
        )
        val health = interactor("health", 1, 20f, EntityDataSerializers.FLOAT)
        val effectColor = interactor("effectColor", 2, 0, EntityDataSerializers.INT)
        val effectAmbience = interactor("effectAmbience", 3, false, EntityDataSerializers.BOOLEAN)
        val arrowCount = interactor("arrowCount", 4, 0, EntityDataSerializers.INT)
        val stingerCount = interactor("stingerCount", 5, 0, EntityDataSerializers.INT)
//        val sleepingPos = interactor("sleepingPos", 6, Optional.empty(), EntityDataSerializers.OPTIONAL_BLOCK_POS)
    }

    object Villager : InteractorProvider(net.minecraft.world.entity.npc.Villager::class.java, "villager") {
        val data = interactor(
            "data",
            0,
            VillagerData(VillagerType.PLAINS, VillagerProfession.NONE, 1),
            EntityDataSerializers.VILLAGER_DATA
        )
    }

    object Phantom : InteractorProvider(net.minecraft.world.entity.monster.Phantom::class.java, "phantom") {
        val size = interactor("size", 0, 0, EntityDataSerializers.INT)
    }

    object Slime : InteractorProvider(net.minecraft.world.entity.monster.Slime::class.java, "slime") {
        val size = interactor("size", 0, 0, EntityDataSerializers.INT)
    }

    object Interaction : InteractorProvider(net.minecraft.world.entity.Interaction::class.java, "interaction") {
        val width = interactor("width", 0, 0f, EntityDataSerializers.FLOAT)
        val height = interactor("height", 1, 0f, EntityDataSerializers.FLOAT)
        val responseId = interactor("responseId", 2, false, EntityDataSerializers.BOOLEAN)
    }

    object Guardian : InteractorProvider(net.minecraft.world.entity.monster.Guardian::class.java, "guardian") {
        val moving = interactor("moving", 0, false, EntityDataSerializers.BOOLEAN)
    }

    object Bat : InteractorProvider(net.minecraft.world.entity.ambient.Bat::class.java, "bat") {
        val flags = interactor(
            "flags", 0, 0, EntityDataSerializers.BYTE, setOf(
                flag("resting", 0),
            )
        )
    }

    object Dolphin : InteractorProvider(net.minecraft.world.entity.animal.Dolphin::class.java, "dolphin") {
        val gotFish = interactor("gotFish", 1, false, EntityDataSerializers.BOOLEAN)
    }

    object Zombie : InteractorProvider(net.minecraft.world.entity.monster.Zombie::class.java, "zombie") {
        val baby = interactor("baby", 0, false, EntityDataSerializers.BOOLEAN)
    }

    object Tamable : InteractorProvider(net.minecraft.world.entity.TamableAnimal::class.java, "parrot") {
        val flags = interactor(
            "flags", 0, 0, EntityDataSerializers.BYTE, setOf(
                flag("sitting", 0),
                flag("unknown", 1),
                flag("tamed", 2),
            )
        )
    }

    object Parrot : InteractorProvider(net.minecraft.world.entity.animal.Parrot::class.java, "parrot") {
        val variantId = interactor("variantId", 0, 0, EntityDataSerializers.INT)
    }

    object ThrowableItemProjectile : InteractorProvider(
        net.minecraft.world.entity.projectile.ThrowableItemProjectile::class.java, "throwableItemProjectile"
    ) {
        val item = interactor("item", 0, ItemStack.EMPTY, EntityDataSerializers.ITEM_STACK)
    }

    object ItemEntity : InteractorProvider(net.minecraft.world.entity.item.ItemEntity::class.java, "item") {
        val item = interactor("item", 0, ItemStack.EMPTY, EntityDataSerializers.ITEM_STACK)
    }

    object Fireworks :
        InteractorProvider(net.minecraft.world.entity.projectile.FireworkRocketEntity::class.java, "fireworkRocket") {
        val item = interactor("item", 0, ItemStack.EMPTY, EntityDataSerializers.ITEM_STACK)
    }

    object Creeper : InteractorProvider(net.minecraft.world.entity.monster.Creeper::class.java, "creeper") {
        val swellDirection = interactor("swellDirection", 0, 0, EntityDataSerializers.INT)
        val isPowered = interactor("isPowered", 1, false, EntityDataSerializers.BOOLEAN)
        val isIgnited = interactor("isIgnited", 2, false, EntityDataSerializers.BOOLEAN)
    }

    object ArmorStand : InteractorProvider(net.minecraft.world.entity.decoration.ArmorStand::class.java, "armorStand") {
        val flags = interactor(
            "flags", 0, 0, EntityDataSerializers.BYTE, setOf(
                flag("small", 0),
                flag("showArms", 2),
                flag("hideBaseplate", 3),
                flag("marker", 4),
            )
        )
        val headPose = interactor("headPose", 1, emptyRotations, EntityDataSerializers.ROTATIONS)
        val bodyPose = interactor("bodyPose", 2, emptyRotations, EntityDataSerializers.ROTATIONS)
        val leftArmPose = interactor("leftArmPose", 3, emptyRotations, EntityDataSerializers.ROTATIONS)
        val rightArmPose = interactor("rightArmPose", 4, emptyRotations, EntityDataSerializers.ROTATIONS)
        val leftLegPose = interactor("leftLegPose", 5, emptyRotations, EntityDataSerializers.ROTATIONS)
        val rightLegPose = interactor("rightLegPose", 6, emptyRotations, EntityDataSerializers.ROTATIONS)
    }

    object Bee : InteractorProvider(net.minecraft.world.entity.animal.Bee::class.java, "bee") {
        val dataFlagsId = interactor("dataFlagsId", 0, 0, EntityDataSerializers.BYTE)
        val angerTime = interactor("angerTime", 1, 0, EntityDataSerializers.INT)
    }

    object PufferFish : InteractorProvider(net.minecraft.world.entity.animal.Pufferfish::class.java, "pufferfish") {
        val state = interactor("state", 0, 0, EntityDataSerializers.INT)
    }

    object TropicalFish :
        InteractorProvider(net.minecraft.world.entity.animal.TropicalFish::class.java, "tropicalFish") {
        val variant = interactor("variant", 0, 0, EntityDataSerializers.INT)
    }

    object Ageable : InteractorProvider(net.minecraft.world.entity.AgeableMob::class.java, "ageable") {
        val baby = interactor("baby", 0, false, EntityDataSerializers.BOOLEAN)
    }

    object EnderDragon :
        InteractorProvider(net.minecraft.world.entity.boss.enderdragon.EnderDragon::class.java, "enderdragon") {
        val dataPhase = interactor("dataPhase", 0, 0, EntityDataSerializers.INT)
    }

    object Panda : InteractorProvider(net.minecraft.world.entity.animal.Panda::class.java, "panda") {
        val unhappyCounter = interactor("unhappyCounter", 0, 0, EntityDataSerializers.INT)
        val sneezeCounter = interactor("sneezeCounter", 1, 0, EntityDataSerializers.INT)
        val eatCounter = interactor("eatCounter", 2, 0, EntityDataSerializers.INT)
        val mainGeneId = interactor("mainGeneId", 3, 0, EntityDataSerializers.BYTE, Gene.entries.map {
            flag(it.serializedName, it.id)
        }.toSet())
        val hiddenGeneId = interactor("hiddenGeneId", 4, 0, EntityDataSerializers.BYTE, Gene.entries.map {
            flag(it.serializedName, it.id)
        }.toSet())
        val dataIdFlags = interactor("dataIdFlags", 5, 0, EntityDataSerializers.BYTE)
    }

    object Player : InteractorProvider(net.minecraft.world.entity.player.Player::class.java, "player") {
        val customization = interactor(
            "customization", 2, Byte.MAX_VALUE, EntityDataSerializers.BYTE, setOf(
                flag("cape", PlayerModelPart.CAPE.bit),
                flag("jacket", PlayerModelPart.JACKET.bit),
                flag("leftSleeve", PlayerModelPart.LEFT_SLEEVE.bit),
                flag("rightSleeve", PlayerModelPart.RIGHT_SLEEVE.bit),
                flag("leftPants", PlayerModelPart.LEFT_PANTS_LEG.bit),
                flag("rightPants", PlayerModelPart.RIGHT_PANTS_LEG.bit),
                flag("hat", PlayerModelPart.HAT.bit),
                flag("all", (1 shl 1) or (1 shl 2) or (1 shl 3) or (1 shl 4) or (1 shl 5) or (1 shl 6) or (1 shl 7)),
            )
        )
        val shoulderLeft = interactor("shoulderLeft", 4, CompoundTag(), EntityDataSerializers.COMPOUND_TAG)
        val shoulderRight = interactor("shoulderRight", 5, CompoundTag(), EntityDataSerializers.COMPOUND_TAG)
    }

    private val emptyRotations get() = Rotations.createWithoutValidityChecks(0f, 0f, 0f)

    //    private val registeredConverters = hashMapOf<String, EntityInteractorJson<Any>>()
    private val registeredProviders = hashSetOf<InteractorProvider>()
    private fun register(provider: InteractorProvider) {
        registeredProviders += provider
    }

    init {
        javaClass.declaredClasses.forEach {
            if (InteractorProvider::class.java.isAssignableFrom(it)) {
                it.declaredFields.firstOrNull { Interactor::class.java.isAssignableFrom(it.type) }?.let { field ->
                    field.isAccessible = true
                    field.get(it) // Hacky way to load all nested object classes that are InteractorProviders
                }
            }
        }

        registeredProviders.forEach { EntityInteractorJson.handle(it) }

        EntityInteractorJson.init()
    }

    fun <E : net.minecraft.world.entity.Entity, T> of(
        type: Class<E>, entityTypeSpecificFlagOffset: Int, serializer: EntityDataSerializer<T>
    ): EntityDataAccessor<T> {
        val absoluteIndex: Int =
            entityDatawatcherLookup[type as Class<net.minecraft.world.entity.Entity>]!!.minByOrNull { it.id }!!
                .let { it.id + entityTypeSpecificFlagOffset }
        return EntityDataAccessor(absoluteIndex, serializer)
    }

    data class Interactor<T>(
        val type: Class<out net.minecraft.world.entity.Entity>,
        val name: String,
        val entityTypeSpecificFlagOffset: Int,
        val defaultValue: T,
        val serializer: EntityDataSerializer<T>,
        val flags: Set<ByteFlag> = emptySet(),
    ) {
//        init {
//            logcat { "Creating interactor for type $type ($name) offset=${entityTypeSpecificFlagOffset} flags=${entityDatawatcherLookup[this.type]?.size}" }
//            logcat { "Test ${entityDatawatcherLookup[net.minecraft.world.entity.Entity::class.java]?.size}" }
//        }

        fun resolveFlag(name: String) = flags.firstOrNull { it.name == name }?.value

        val isValid: Boolean
            get() = absoluteIndex != null

        val accessor =
            entityDatawatcherLookup[this.type]!!.sortedBy { it.id }[entityTypeSpecificFlagOffset] as EntityDataAccessor<T>
        val wrappedSerializer = WrappedDataWatcher.Registry.fromHandle(accessor.serializer)

        val absoluteIndex: Int? =
            entityDatawatcherLookup[this.type]?.minByOrNull { it.id }?.let { it.id + entityTypeSpecificFlagOffset }
    }

    private fun getSuperClasses(clazz: Class<*>): Set<Class<*>> {
        val classes = mutableSetOf<Class<*>>()
        var superClass: Class<*>? = clazz
        do {
            if (superClass == null) continue
            classes += superClass
            superClass = superClass.superclass
        } while (superClass != null)
        return classes
    }
}