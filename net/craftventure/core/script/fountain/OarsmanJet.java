package net.craftventure.core.script.fountain;

import net.craftventure.core.CraftventureCore;
import net.craftventure.core.animation.IndexedSimpleFrameList;
import net.craftventure.core.animation.keyframed.DoubleValueKeyFrame;
import net.craftventure.core.npc.NpcEntity;
import net.craftventure.core.npc.tracker.NpcEntityTracker;
import net.craftventure.core.utils.InterpolationUtils;
import net.craftventure.core.utils.MathUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;


public class OarsmanJet extends Fountain {
    public Location location;
    private Vector calculationVector = new Vector(0, 0, 0);
    private double pitch, heading, pressure;
    private NpcEntityTracker npcEntityTracker;
    private int lifeTimeTicks;

    public IndexedSimpleFrameList<DoubleValueKeyFrame> pitchFramesList = new IndexedSimpleFrameList<>();
    public IndexedSimpleFrameList<DoubleValueKeyFrame> headingFramesList = new IndexedSimpleFrameList<>();
    public IndexedSimpleFrameList<DoubleValueKeyFrame> pressureFramesList = new IndexedSimpleFrameList<>();

    public OarsmanJet(final Location location, final double currentHeading, double currentPitch, double currentPressure, NpcEntityTracker npcEntityTracker, int lifeTimeTicks) {
        super();
        this.location = location;
        heading(0, currentHeading);
        pitch(0, currentPitch);
        pressure(0, currentPressure);
        this.npcEntityTracker = npcEntityTracker;
        this.lifeTimeTicks = lifeTimeTicks;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    public void setLifeTimeTicks(int lifeTimeTicks) {
        this.lifeTimeTicks = lifeTimeTicks;
    }

    public OarsmanJet pitch(double time, double pitch) {
        pitchFramesList.add(new DoubleValueKeyFrame(time, pitch));
        return this;
    }

    public OarsmanJet heading(double time, double pitch) {
        headingFramesList.add(new DoubleValueKeyFrame(time, pitch));
        return this;
    }

    public OarsmanJet pressure(double time, double pitch) {
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

        pitchFramesList.updateIndexForTime(showTime);
        headingFramesList.updateIndexForTime(showTime);
        pressureFramesList.updateIndexForTime(showTime);

        DoubleValueKeyFrame currentPressureFrame = pressureFramesList.getCurrent();
        DoubleValueKeyFrame nextPressureFrame = pressureFramesList.getNext();
        boolean sameFrames = currentPressureFrame.getValue() == nextPressureFrame.getValue() && currentPressureFrame.getTime() == nextPressureFrame.getTime();
        pressure = sameFrames ? currentPressureFrame.getValue() : InterpolationUtils.linearInterpolate(currentPressureFrame.getValue(), nextPressureFrame.getValue(),
                (showTime - currentPressureFrame.getTime()) / (nextPressureFrame.getTime() - currentPressureFrame.getTime()));

        if (pressure > 0) {

            DoubleValueKeyFrame currentPitchFrame = pitchFramesList.getCurrent();
            DoubleValueKeyFrame nextPitchFrame = pitchFramesList.getNext();
            sameFrames = currentPitchFrame.getValue() == nextPitchFrame.getValue() && currentPitchFrame.getTime() == nextPitchFrame.getTime();
            pitch = sameFrames ? currentPitchFrame.getValue() : InterpolationUtils.linearInterpolate(currentPitchFrame.getValue(), nextPitchFrame.getValue(),
                    (showTime - currentPitchFrame.getTime()) / (nextPitchFrame.getTime() - currentPitchFrame.getTime()));

            DoubleValueKeyFrame currentHeadingFrame = headingFramesList.getCurrent();
            DoubleValueKeyFrame nextHeadingFrame = headingFramesList.getNext();
            sameFrames = currentHeadingFrame.getValue() == nextHeadingFrame.getValue() && currentHeadingFrame.getTime() == nextHeadingFrame.getTime();
            heading = sameFrames ? currentHeadingFrame.getValue() : InterpolationUtils.linearInterpolate(currentHeadingFrame.getValue(), nextHeadingFrame.getValue(),
                    (showTime - currentHeadingFrame.getTime()) / (nextHeadingFrame.getTime() - currentHeadingFrame.getTime()));

            MathUtil.setYawPitchDegrees(calculationVector, heading, pitch);

            final NpcEntity npcEntity = new NpcEntity(EntityType.FALLING_BLOCK, location).setBlockData(blockData);
            npcEntityTracker.addEntity(npcEntity);
            npcEntity.velocity(calculationVector.multiply(pressure));
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
        pitchFramesList.setIndex(0);
        headingFramesList.setIndex(0);
        pressureFramesList.setIndex(0);
    }
}
