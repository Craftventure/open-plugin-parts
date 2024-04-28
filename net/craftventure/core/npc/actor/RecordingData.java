package net.craftventure.core.npc.actor;

import com.google.gson.annotations.Expose;
import net.craftventure.core.api.CvApi;
import net.craftventure.core.npc.actor.action.ActionDoubleSetting;
import net.craftventure.core.npc.actor.action.ActionStateFlag;
import net.craftventure.core.npc.actor.action.ActorAction;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.EulerAngle;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;


public class RecordingData {
    @Expose
    public List<ActorFrame> frameList = new LinkedList<>();
    @Expose
    public String preferredType;
    @Expose
    public RecordingEquipment equipment;
    @Expose
    public TalkingHeads talkingHeads;
    @Expose
    public String gameProfile = null;
    @Expose
    public boolean repeat = true;
    @Expose
    public long targetDuration;
    @Expose
    @Nullable
    public Long updateTick;
    @Expose
    public List<ActionStateFlag> stateFlags = new LinkedList<>();

    public RecordingData() {
    }

    public RecordingData(Entity entity) {
        preferredType = entity.getType().name();
        repeat = true;
        Location location = entity.getLocation();


        if (entity instanceof LivingEntity)
            equipment = new RecordingEquipment(((LivingEntity) entity).getEquipment());

        frameList.add(new ActorFrame(
                0,
                new ActionDoubleSetting(location.getX()).withType(ActorAction.DoubleSettingType.x),
                ActorAction.Type.DOUBLE_SETTING
        ));
        frameList.add(new ActorFrame(
                0,
                new ActionDoubleSetting(location.getY()).withType(ActorAction.DoubleSettingType.y),
                ActorAction.Type.DOUBLE_SETTING
        ));
        frameList.add(new ActorFrame(
                0,
                new ActionDoubleSetting(location.getZ()).withType(ActorAction.DoubleSettingType.z),
                ActorAction.Type.DOUBLE_SETTING
        ));
        frameList.add(new ActorFrame(
                0,
                new ActionDoubleSetting(location.getYaw()).withType(ActorAction.DoubleSettingType.yaw),
                ActorAction.Type.DOUBLE_SETTING
        ));
        frameList.add(new ActorFrame(
                0,
                new ActionDoubleSetting(location.getPitch()).withType(ActorAction.DoubleSettingType.pitch),
                ActorAction.Type.DOUBLE_SETTING
        ));

        if (entity instanceof ArmorStand) {
            ArmorStand armorStand = (ArmorStand) entity;

            stateFlags.add(new ActionStateFlag(ActionStateFlag.StateType.ARMORSTAND_HIDE_BASEPLATE, "" + armorStand.hasBasePlate()));
            stateFlags.add(new ActionStateFlag(ActionStateFlag.StateType.ARMORSTAND_SHOW_ARMS, "" + armorStand.hasArms()));
            stateFlags.add(new ActionStateFlag(ActionStateFlag.StateType.INVISIBLE, "" + !armorStand.isVisible()));

            addPose(armorStand.getLeftArmPose(), ActorAction.DoubleSettingType.leftArmX);
            addPose(armorStand.getRightArmPose(), ActorAction.DoubleSettingType.rightArmX);
            addPose(armorStand.getLeftLegPose(), ActorAction.DoubleSettingType.leftLegX);
            addPose(armorStand.getRightLegPose(), ActorAction.DoubleSettingType.rightLegX);
            addPose(armorStand.getBodyPose(), ActorAction.DoubleSettingType.bodyX);
            addPose(armorStand.getHeadPose(), ActorAction.DoubleSettingType.headX);
        }
    }

    private void addPose(EulerAngle pose, int index) {
        frameList.add(new ActorFrame(
                0,
                new ActionDoubleSetting(Math.toDegrees(pose.getX())).withType(index),
                ActorAction.Type.DOUBLE_SETTING
        ));
        frameList.add(new ActorFrame(
                0,
                new ActionDoubleSetting(Math.toDegrees(pose.getY())).withType(index + 1),
                ActorAction.Type.DOUBLE_SETTING
        ));
        frameList.add(new ActorFrame(
                0,
                new ActionDoubleSetting(Math.toDegrees(pose.getZ())).withType(index + 2),
                ActorAction.Type.DOUBLE_SETTING
        ));
    }

    public RecordingEquipment getEquipment() {
        return equipment;
    }

    @Nullable
    public Long getUpdateTick() {
        return updateTick;
    }

    public String getPreferredTypeName() {
        return preferredType;
    }

    @Nullable
    public EntityType getPreferredType() {
        try {
            return CvApi.getGsonActor().fromJson(preferredType, EntityType.class);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public TalkingHeads getTalkingHeads() {
        return talkingHeads;
    }

    public void clear() {
        frameList.clear();
    }

    public void add(ActorFrame actorFrame) {
        frameList.add(actorFrame);
    }

    public List<ActionStateFlag> getStateFlags() {
        return stateFlags;
    }

    public List<ActorFrame> getFrameList() {
        return new LinkedList<>(frameList);
    }

    public String getGameProfile() {
        return gameProfile;
    }

    // TODO: Fix getFirstLocation
    @Nullable
    public Location getFirstLocation(World world) {
        Double x = null, y = null, z = null, yaw = null, pitch = null;
        frameList.sort((o1, o2) -> (int) (o1.getTime() - o2.getTime()));
        for (ActorFrame actorFrame : frameList) {
            if (actorFrame.getAction() instanceof ActionDoubleSetting) {
                ActionDoubleSetting actionDoubleSetting = (ActionDoubleSetting) actorFrame.getAction();
                if (x == null && actionDoubleSetting.getType() == ActorAction.DoubleSettingType.x)
                    x = actionDoubleSetting.getValue();
                if (y == null && actionDoubleSetting.getType() == ActorAction.DoubleSettingType.y)
                    y = actionDoubleSetting.getValue();
                if (z == null && actionDoubleSetting.getType() == ActorAction.DoubleSettingType.z)
                    z = actionDoubleSetting.getValue();
                if (yaw == null && actionDoubleSetting.getType() == ActorAction.DoubleSettingType.yaw)
                    yaw = actionDoubleSetting.getValue();
                if (pitch == null && actionDoubleSetting.getType() == ActorAction.DoubleSettingType.pitch)
                    pitch = actionDoubleSetting.getValue();
            }
        }
        if (x != null && y != null && z != null) {
            return new Location(world, x, y, z, yaw != null ? yaw.floatValue() : 0, pitch != null ? pitch.floatValue() : 0);
        }
        return null;
    }

    public boolean isRepeat() {
        return repeat;
    }

    public long getTargetDuration() {
        return targetDuration;
    }

    public void save(File file) throws FileNotFoundException {
        String data = CvApi.getGsonActor().toJson(this);
        PrintWriter out = new PrintWriter(file);
        out.println(data);
        out.close();

//        RecordingData recordingData = getGsonActor().fromJson(data, RecordingData.class);
    }
}
