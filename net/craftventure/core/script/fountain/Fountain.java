package net.craftventure.core.script.fountain;

import net.craftventure.core.animation.IndexedSimpleFrameList;
import net.craftventure.core.animation.keyframed.BooleanValueKeyFrame;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;


public abstract class Fountain {
    protected boolean lowPrecision = false;
    protected boolean doTick = true;
    protected BlockData blockData = Material.BLUE_TERRACOTTA.createBlockData();
    public IndexedSimpleFrameList<BooleanValueKeyFrame> playStopFramesList = new IndexedSimpleFrameList<>();
    public String name;

    public Fountain() {
        stopAt(0);
    }

    public abstract Location getLocation();

    public BlockData getBlockData() {
        return blockData;
    }

    public void setBlockData(BlockData blockData) {
        this.blockData = blockData;
    }

    public abstract void update(double showTime);

    // Can be implemented by fountains to optimize their performance by doing stuff like precaching lists etc
    public abstract void compile();

    public abstract void cleanup();

    public abstract void reset();

    public void setLowPrecision() {
        this.lowPrecision = true;
    }

    public Fountain playFor(double startTime, double playTime) {
        return play(startTime, startTime + playTime);
    }

    public Fountain play(double startTime, double stopTime) {
        if (startTime == 0) {
            if (playStopFramesList.size() > 0)
                playStopFramesList.remove(0);
            playStopFramesList.add(0, new BooleanValueKeyFrame(startTime, true));
        } else {
            playStopFramesList.add(new BooleanValueKeyFrame(startTime, true));
        }
        playStopFramesList.add(new BooleanValueKeyFrame(stopTime, false));
        return this;
    }

    public Fountain playAt(double time) {
        if (time == 0) {
            if (playStopFramesList.size() > 0)
                playStopFramesList.remove(0);
            playStopFramesList.add(0, new BooleanValueKeyFrame(time, true));
            return this;
        }
        playStopFramesList.add(new BooleanValueKeyFrame(time, true));
        return this;
    }

    public Fountain stopAt(double time) {
        if (time == 0) {
            if (playStopFramesList.size() > 0)
                playStopFramesList.remove(0);
            playStopFramesList.add(0, new BooleanValueKeyFrame(time, false));
            return this;
        }
        playStopFramesList.add(new BooleanValueKeyFrame(time, false));
        return this;
    }
}
