package net.craftventure.core.script.fountain;

import net.craftventure.core.CraftventureCore;
import net.craftventure.core.npc.NpcEntity;
import net.craftventure.core.npc.tracker.NpcEntityTracker;
import net.craftventure.core.utils.MathUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;


public class BloomJet extends Fountain {
    public Location location;
    public double pressure;
    public double pitch;
    private Vector[] vectors;
    public int rayAmount;
    private NpcEntityTracker npcEntityTracker;
    private int lifeTimeTicks;

    public BloomJet(Location location, int rayAmount, double pitch, double pressure, NpcEntityTracker npcEntityTracker, int lifeTimeTicks) {
        super();
        this.location = location;
        this.rayAmount = rayAmount;
        this.pressure = pressure;
        this.pitch = pitch;
        this.vectors = new Vector[rayAmount];

        float currentYaw = 0;
        float yawIncreasePerFountain = 360 / (float) rayAmount;

        for (int i = 0; i < rayAmount; i++) {
            Vector vector = new Vector(0, 0, 0);
            this.vectors[i] = MathUtil.setYawPitchDegrees(vector, currentYaw, pitch).multiply(pressure);
            currentYaw += yawIncreasePerFountain;
        }
        this.npcEntityTracker = npcEntityTracker;
        this.lifeTimeTicks = lifeTimeTicks;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public void update(double showTime) {
        if (lowPrecision)
            doTick = !doTick;

        if (lowPrecision && !doTick)
            return;

        playStopFramesList.updateIndexForTime(showTime);

        if (!playStopFramesList.getCurrent().getValue())
            return;

        for (int i = 0; i < vectors.length; i++) {
            Vector vector = vectors[i];
            final NpcEntity npcEntity = new NpcEntity(EntityType.FALLING_BLOCK, location).setBlockData(blockData);
            npcEntityTracker.addEntity(npcEntity);
            npcEntity.velocity(vector);
            Bukkit.getScheduler().scheduleSyncDelayedTask(CraftventureCore.getInstance(), new Runnable() {
                @Override
                public void run() {
                    npcEntityTracker.removeEntity(npcEntity);
                }
            }, lifeTimeTicks);
        }
    }

    @Override
    public void compile() {

    }

    @Override
    public void cleanup() {

    }

    @Override
    public void reset() {
        playStopFramesList.setIndex(0);
    }
}
