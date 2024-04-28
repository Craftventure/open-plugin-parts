package net.craftventure.core.npc;

@Deprecated
public class EntityBitFlags {
    public interface EntityState {
        byte ON_FIRE = 0x01;
        byte CROUCHED = 0x02;
        byte SPRINTING = 0x08;
        byte EATING = 0x10;
        byte INVISIBLE = 0x20;
        byte GLOWING = 0x40;
        byte FLYING_ELYTRA = (byte) 0x80;
    }

    public interface PlayerSkinFlags {
        byte CAPE = 0x01;
        byte JACKET = 0x02;
        byte LEFT_SLEEVE = 0x04;
        byte RIGHT_SLEEVE = 0x08;
        byte LEFT_PANTS = 0x10;
        byte RIGHT_PANTS = 0x20;
        byte HAT = 0x40;
    }

    public interface ArmorStandState {
        byte SMALL = 0x01;
        byte ARMS = 0x04;
        byte HIDE_BASEPLATE = 0x08;
        byte MARKER = 0x16;
    }

    public interface ArrowState {
        byte CRITICAL = 0x01;
    }

    public interface LivingState {
        byte HAND_ACTIVE = 0x01;
        byte ACTIVE_HAND = 0x02;
    }

    public interface InsentientState {
        byte NO_AI = 0x01;
        byte LEFT_HANDED = 0x02;
    }

    public interface BatState {
        byte HANGING = 0x01;
    }

    public interface HorseState {
        byte TAMED = 0x2;
        byte SADDLED = 0x04;
        byte CHEST = 0x08;
        byte EATING = 0x20;
        byte REARING = 0x40;
        byte MOUTH_OPEN = (byte) 0x80;
    }

    public interface SheepState {
        byte SHEARED = (byte) 0x10;
        byte COLOR = 0x0F;
    }

    public interface TameableAnimalState {
        byte SITTING = 0x01;
        byte ANGRY = 0x02;
        byte TAMED = 0x04;
    }

    public interface IronGolem {
        byte PLAYER_CREATED = 0x01;
    }

    public interface Snowman {
        byte NO_PUMPKIN_HAT = 0x10;
    }

    public interface EvocationIllagerSpell {
        byte NONE = 0;
        byte SUMMON_VEX = 1;
        byte ATTACK = 2;
        byte WOLOLO = 3;
    }

    public interface VexState {
        byte ATTACK_MODE = 0x01;
    }

    public interface VindicationIllagerState {
        byte HAS_TARGET = 0x01;
    }

    public interface Blaze {
        byte ON_FIRE = 0x01;
    }

    public interface Guardian {
        byte RETRACTING_SPIKES = 0x02;
        byte ELDERY = 0x04;
    }

    public interface Spider {
        byte CLIMBING = 0x01;
    }

    public interface Bee {
        byte IS_ANGRY = 0x02;
        byte HAS_STUNG = 0x04;
        byte HAS_NECTAR = 0x08;
    }
}
