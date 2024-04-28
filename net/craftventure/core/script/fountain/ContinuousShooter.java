package net.craftventure.core.script.fountain;

import net.craftventure.core.CraftventureCore;
import net.craftventure.core.animation.IndexedSimpleFrameList;
import net.craftventure.core.animation.keyframed.DoubleValueKeyFrame;
import net.craftventure.core.npc.NpcEntity;
import net.craftventure.core.npc.tracker.NpcEntityTracker;
import net.craftventure.core.utils.InterpolationUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;


public class ContinuousShooter extends Fountain {
    public Location location;
    private Vector calculationVector = new Vector(0, 1, 0);
    private double pressure;
    private NpcEntityTracker npcEntityTracker;
    private int lifeTimeTicks;

    public IndexedSimpleFrameList<DoubleValueKeyFrame> pressureFramesList = new IndexedSimpleFrameList<>();

    public ContinuousShooter(final Location location, double currentPressure, NpcEntityTracker npcEntityTracker, int lifeTimeTicks) {
        super();
        this.location = location;
        pressure(0, currentPressure);
        this.npcEntityTracker = npcEntityTracker;
        this.lifeTimeTicks = lifeTimeTicks;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    public ContinuousShooter pressure(double time, double pitch) {
        pressureFramesList.add(new DoubleValueKeyFrame(time, pitch));
        return this;
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

        pressureFramesList.updateIndexForTime(showTime);

        DoubleValueKeyFrame currentPressureFrame = pressureFramesList.getCurrent();
        DoubleValueKeyFrame nextPressureFrame = pressureFramesList.getNext();
        boolean sameFrames = currentPressureFrame.getValue() == nextPressureFrame.getValue() && currentPressureFrame.getTime() == nextPressureFrame.getTime();
        pressure = sameFrames ? currentPressureFrame.getValue() : InterpolationUtils.linearInterpolate(currentPressureFrame.getValue(), nextPressureFrame.getValue(),
                (showTime - currentPressureFrame.getTime()) / (nextPressureFrame.getTime() - currentPressureFrame.getTime()));

        if (pressure > 0) {
            calculationVector.setY(pressure);

            final NpcEntity npcEntity = new NpcEntity(EntityType.FALLING_BLOCK, location).setBlockData(blockData);
            npcEntityTracker.addEntity(npcEntity);
            npcEntity.velocity(calculationVector);
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
        pressureFramesList.setIndex(0);
    }
}
