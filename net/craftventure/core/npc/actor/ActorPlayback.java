package net.craftventure.core.npc.actor;

import net.craftventure.core.ktx.util.BackgroundService;
import net.craftventure.core.CraftventureCore;
import net.craftventure.core.ktx.util.Logger;
import net.craftventure.core.npc.NpcEntity;
import net.craftventure.core.npc.actor.action.ActionDoubleSetting;
import net.craftventure.core.npc.actor.action.ActionEquipment;
import net.craftventure.core.npc.actor.action.ActionStateFlag;
import net.craftventure.core.npc.actor.action.ActorAction;
import net.craftventure.database.bukkit.extensions.GameProfileExtensionsKt;
import net.craftventure.database.generated.cvdata.tables.pojos.CachedGameProfile;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public class ActorPlayback implements BackgroundService.Animatable {

    //    private final RecordingData recordingData;
    private final NpcEntity npcEntity;
    private final ActorFrameList<ActorFrame<ActionEquipment>> equipmentFrames = new ActorFrameList<>();
    private final ActorFrameList<ActorFrame<ActionDoubleSetting>> xFrames = new ActorFrameList<>();
    private final ActorFrameList<ActorFrame<ActionDoubleSetting>> yFrames = new ActorFrameList<>();
    private final ActorFrameList<ActorFrame<ActionDoubleSetting>> zFrames = new ActorFrameList<>();
    private final ActorFrameList<ActorFrame<ActionDoubleSetting>> yawFrames = new ActorFrameList<>();
    private final ActorFrameList<ActorFrame<ActionDoubleSetting>> pitchFrames = new ActorFrameList<>();
    private final ActorFrameList<ActorFrame<ActionDoubleSetting>> leftArmXFrames = new ActorFrameList<>();
    private final ActorFrameList<ActorFrame<ActionDoubleSetting>> leftArmYFrames = new ActorFrameList<>();
    private final ActorFrameList<ActorFrame<ActionDoubleSetting>> leftArmZFrames = new ActorFrameList<>();
    private final ActorFrameList<ActorFrame<ActionDoubleSetting>> rightArmXFrames = new ActorFrameList<>();
    private final ActorFrameList<ActorFrame<ActionDoubleSetting>> rightArmYFrames = new ActorFrameList<>();
    private final ActorFrameList<ActorFrame<ActionDoubleSetting>> rightArmZFrames = new ActorFrameList<>();
    private final ActorFrameList<ActorFrame<ActionDoubleSetting>> leftLegXFrames = new ActorFrameList<>();
    private final ActorFrameList<ActorFrame<ActionDoubleSetting>> leftLegYFrames = new ActorFrameList<>();
    private final ActorFrameList<ActorFrame<ActionDoubleSetting>> leftLegZFrames = new ActorFrameList<>();
    private final ActorFrameList<ActorFrame<ActionDoubleSetting>> rightLegXFrames = new ActorFrameList<>();
    private final ActorFrameList<ActorFrame<ActionDoubleSetting>> rightLegYFrames = new ActorFrameList<>();
    private final ActorFrameList<ActorFrame<ActionDoubleSetting>> rightLegZFrames = new ActorFrameList<>();
    private final ActorFrameList<ActorFrame<ActionDoubleSetting>> bodyXFrames = new ActorFrameList<>();
    private final ActorFrameList<ActorFrame<ActionDoubleSetting>> bodyYFrames = new ActorFrameList<>();
    private final ActorFrameList<ActorFrame<ActionDoubleSetting>> bodyZFrames = new ActorFrameList<>();
    private final ActorFrameList<ActorFrame<ActionDoubleSetting>> headXFrames = new ActorFrameList<>();
    private final ActorFrameList<ActorFrame<ActionDoubleSetting>> headYFrames = new ActorFrameList<>();
    private final ActorFrameList<ActorFrame<ActionDoubleSetting>> headZFrames = new ActorFrameList<>();

    private final ActorFrameList.MultiDoubleSettingInterpolator positionInterpolator = new ActorFrameList.MultiDoubleSettingInterpolator(xFrames, yFrames, zFrames, yawFrames, pitchFrames);
    private final ActorFrameList.MultiDoubleSettingInterpolator leftArmInterpolator = new ActorFrameList.MultiDoubleSettingInterpolator(leftArmXFrames, leftArmYFrames, leftArmZFrames);
    private final ActorFrameList.MultiDoubleSettingInterpolator rightArmInterpolator = new ActorFrameList.MultiDoubleSettingInterpolator(rightArmXFrames, rightArmYFrames, rightArmZFrames);
    private final ActorFrameList.MultiDoubleSettingInterpolator leftLegInterpolator = new ActorFrameList.MultiDoubleSettingInterpolator(leftLegXFrames, leftLegYFrames, leftLegZFrames);
    private final ActorFrameList.MultiDoubleSettingInterpolator rightLegInterpolator = new ActorFrameList.MultiDoubleSettingInterpolator(rightLegXFrames, rightLegYFrames, rightLegZFrames);
    private final ActorFrameList.MultiDoubleSettingInterpolator bodyInterpolator = new ActorFrameList.MultiDoubleSettingInterpolator(bodyXFrames, bodyYFrames, bodyZFrames);
    private final ActorFrameList.MultiDoubleSettingInterpolator headInterpolator = new ActorFrameList.MultiDoubleSettingInterpolator(headXFrames, headYFrames, headZFrames);

    private final ActorFrameList<ActorFrame<ActionStateFlag>> stateFlagFrames = new ActorFrameList<>();
    private int animationLength = 0;
    public boolean repeat = true;
    private long currentTime = 0;
    private long lastUpdateTime;
    private long currentTick = 0;
    private long updateTick = 0;

    @Nullable
    private final TalkingHeads talkingHeads;
    @Nullable
    private CachedGameProfile lastTalkingHeadGameProfile;
    private long lastTalkingHeadUpdateTime = 0;
    private final long talkingHeadFrameTime = 200;

    public ActorPlayback(NpcEntity npcEntity, RecordingData recordingData) {
        lastUpdateTime = System.currentTimeMillis();
        this.repeat = recordingData.isRepeat();
        this.talkingHeads = recordingData.getTalkingHeads();

        positionInterpolator.setApplyer(multiDoubleSettingInterpolator -> {
//            Logger.debug("Moving to %.1f %.1f %.1f", false, multiDoubleSettingInterpolator.values[0], multiDoubleSettingInterpolator.values[1], multiDoubleSettingInterpolator.values[2]);
            npcEntity.move(multiDoubleSettingInterpolator.values[0],
                    multiDoubleSettingInterpolator.values[1],
                    multiDoubleSettingInterpolator.values[2],
                    (float) multiDoubleSettingInterpolator.values[3],
                    (float) multiDoubleSettingInterpolator.values[4],
                    (float) multiDoubleSettingInterpolator.values[3]);
        });

        leftArmInterpolator.setApplyer(multiDoubleSettingInterpolator -> npcEntity.leftArm((float) multiDoubleSettingInterpolator.values[0],
                (float) multiDoubleSettingInterpolator.values[1],
                (float) multiDoubleSettingInterpolator.values[2]));
        rightArmInterpolator.setApplyer(multiDoubleSettingInterpolator -> npcEntity.rightArm((float) multiDoubleSettingInterpolator.values[0],
                (float) multiDoubleSettingInterpolator.values[1],
                (float) multiDoubleSettingInterpolator.values[2]));

        leftLegInterpolator.setApplyer(multiDoubleSettingInterpolator -> npcEntity.leftLeg((float) multiDoubleSettingInterpolator.values[0],
                (float) multiDoubleSettingInterpolator.values[1],
                (float) multiDoubleSettingInterpolator.values[2]));
        rightLegInterpolator.setApplyer(multiDoubleSettingInterpolator -> npcEntity.rightLeg((float) multiDoubleSettingInterpolator.values[0],
                (float) multiDoubleSettingInterpolator.values[1],
                (float) multiDoubleSettingInterpolator.values[2]));

        headInterpolator.setApplyer(multiDoubleSettingInterpolator -> npcEntity.head((float) multiDoubleSettingInterpolator.values[0],
                (float) multiDoubleSettingInterpolator.values[1],
                (float) multiDoubleSettingInterpolator.values[2]));
        bodyInterpolator.setApplyer(multiDoubleSettingInterpolator -> npcEntity.body((float) multiDoubleSettingInterpolator.values[0],
                (float) multiDoubleSettingInterpolator.values[1],
                (float) multiDoubleSettingInterpolator.values[2]));

        if (recordingData.getStateFlags() != null) {
            List<ActionStateFlag> stateFlags = recordingData.getStateFlags();
            for (int i = 0; i < stateFlags.size(); i++) {
                ActionStateFlag actionStateFlag = stateFlags.get(i);
                actionStateFlag.executeAction(npcEntity);
            }
        }

        this.npcEntity = npcEntity;
//        this.recordingData = recordingData;

        for (ActorFrame actorFrame : recordingData.getFrameList()) {
//            Logger.debug("Type of frame " + actorFrame.getAction().getClass().getSimpleName() + " at " + actorFrame.getTime() + "ms");
            if (actorFrame.getAction() instanceof ActionEquipment) {
                equipmentFrames.add(actorFrame);
            } else if (actorFrame.getAction() instanceof ActionStateFlag) {
                stateFlagFrames.add(actorFrame);
            } else if (actorFrame.getAction() instanceof ActionDoubleSetting) {
                ActionDoubleSetting actionDoubleSetting = (ActionDoubleSetting) actorFrame.getAction();

//                Logger.debug("  - Typed %d/%d for value %s", false, actionDoubleSetting.getType(), actionDoubleSetting.getActionTypeId(), actionDoubleSetting.getValue());
                if (actionDoubleSetting.getType() == ActorAction.DoubleSettingType.x)
                    xFrames.add(actorFrame);
                if (actionDoubleSetting.getType() == ActorAction.DoubleSettingType.y)
                    yFrames.add(actorFrame);
                if (actionDoubleSetting.getType() == ActorAction.DoubleSettingType.z)
                    zFrames.add(actorFrame);
                if (actionDoubleSetting.getType() == ActorAction.DoubleSettingType.yaw)
                    yawFrames.add(actorFrame);
                if (actionDoubleSetting.getType() == ActorAction.DoubleSettingType.pitch)
                    pitchFrames.add(actorFrame);

                if (npcEntity.getEntityType().getTypeId() == EntityType.ARMOR_STAND.getTypeId()) {
                    if (actionDoubleSetting.getType() == ActorAction.DoubleSettingType.leftArmX)
                        leftArmXFrames.add(actorFrame);
                    if (actionDoubleSetting.getType() == ActorAction.DoubleSettingType.leftArmY)
                        leftArmYFrames.add(actorFrame);
                    if (actionDoubleSetting.getType() == ActorAction.DoubleSettingType.leftArmZ)
                        leftArmZFrames.add(actorFrame);
                    if (actionDoubleSetting.getType() == ActorAction.DoubleSettingType.rightArmX)
                        rightArmXFrames.add(actorFrame);
                    if (actionDoubleSetting.getType() == ActorAction.DoubleSettingType.rightArmY)
                        rightArmYFrames.add(actorFrame);
                    if (actionDoubleSetting.getType() == ActorAction.DoubleSettingType.rightArmZ)
                        rightArmZFrames.add(actorFrame);
                    if (actionDoubleSetting.getType() == ActorAction.DoubleSettingType.leftLegX)
                        leftLegXFrames.add(actorFrame);
                    if (actionDoubleSetting.getType() == ActorAction.DoubleSettingType.leftLegY)
                        leftLegYFrames.add(actorFrame);
                    if (actionDoubleSetting.getType() == ActorAction.DoubleSettingType.leftLegZ)
                        leftLegZFrames.add(actorFrame);
                    if (actionDoubleSetting.getType() == ActorAction.DoubleSettingType.rightLegX)
                        rightLegXFrames.add(actorFrame);
                    if (actionDoubleSetting.getType() == ActorAction.DoubleSettingType.rightLegY)
                        rightLegYFrames.add(actorFrame);
                    if (actionDoubleSetting.getType() == ActorAction.DoubleSettingType.rightLegZ)
                        rightLegZFrames.add(actorFrame);
                    if (actionDoubleSetting.getType() == ActorAction.DoubleSettingType.bodyX)
                        bodyXFrames.add(actorFrame);
                    if (actionDoubleSetting.getType() == ActorAction.DoubleSettingType.bodyY)
                        bodyYFrames.add(actorFrame);
                    if (actionDoubleSetting.getType() == ActorAction.DoubleSettingType.bodyZ)
                        bodyZFrames.add(actorFrame);
                    if (actionDoubleSetting.getType() == ActorAction.DoubleSettingType.headX)
                        headXFrames.add(actorFrame);
                    if (actionDoubleSetting.getType() == ActorAction.DoubleSettingType.headY)
                        headYFrames.add(actorFrame);
                    if (actionDoubleSetting.getType() == ActorAction.DoubleSettingType.headZ)
                        headZFrames.add(actorFrame);
                }
            } else if (actorFrame.getAction() != null) {
                Logger.severe("Unknown ActorFrame class " + actorFrame.getAction().getClass().getSimpleName());
            } else {
                Logger.severe("ActorFrame action = " + actorFrame.getAction());
            }
        }

        equipmentFrames.sortByFrametime();
        stateFlagFrames.sortByFrametime();

        xFrames.sortByFrametime();
        yFrames.sortByFrametime();
        zFrames.sortByFrametime();
        yawFrames.sortByFrametime();
        pitchFrames.sortByFrametime();

        leftArmXFrames.sortByFrametime();
        leftArmYFrames.sortByFrametime();
        leftArmZFrames.sortByFrametime();
        rightArmXFrames.sortByFrametime();
        rightArmYFrames.sortByFrametime();
        rightArmZFrames.sortByFrametime();
        leftLegXFrames.sortByFrametime();
        leftLegYFrames.sortByFrametime();
        leftLegZFrames.sortByFrametime();
        rightLegXFrames.sortByFrametime();
        rightLegYFrames.sortByFrametime();
        rightLegZFrames.sortByFrametime();
        bodyXFrames.sortByFrametime();
        bodyYFrames.sortByFrametime();
        bodyZFrames.sortByFrametime();
        headXFrames.sortByFrametime();
        headYFrames.sortByFrametime();
        headZFrames.sortByFrametime();

        if (!equipmentFrames.isEmpty())
            animationLength = Math.max(animationLength, (int) equipmentFrames.get(equipmentFrames.size() - 1).getTime());
        if (!stateFlagFrames.isEmpty())
            animationLength = Math.max(animationLength, (int) stateFlagFrames.get(stateFlagFrames.size() - 1).getTime());
        if (!xFrames.isEmpty())
            animationLength = Math.max(animationLength, (int) xFrames.get(xFrames.size() - 1).getTime());
        if (!yFrames.isEmpty())
            animationLength = Math.max(animationLength, (int) yFrames.get(yFrames.size() - 1).getTime());
        if (!zFrames.isEmpty())
            animationLength = Math.max(animationLength, (int) zFrames.get(zFrames.size() - 1).getTime());
        if (!yawFrames.isEmpty())
            animationLength = Math.max(animationLength, (int) yawFrames.get(yawFrames.size() - 1).getTime());
        if (!pitchFrames.isEmpty())
            animationLength = Math.max(animationLength, (int) pitchFrames.get(pitchFrames.size() - 1).getTime());
        if (!leftArmXFrames.isEmpty())
            animationLength = Math.max(animationLength, (int) leftArmXFrames.get(leftArmXFrames.size() - 1).getTime());
        if (!leftArmYFrames.isEmpty())
            animationLength = Math.max(animationLength, (int) leftArmYFrames.get(leftArmYFrames.size() - 1).getTime());
        if (!leftArmZFrames.isEmpty())
            animationLength = Math.max(animationLength, (int) leftArmZFrames.get(leftArmZFrames.size() - 1).getTime());
        if (!rightArmXFrames.isEmpty())
            animationLength = Math.max(animationLength, (int) rightArmXFrames.get(rightArmXFrames.size() - 1).getTime());
        if (!rightArmYFrames.isEmpty())
            animationLength = Math.max(animationLength, (int) rightArmYFrames.get(rightArmYFrames.size() - 1).getTime());
        if (!rightArmZFrames.isEmpty())
            animationLength = Math.max(animationLength, (int) rightArmZFrames.get(rightArmZFrames.size() - 1).getTime());
        if (!leftLegXFrames.isEmpty())
            animationLength = Math.max(animationLength, (int) leftLegXFrames.get(leftLegXFrames.size() - 1).getTime());
        if (!leftLegYFrames.isEmpty())
            animationLength = Math.max(animationLength, (int) leftLegYFrames.get(leftLegYFrames.size() - 1).getTime());
        if (!leftLegZFrames.isEmpty())
            animationLength = Math.max(animationLength, (int) leftLegZFrames.get(leftLegZFrames.size() - 1).getTime());
        if (!rightLegXFrames.isEmpty())
            animationLength = Math.max(animationLength, (int) rightLegXFrames.get(rightLegXFrames.size() - 1).getTime());
        if (!rightLegYFrames.isEmpty())
            animationLength = Math.max(animationLength, (int) rightLegYFrames.get(rightLegYFrames.size() - 1).getTime());
        if (!rightLegZFrames.isEmpty())
            animationLength = Math.max(animationLength, (int) rightLegZFrames.get(rightLegZFrames.size() - 1).getTime());
        if (!bodyXFrames.isEmpty())
            animationLength = Math.max(animationLength, (int) bodyXFrames.get(bodyXFrames.size() - 1).getTime());
        if (!bodyYFrames.isEmpty())
            animationLength = Math.max(animationLength, (int) bodyYFrames.get(bodyYFrames.size() - 1).getTime());
        if (!bodyZFrames.isEmpty())
            animationLength = Math.max(animationLength, (int) bodyZFrames.get(bodyZFrames.size() - 1).getTime());
        if (!headXFrames.isEmpty())
            animationLength = Math.max(animationLength, (int) headXFrames.get(headXFrames.size() - 1).getTime());
        if (!headYFrames.isEmpty())
            animationLength = Math.max(animationLength, (int) headYFrames.get(headYFrames.size() - 1).getTime());
        if (!headZFrames.isEmpty())
            animationLength = Math.max(animationLength, (int) headZFrames.get(headZFrames.size() - 1).getTime());

        animationLength = Math.max(animationLength, (int) recordingData.getTargetDuration());
    }

    public NpcEntity getNpcEntity() {
        return npcEntity;
    }

    public boolean shouldKeepPlaying() {
        return repeat || currentTime <= animationLength;
    }

    public void setUpdateTick(long updateTick) {
        this.updateTick = updateTick;
    }

    public void reset() {
        resetFramesAndTimes();
        lastUpdateTime = System.currentTimeMillis();
        currentTime = 0;
        currentTick = 0;
        positionInterpolator.apply(currentTime, npcEntity);
    }

    @Override
    public void onAnimationUpdate() {
//        Logger.console("Animation update actor");
        if (updateTick > 0) {
            currentTick++;
            if (currentTick == updateTick) {
                currentTick = 0;
            } else {
                return;
            }
        }
        long newTime = System.currentTimeMillis();
        double delta = newTime - lastUpdateTime;

        // Time rollbacked?
        if (delta < 0) {
            lastUpdateTime = newTime;
            return;
        }

        currentTime += delta;
        if (currentTime > animationLength && repeat) {
            resetFramesAndTimes();
        }
        while (currentTime > animationLength && repeat && animationLength > 0) {
            currentTime -= animationLength;
        }
        if (animationLength == 0)
            currentTime = 0;

        equipmentFrames.apply(currentTime, npcEntity);
        stateFlagFrames.apply(currentTime, npcEntity);

        positionInterpolator.apply(currentTime, npcEntity);

        if (talkingHeads != null) {
            if (lastTalkingHeadUpdateTime < currentTime - talkingHeadFrameTime) {
                TalkingHeads.ProfileGroup currentProfileGroup = talkingHeads.getCurrentProfileGroup(currentTime);
                if (currentProfileGroup != null) {
                    CachedGameProfile[] cachedProfiles = currentProfileGroup.getCachedProfiles();
                    if (cachedProfiles.length > 0) {
//                        Logger.debug("Has profile for %s", false, currentTime);
                        CachedGameProfile nextProfile = cachedProfiles[CraftventureCore.getRandom().nextInt(cachedProfiles.length)];
                        if (lastTalkingHeadGameProfile == null || !lastTalkingHeadGameProfile.getId().equals(nextProfile.getId())) {
                            lastTalkingHeadGameProfile = nextProfile;
                            ItemStack skullItem = GameProfileExtensionsKt.getSkullItem(nextProfile);
                            if (skullItem != null) {
                                npcEntity.helmet(skullItem);
                                lastTalkingHeadUpdateTime = currentTime;
                            }
                        } else {
                        }
                    }
                }
            }
        }

        if (npcEntity.getEntityType().getTypeId() == EntityType.ARMOR_STAND.getTypeId()) {
            leftArmInterpolator.apply(currentTime, npcEntity);
            rightArmInterpolator.apply(currentTime, npcEntity);
            leftLegInterpolator.apply(currentTime, npcEntity);
            rightLegInterpolator.apply(currentTime, npcEntity);
            bodyInterpolator.apply(currentTime, npcEntity);
            headInterpolator.apply(currentTime, npcEntity);
        }

        lastUpdateTime = newTime;
    }

    private void resetFramesAndTimes() {
        lastUpdateTime = System.currentTimeMillis();

        equipmentFrames.resetIndex();
        stateFlagFrames.resetIndex();

        xFrames.resetIndex();
        yFrames.resetIndex();
        zFrames.resetIndex();
        yawFrames.resetIndex();
        pitchFrames.resetIndex();

        if (npcEntity.getEntityType().getTypeId() == EntityType.ARMOR_STAND.getTypeId()) {
            leftArmXFrames.resetIndex();
            leftArmYFrames.resetIndex();
            leftArmZFrames.resetIndex();
            rightArmXFrames.resetIndex();
            rightArmYFrames.resetIndex();
            rightArmZFrames.resetIndex();
            leftLegXFrames.resetIndex();
            leftLegYFrames.resetIndex();
            leftLegZFrames.resetIndex();
            rightLegXFrames.resetIndex();
            rightLegYFrames.resetIndex();
            rightLegZFrames.resetIndex();
            bodyXFrames.resetIndex();
            bodyYFrames.resetIndex();
            bodyZFrames.resetIndex();
            headXFrames.resetIndex();
            headYFrames.resetIndex();
            headZFrames.resetIndex();
        }
        lastTalkingHeadUpdateTime = 0;
    }

    public void play() {
        lastUpdateTime = System.currentTimeMillis();
        BackgroundService.INSTANCE.add(this);
    }

    public void stop() {
        BackgroundService.INSTANCE.remove(this);
    }
}
