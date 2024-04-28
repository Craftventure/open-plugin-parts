package net.craftventure.core.npc.actor.action

import com.google.gson.annotations.Expose
import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.util.Translation.Companion.getTranslationByKey
import net.craftventure.chat.bungee.util.parseWithCvMessage
import net.craftventure.core.ktx.util.Logger.capture
import net.craftventure.core.ktx.util.Logger.debug
import net.craftventure.core.ktx.util.Logger.severe
import net.craftventure.core.npc.EntityMetadata
import net.craftventure.core.npc.NpcEntity
import net.minecraft.world.entity.animal.Panda
import org.bukkit.DyeColor
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftTropicalFish
import org.bukkit.entity.Parrot
import org.bukkit.entity.TropicalFish
import java.util.*

@Deprecated("Will be replaced with a better system soon")
@JsonClass(generateAdapter = true)
class ActionStateFlag(
    @field:Expose val type: Int,
    @field:Expose val data: String?
) : ActorAction() {
    override fun executeAction(npcEntity: NpcEntity?) {
        if (type == StateType.FIRE) {
            npcEntity!!.onFire(parseAsBoolean())
        } else if (type == StateType.CUSTOM_NAME) {
            if (data!!.startsWith("translation:")) {
                npcEntity!!.customName(
                    getTranslationByKey(
                        data.replace(
                            "translation:",
                            ""
                        )
                    )!!.translation
                )
            } else npcEntity!!.customName(data.parseWithCvMessage())
        } else if (type == StateType.CUSTOM_NAME_VISIBLE) {
            npcEntity!!.customNameVisible(parseAsBoolean())
//        } else if (type == StateType.CROUCHING) {
//            npcEntity!!.crouching(parseAsBoolean())
//        } else if (type == StateType.EATING) {
//            npcEntity!!.eating(parseAsBoolean())
//        } else if (type == StateType.ARROW_COUNT) {
//            npcEntity!!.arrowCount(parseAsByte())
//        } else if (type == StateType.SPRINTING) {
//            npcEntity!!.sprinting(parseAsBoolean())
//        } else if (type == StateType.GLOWING) {
//            npcEntity!!.glowing(parseAsBoolean())
//        } else if (type == StateType.ELYTRA) {
//            npcEntity!!.elytra(parseAsBoolean())
        } else if (type == StateType.SMALL_ARMOR_STAND) {
            npcEntity!!.armorstandSmall(parseAsBoolean())
//        } else if (type == StateType.HORSE_TAMED) {
//            npcEntity!!.horseTamed(parseAsBoolean())
//        } else if (type == StateType.HORSE_SADDLED) {
//            npcEntity!!.horseSaddled(parseAsBoolean())
//        } else if (type == StateType.HORSE_CHEST) {
//            npcEntity!!.horseChest(parseAsBoolean())
//        } else if (type == StateType.HORSE_EATING) {
//            npcEntity!!.horseEating(parseAsBoolean())
//        } else if (type == StateType.HORSE_REARING) {
//            npcEntity!!.horseRearing(parseAsBoolean())
//        } else if (type == StateType.HORSE_MOUTH_OPEN) {
//            npcEntity!!.horseMouthOpen(parseAsBoolean())
            //        } else if (type == StateType.HORSE_VARIANT) {
//            npcEntity.horseVAz(parseAsInt());
//        } else if (type == StateType.HORSE_COLOR) {
//            npcEntity.horseColor(parseAsInt());
//        } else if (type == StateType.HORSE_ARMOR) {
//            npcEntity!!.horseArmor(parseAsInt())
//        } else if (type == StateType.RABBIT_TYPE) {
//            npcEntity!!.rabbitType(parseAsInt())
//        } else if (type == StateType.TAMED_ANGRY) {
//            npcEntity!!.tamedAngry(parseAsBoolean())
//        } else if (type == StateType.TAMED_SITTING) {
//            npcEntity!!.tamedSit(parseAsBoolean())
//        } else if (type == StateType.TAMED_TAMED) {
//            npcEntity!!.tamedTamed(parseAsBoolean())
//        } else if (type == StateType.SHEEP_COLOR) {
//            npcEntity!!.sheepColor(parseAsDyeColor())
//        } else if (type == StateType.SHEEP_SHEARED) {
//            npcEntity!!.sheepSheared(parseAsBoolean())
//        } else if (type == StateType.POTION_EFFECT_COLOR) {
//            npcEntity!!.potionEffectColor(parseAsInt())
        } else if (type == StateType.INVISIBLE) {
            npcEntity!!.invisible(parseAsBoolean())
        } else if (type == StateType.BAT_HANGING) {
            npcEntity!!.batHanging(parseAsBoolean())
//        } else if (type == StateType.AGEABLE_BABY) {
//            npcEntity!!.ageableBaby(parseAsBoolean())
//        } else if (type == StateType.OCELOT_TYPE) {
//            npcEntity!!.ocelotType(parseAsInt())
//        } else if (type == StateType.WOLF_DAMAGE) {
//            npcEntity!!.wolfDamage(parseAsFloat())
//        } else if (type == StateType.WOLF_BEGGING) {
//            npcEntity!!.wolfBegging(parseAsBoolean())
//        } else if (type == StateType.WOLF_COLLAR_COLOR) {
//            npcEntity!!.wolfCollarColor(parseAsDyeColor())
//        } else if (type == StateType.VILLAGER_TYPE) {
//            npcEntity!!.villagerType(parseAsInt())
//        } else if (type == StateType.IRONGOLEM_PLAYER_CREATED) {
//            npcEntity!!.ironGolemPlayerCreated(parseAsBoolean())
//        } else if (type == StateType.SNOWMAN_HIDE_PUMPKIN) {
//            npcEntity!!.snowmanHidePumpkin(parseAsBoolean())
//        } else if (type == StateType.BLAZE_ON_FIRE) {
//            npcEntity!!.blazeOnFire(parseAsBoolean())
//        } else if (type == StateType.CREEPER_STATE) {
//            npcEntity!!.creeperState(parseAsInt())
//        } else if (type == StateType.CREEPER_CHARGED) {
//            npcEntity!!.creeperCharged(parseAsBoolean())
//        } else if (type == StateType.CREEPER_IGNITED) {
//            npcEntity!!.creeperIgnited(parseAsBoolean())
//        } else if (type == StateType.GUARDIAN_ELDERLY) {
//            npcEntity!!.guardianElderly(parseAsBoolean())
        } else if (type == StateType.GUARDIAN_RETRACTING_SPIKES) {
            npcEntity!!.guardianMoving(parseAsBoolean())
            //} else if (type == StateType.SKELETON_TYPE) {
//            npcEntity.skeletonType(parseAsInt());
//        } else if (type == StateType.SKELETON_SWINGING_ARMS) {
//            npcEntity!!.skeletonSwingingArms(parseAsBoolean())
//        } else if (type == StateType.SPIDER_CLIMBING) {
//            npcEntity!!.spiderClimbing(parseAsBoolean())
//        } else if (type == StateType.WITCH_AGGRESIVE) {
//            npcEntity!!.witchAggresive(parseAsBoolean())
//        } else if (type == StateType.ZOMBIE_BABY) {
//            npcEntity!!.zombieBaby(parseAsBoolean())
            //        } else if (type == StateType.ZOMBIE_TYPE) {
//            npcEntity.zombieType(parseAsInt());
//        } else if (type == StateType.ZOMBIE_CONVERTING) {
//            npcEntity!!.zombieVillagerConverting(parseAsBoolean())
//        } else if (type == StateType.ZOMBIE_HANDS_UP) {
//            npcEntity!!.zombieHandsUp(parseAsBoolean())
//        } else if (type == StateType.ENDERMAN_BLOCK_ID) {
//            if (data != null) {
//                val blockData = data.split(":".toRegex()).toTypedArray()
//                if (blockData.size == 2) {
//                    val material = Material.getMaterial(blockData[0].toUpperCase())
//                    val damage = blockData[1].toShort()
//                    severe(material!!.name + ":" + damage)
//                    npcEntity!!.endermanBlockId(WrappedBlockData.createData(material, damage.toInt()))
//                }
//            }
//        } else if (type == StateType.ENDERMAN_SCREAMING) {
//            npcEntity!!.endermanScreaming(parseAsBoolean())
//        } else if (type == StateType.GHAST_ATTACKING) {
//            npcEntity!!.ghastAttacking(parseAsBoolean())
//        } else if (type == StateType.SLIME_SIZE) {
//            npcEntity!!.slimeSize(parseAsInt())
//        } else if (type == StateType.MINECART_BLOCK_ID_AND_DAMAGE) {
//            npcEntity!!.minecartBlockIdAndDamage(parseAsInt())
//        } else if (type == StateType.MINECART_BLOCK_Y) {
//            npcEntity!!.minecartBlockY(parseAsByte())
//        } else if (type == StateType.MINECART_SHOW_BLOCK) {
//            npcEntity!!.minecartShowBlock(parseAsBoolean())
//        } else if (type == StateType.MINECART_FURNACE) {
//            npcEntity!!.minecartFurnace(parseAsBoolean())
        } else if (type == StateType.ARMORSTAND_HIDE_BASEPLATE) {
            npcEntity!!.hideBasePlate(parseAsBoolean())
        } else if (type == StateType.ARMORSTAND_SHOW_ARMS) {
            npcEntity!!.armorstandShowArms(parseAsBoolean())
//        } else if (type == StateType.POLAR_BEAR_STAND) {
//            npcEntity!!.polarBearStandUp(parseAsBoolean())
        } else if (type == StateType.NO_GRAVITY) {
            npcEntity!!.noGravity(parseAsBoolean())
        } else if (type == StateType.PARROT_VARIANT) {
            try {
                npcEntity!!.parrotVariant(Parrot.Variant.valueOf(data!!.uppercase(Locale.getDefault())))
            } catch (e: IllegalArgumentException) {
                npcEntity!!.parrotVariant(parseAsInt())
            }
        } else if (type == StateType.PUFFER_FISH_STATE) {
            npcEntity!!.applyInteractor(
                EntityMetadata.PufferFish.state,
                parseAsInt(),
            )
        } else if (type == StateType.TROPICAL_FISH_VARIANT) {
            val parts = data?.split(",")?.takeIf { it.size == 3 } ?: return
            val type = TropicalFish.Pattern.valueOf(parts[0].uppercase(Locale.getDefault()))
            val bodyColor = DyeColor.valueOf(parts[1].uppercase(Locale.getDefault()))
            val patternColor = DyeColor.valueOf(parts[2].uppercase(Locale.getDefault()))

            val variant = CraftTropicalFish.getData(patternColor, bodyColor, type)

            npcEntity!!.applyInteractor(
                EntityMetadata.TropicalFish.variant,
                variant,
            )
        } else if (type == StateType.BROWN_PANDA) {
            npcEntity!!.applyInteractor(
                EntityMetadata.Panda.mainGeneId,
                Panda.Gene.BROWN.id.toByte()
            )
            npcEntity.applyInteractor(
                EntityMetadata.Panda.hiddenGeneId,
                Panda.Gene.BROWN.id.toByte()
            )
        } else {
            severe("Unknown type $type")
        }
    }

    private fun parseAsDyeColor(): DyeColor {
        return try {
            DyeColor.valueOf(data!!.uppercase(Locale.getDefault()))
        } catch (e: Exception) {
            if (DEBUG) capture(e)
            DyeColor.WHITE
        }
    }

    private fun parseAsByte(): Byte {
        return try {
            if (data!!.startsWith("#") || data.startsWith("0x")) {
                val numberString = data.replace("#", "").replace("0x", "")
                return numberString.toByte(16)
            }
            data.toByte()
        } catch (e: Exception) {
            if (DEBUG) capture(e)
            0
        }
    }

    private fun parseAsFloat(): Float {
        return try {
            data!!.toFloat()
        } catch (e: Exception) {
            if (DEBUG) capture(e)
            0f
        }
    }

    private fun parseAsInt(): Int {
        return try {
            if (data!!.startsWith("#") || data.startsWith("0x")) {
                val numberString = data.replace("#", "").replace("0x", "")
                return if (data.length == 2) numberString.toByte(16).toInt() else numberString.toInt(16)
            }
            data.toInt()
        } catch (e: Exception) {
            if (DEBUG) capture(e)
            0
        }
    }

    private fun parseAsBoolean(): Boolean {
        if (DEBUG) debug(
            "Value of $type? " + (data.equals(
                "true",
                ignoreCase = true
            ) || data == "1")
        )
        return data.equals("true", ignoreCase = true) || data == "1"
    }

    override val actionTypeId: Int
        get() = Type.STATE_FLAG

    interface StateType {
        companion object {
            const val FIRE = 1
            const val CUSTOM_NAME = 2
            const val CUSTOM_NAME_VISIBLE = 3
            const val CROUCHING = 4
            const val EATING = 5
            const val ARROW_COUNT = 6
            const val SPRINTING = 7
            const val GLOWING = 8
            const val ELYTRA = 9
            const val SMALL_ARMOR_STAND = 10
            const val HORSE_TAMED = 11
            const val HORSE_SADDLED = 12
            const val HORSE_CHEST = 13
            const val HORSE_EATING = 14
            const val HORSE_REARING = 15
            const val HORSE_MOUTH_OPEN = 16
            const val HORSE_VARIANT = 17
            const val HORSE_COLOR = 18
            const val HORSE_ARMOR = 19
            const val RABBIT_TYPE = 20
            const val SHEEP_COLOR = 21
            const val SHEEP_SHEARED = 22
            const val TAMED_SITTING = 23
            const val TAMED_ANGRY = 24
            const val TAMED_TAMED = 25
            const val POTION_EFFECT_COLOR = 26
            const val INVISIBLE = 27
            const val BAT_HANGING = 28
            const val AGEABLE_BABY = 29
            const val OCELOT_TYPE = 30
            const val WOLF_DAMAGE = 31
            const val WOLF_BEGGING = 32
            const val WOLF_COLLAR_COLOR = 33
            const val VILLAGER_TYPE = 34
            const val IRONGOLEM_PLAYER_CREATED = 35
            const val SNOWMAN_HIDE_PUMPKIN = 36
            const val BLAZE_ON_FIRE = 37
            const val CREEPER_STATE = 38
            const val CREEPER_CHARGED = 39
            const val CREEPER_IGNITED = 40
            const val GUARDIAN_RETRACTING_SPIKES = 41
            const val GUARDIAN_ELDERLY = 42
            const val SKELETON_TYPE = 43
            const val SKELETON_SWINGING_ARMS = 44
            const val SPIDER_CLIMBING = 45
            const val WITCH_AGGRESIVE = 46
            const val ZOMBIE_BABY = 47
            const val ZOMBIE_TYPE = 48
            const val ZOMBIE_CONVERTING = 49
            const val ZOMBIE_HANDS_UP = 50
            const val ENDERMAN_BLOCK_ID = 51 // doesnt work due to optional
            const val ENDERMAN_SCREAMING = 52
            const val GHAST_ATTACKING = 53
            const val SLIME_SIZE = 54
            const val MINECART_BLOCK_ID_AND_DAMAGE = 55
            const val MINECART_BLOCK_Y = 56
            const val MINECART_SHOW_BLOCK = 57
            const val MINECART_FURNACE = 58
            const val ARMORSTAND_HIDE_BASEPLATE = 59
            const val ARMORSTAND_SHOW_ARMS = 60
            const val POLAR_BEAR_STAND = 61
            const val NO_GRAVITY = 62
            const val PARROT_VARIANT = 63
            const val PUFFER_FISH_STATE = 64
            const val TROPICAL_FISH_VARIANT = 65
            const val BROWN_PANDA = 66
        }
    }

    companion object {
        private const val DEBUG = false
    }

}