package net.craftventure.core.npc;

import kotlin.Deprecated;

@Deprecated(message = "Scheduled for removal")
public final class EntityMetadataIndices {
    public interface Entity {
        int STATE = 0; // byte
        int AIR = 1; // VarInt(short)
        int NAME_TAG = 2; // string
        int ALWAYS_SHOW_NAME_TAG = 3; // byte
        int SILENT = 4; // byte
        int NO_GRAVITY = 5; // byte
        int POSE = 6; // pose
    }

    public interface Fireworks extends Entity {
        int FIREWORKS_INFO = 8; // slot
    }

    public interface Item extends Entity {
        int ITEM_SLOT = 8; // slot
    }

    public interface Living extends Entity {
        int LIVING_FLAGS = 8; // byte
        int LIVING_HEALTH = 9; // float
        int LIVING_POTION_EFFECT_COLOR = 10; // VarInt
        int LIVING_POTION_AMBIENT = 11; // boolean
        int LIVING_ARROW_COUNT = 12; // VarInt
        int ABSOPTION_AMOUNT = 13; // VarInt
        int SLEEP_LOCATION = 14; // OptBlockPos
    }

    public interface Player extends Living {
        int PLAYER_ADDITIONAL_HEARTS = 15; // float
        int PLAYER_SCORE = 16; // VarInt
        int PLAYER_SKIN_FLAGS = 17; // byte
        int PLAYER_MAIN_HAND = 1; // byte (0 = left, 1 = right)
    }

    public interface ArmorStand extends Living {
        int ARMORSTAND_STATE = 15; // byte
        int ARMORSTAND_HEAD_ROTATION = 16; // float, float, float
        int ARMORSTAND_BODY_ROTATION = 17; // float, float, float
        int ARMORSTAND_LEFT_ARM_ROTATION = 18; // float, float, float
        int ARMORSTAND_RIGHT_ARM_ROTATION = 19; // float, float, float
        int ARMORSTAND_LEFT_LEG_ROTATION = 20; // float, float, float
        int ARMORSTAND_RIGHT_LEG_ROTATION = 21; // float, float, float
    }

    public interface Insentient extends Living {
        int INSENTIENT_FLAGS = 14; // byte
    }

    public interface Ambient extends Living {

    }

    public interface Bat extends Ambient {
        int BAT_FLAGS = 16; // byte
    }

    public interface Creature extends Insentient {

    }

    public interface Ageable extends Creature {
        int AGEABLE_BABY = 16; // boolean
    }

    public interface Animal extends Ageable {

    }

    public interface AbstractHorse extends Animal {
        int ABSTRACT_HORSE_FLAGS = 17; // byte
        int ABSTRACT_HORSE_OWNER = 18; // OptUuid
    }

    public interface Horse extends AbstractHorse {
        int HORSE_VARIANT = 19; // VarInt
        int HORSE_ARMOR = 20; // VarInt
    }

    public interface Rabbit extends Animal {
        int RABBIT_TYPE = 17; // VarInt
    }

    public interface PolarBear extends Animal {
        int STANDING_UP = 17; // boolean
    }

    public interface Sheep extends Animal {
        int SHEEP_FLAGS = 17; // byte
    }

    public interface TameableAnimal extends Animal {
        int TAMEABLE_STATE = 17; // byte
        int OWNER = 18; // OptUuid
    }

    public interface Ocelot extends TameableAnimal {
        int OCELOT_TYPE = 18; // int
    }

    public interface Wolf extends TameableAnimal {
        int WOLF_DAMAGE = 16; // float
        int WOLF_BEGGING = 17; // boolean
        int WOLF_COLLAR_COLOR = 18; // byte?
    }

    public interface Parrot extends TameableAnimal {
        int VARIANT = 18; // float
    }

    public interface Villager extends Ageable {
        int VILLAGER_TYPE = 13;
    }

    public interface Golem extends Creature {

    }

    public interface IronGolem extends Golem {
        int IRONGOLEM_FLAGS = 14; // byte
    }

    public interface Snowman extends Golem {
        int SNOWMAN_FLAGS = 14; // byte
    }

    public interface Monster extends Creature {

    }

    public interface Blaze extends Monster {
        int BLAZE_FLAGS = 15;
    }

    public interface Creeper extends Monster {
        int CREEPER_STATE = 16; // byte, -1 = idle, 1 = fuse
        int CREEPER_CHARED = 17; // boolean
        int CREEPER_IGNITED = 18; // boolean
    }

    public interface Guardian extends Monster {
        int GUARDIAN_FLAGS = 15; // byte
        int TARGET_ID = 16; // VarInt
    }

    public interface Skeleton extends Monster {
        int SKELETON_SWINGING_ARMS = 15; // boolean
    }

    public interface Spider extends Monster {
        int SPIDER_FLAGS = 14; // byte
    }

    public interface Witch extends Monster {
        int WITCH_AGGRESIVE = 14; // boolean
    }

    public interface Zombie extends Monster {
        int ZOMBIE_BABY = 15; // boolean
        //        int ZOMBIE_UNUSED = 16; // VarInt
        int ZOMBIE_HANDS_UP = 17; // boolean
    }

    public interface ZombieVillager extends Zombie {
        int ZOMBIE_VILLAGER_CONVERTING = 15; // boolean
        int ZOMBIE_VILLAGER_PROFESSION = 16; // VarInt
    }

    public interface Enderman extends Monster {
        int ENDERMAN_BLOCK_ID = 12; // blockid
        int ENDERMAN_SCREAMING = 13; // boolean
    }

    public interface Flying extends Insentient {

    }

    public interface Ghast extends Flying {
        int GHAST_ATTACKING = 15; // boolean
    }

    public interface Slime extends Insentient {
        int SLIME_SIZE = 12; // byte
    }

    public interface Minecart extends Entity {
        int MINECART_SHAKING_POWER = 6; // byte
        int MINECART_SHAKING_DIRECTION = 7; // byte
        int MINECART_SHAKING_MULTIPLIER = 8; // float
        int MINECART_BLOCK_ID_AND_DAMAGE = 9; // int or short?
        int MINECART_BLOCK_Y = 10; // byte?
        int MINECART_SHOW_BLOCK = 11; // boolean
    }

    public interface MinecartFurnace extends Minecart {
        int MINECART_FURNACE_POWERED = 12; // boolean
    }
}
