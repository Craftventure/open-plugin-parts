package net.craftventure.core.script;

import net.craftventure.core.ktx.util.BackgroundService;
import net.craftventure.core.npc.tracker.NpcAreaTracker;
import net.craftventure.core.script.action.ScriptActionFrame;

import java.util.Collections;


public class ScriptActionPlayback implements BackgroundService.Animatable {
    private final ScriptFrameMap scriptFrameMap;
    private final NpcAreaTracker areaTracker;

    private int actionValueIndex = 0;
    private long animationLength = 0;
    public boolean repeat = true;

    private long currentTime = 0;
    private long lastUpdateTime;

    private long currentTick = 0;
    private long updateTick = 0;

    public ScriptActionPlayback(ScriptFrameMap scriptFrameMap, NpcAreaTracker areaTracker) {
        this.scriptFrameMap = scriptFrameMap;
        this.areaTracker = areaTracker;
        this.updateTick = scriptFrameMap.getUpdateTick();
        repeat = scriptFrameMap.getRepeat();

        Collections.sort(scriptFrameMap.getFrames(), (o1, o2) -> (int) (o1.getTime() - o2.getTime()));
        for (ScriptActionFrame scriptActionFrame : scriptFrameMap.getFrames()) {
            animationLength = Math.max(scriptActionFrame.getTime(), animationLength);
        }
        animationLength = Math.max(animationLength, scriptFrameMap.getTargetDuration());
    }

    public boolean shouldKeepPlaying() {
//        Logger.debug("currentTime=%d animationLength=%d", false, currentTime, animationLength);
        return repeat || currentTime <= animationLength || actionValueIndex < scriptFrameMap.getFrames().size();
    }

    public long getAnimationLength() {
        return animationLength;
    }

    public void setAnimationLength(long animationLength) {
        this.animationLength = animationLength;
    }

    public void reset() {
        actionValueIndex = 0;
        currentTick = 0;
        lastUpdateTime = System.currentTimeMillis();
        currentTime = 0;
    }

    public void play() {
        lastUpdateTime = System.currentTimeMillis();
        BackgroundService.INSTANCE.add(this);
//        Logger.console("Animation start particle");
    }

    public void stop() {
        BackgroundService.INSTANCE.remove(this);
//        Logger.console("Animation stop particle");
    }

    @Override
    public void onAnimationUpdate() {
//        Logger.console("Animation update particle");
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

        if (scriptFrameMap.getFrames().size() > 0 && (scriptFrameMap.getPlayWithoutPlayersInArea() || !areaTracker.getPlayers().isEmpty())) {
            while (actionValueIndex < scriptFrameMap.getFrames().size()) {
                ScriptActionFrame scriptActionFrame = scriptFrameMap.getFrames().get(actionValueIndex);
                if (scriptActionFrame.getTime() < currentTime) {
                    scriptActionFrame.getAction().execute(areaTracker);
                    actionValueIndex++;
                } else {
                    break;
                }
            }
        }

        while (currentTime > animationLength && repeat && animationLength > 0) {
            currentTime -= animationLength;
            actionValueIndex = 0;
        }

        if (animationLength == 0) {
            currentTime = 0;
            actionValueIndex = 0;
        }

//        Logger.console("Play particle for " + currentTime + "ms with " + t);
        lastUpdateTime = newTime;
    }
}
