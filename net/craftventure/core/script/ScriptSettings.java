package net.craftventure.core.script;

import com.google.gson.annotations.Expose;
import net.craftventure.core.jsonadapter.DescriptorAreaTracker;
import net.craftventure.core.npc.tracker.NpcAreaTracker;

import javax.annotation.Nullable;


public class ScriptSettings {
    @Expose
    private int updateTick;
    @Expose
    private boolean autoStart;
    @Expose
    private String world;
    @Expose
    private DescriptorAreaTracker areaTracker;
    //
    private String groupId;
    private String name;

    public ScriptSettings(int updateTick, boolean autoStart, String world, DescriptorAreaTracker areaTracker) {
        this.updateTick = updateTick;
        this.autoStart = autoStart;
        this.world = world;
        this.areaTracker = areaTracker;
    }

    public boolean isValid() {
        return true;
    }

    @Nullable
    public NpcAreaTracker createAreaTracker() {
        return areaTracker != null ? areaTracker.getAreaTracker(world) : null;
    }

    public int getUpdateTick() {
        return updateTick;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public String getWorld() {
        return world;
    }
}
