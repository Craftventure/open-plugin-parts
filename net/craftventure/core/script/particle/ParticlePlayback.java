package net.craftventure.core.script.particle;

import net.craftventure.core.ktx.util.BackgroundService;
import net.craftventure.core.npc.tracker.NpcAreaTracker;
import net.craftventure.core.ride.trackedride.SplineHandle;
import net.craftventure.core.ride.trackedride.SplineHelper;
import net.craftventure.core.ride.trackedride.SplineNode;
import net.craftventure.core.utils.InterpolationUtils;
import net.craftventure.core.utils.ParticleSpawner;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ParticlePlayback implements BackgroundService.Animatable {
    private final ParticlePath particlePath;
    private final NpcAreaTracker areaTracker;

    private int particleValueIndex = 0;
    private long animationLength = 0;
    public boolean repeat = true;

    private long currentTime = 0;
    private long lastUpdateTime;

    private long currentTick = 0;
    private long updateTick = 0;

    private final List<ParticleSplineNode> splineNodes = new ArrayList<>();

    public ParticlePlayback(ParticlePath particlePath, NpcAreaTracker areaTracker, long baseUpdateTick) {
        this.particlePath = particlePath;
        this.areaTracker = areaTracker;
        this.updateTick = particlePath.getUpdateTick() != null ? particlePath.getUpdateTick() : baseUpdateTick;
        repeat = particlePath.getRepeat();
        animationLength = particlePath.getTargetDuration();

        List<ParticleValue> nodes = particlePath.getNodes();
        Collections.sort(nodes, (o1, o2) -> (int) (o1.getAt() - o2.getAt()));
        if (nodes.size() > 0)
            animationLength = Math.max(animationLength, nodes.get(nodes.size() - 1).getAt());
        for (int i = 0; i < nodes.size(); i++) {
            ParticleValue particleValue = nodes.get(i);
            SplineNode splineNode = new SplineNode(new SplineHandle(particleValue.getX(), particleValue.getY(), particleValue.getZ()),
                    new SplineHandle(particleValue.getX(), particleValue.getY(), particleValue.getZ()),
                    new SplineHandle(particleValue.getX(), particleValue.getY(), particleValue.getZ()));
            splineNodes.add(new ParticleSplineNode(particleValue, splineNode));
        }


        for (int i = 0; i < nodes.size(); i++) {
            ParticleSplineNode currentNode = splineNodes.get(i);
            if (i > 0 && i < nodes.size() - 1) {
                ParticleSplineNode previous = splineNodes.get(i - 1);
                ParticleSplineNode next = splineNodes.get(i + 1);
                currentNode.getSplineNode().calculateInAndOut(previous.getSplineNode(), next.getSplineNode(), currentNode.getParticleValue().getTension());
            }
        }
    }

    public boolean shouldKeepPlaying() {
        return repeat || currentTime <= animationLength;
    }

    public void reset() {
        particleValueIndex = 0;
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
        if (splineNodes.size() == 1) {
            ParticleSplineNode particleSplineNode = splineNodes.get(0);
            ParticleValue particleValue = particleSplineNode.getParticleValue();

            ParticleSpawner.INSTANCE.spawnParticle(areaTracker.getWorld(),
                    particlePath.getParticleType(),
                    (float) particleValue.getX(),
                    (float) particleValue.getY(),
                    (float) particleValue.getZ(),
                    particleValue.getParticleCount(),
                    (float) particleValue.getOffsetX(),
                    (float) particleValue.getOffsetY(),
                    (float) particleValue.getOffsetZ(),
                    particleValue.getParticleData(),
                    particleValue.getParticleExtraData(),
                    particlePath.getLongDistance(),
                    null,
                    areaTracker.getPlayers(),
                    null
            );
        } else {
            long newTime = System.currentTimeMillis();
            double delta = newTime - lastUpdateTime;

            // Time rollbacked?
            if (delta < 0) {
                lastUpdateTime = newTime;
                return;
            }

            currentTime += delta;
            while (currentTime > animationLength && repeat && animationLength > 0) {
                currentTime -= animationLength;
                particleValueIndex = 0;
            }
            if (animationLength == 0) {
                currentTime = 0;
                particleValueIndex = 0;
            }

            if (splineNodes.size() > 0 && areaTracker.getPlayers().size() > 0) {
                while (particleValueIndex + 1 < splineNodes.size()) {
                    ParticleSplineNode a = splineNodes.get(particleValueIndex + 1);
                    if (a.getParticleValue().getAt() < currentTime) {
                        particleValueIndex++;
                    } else {
                        break;
                    }
                }
                if (particleValueIndex + 1 < splineNodes.size()) {
                    ParticleSplineNode a = splineNodes.get(particleValueIndex);
                    ParticleSplineNode b = splineNodes.get(particleValueIndex + 1);
                    if (areaTracker.getPlayers().size() > 0 && currentTime >= a.getParticleValue().getAt() && currentTime < b.getParticleValue().getAt()) {
                        sendInterpolation(a, b);
                    }
                }
            }

//        Logger.console("Play particle for " + currentTime + "ms with " + t);
            lastUpdateTime = newTime;
        }
    }

    private void sendInterpolation(ParticleSplineNode a, ParticleSplineNode b) {
        double t = (double) (currentTime - a.getParticleValue().getAt()) / (double) (b.getParticleValue().getAt() - a.getParticleValue().getAt());
        Vector vector = SplineHelper.getValue(t,
                a.getSplineNode().getKnot().toVector(),
                a.getSplineNode().getOut().toVector(),
                b.getSplineNode().getIn().toVector(),
                b.getSplineNode().getKnot().toVector());

        double x = vector.getX();//InterpolationUtils.linearInterpolate(a.getParticleValue().getX(), b.getParticleValue().getX(), t);
        double y = vector.getY();//InterpolationUtils.linearInterpolate(a.getParticleValue().getY(), b.getParticleValue().getY(), t);
        double z = vector.getZ();//InterpolationUtils.linearInterpolate(a.getParticleValue().getZ(), b.getParticleValue().getZ(), t);

        double offsetX = InterpolationUtils.linearInterpolate(a.getParticleValue().getOffsetX(), b.getParticleValue().getOffsetX(), t);
        double offsetY = InterpolationUtils.linearInterpolate(a.getParticleValue().getOffsetY(), b.getParticleValue().getOffsetY(), t);
        double offsetZ = InterpolationUtils.linearInterpolate(a.getParticleValue().getOffsetZ(), b.getParticleValue().getOffsetZ(), t);

        double particleCount = InterpolationUtils.linearInterpolate(a.getParticleValue().getParticleCount(), b.getParticleValue().getParticleCount(), t);
        double particleData = InterpolationUtils.linearInterpolate(a.getParticleValue().getParticleData(), b.getParticleValue().getParticleData(), t);

//            Logger.console("ParticleCount for " + t + " = " + particleCount);
//            Logger.console("ParticleData for " + t + " = " + particleData);


        ParticleSpawner.INSTANCE.spawnParticle(areaTracker.getWorld(),
                particlePath.getParticleType(),
                (float) x,
                (float) y,
                (float) z,
                (int) particleCount,
                (float) offsetX,
                (float) offsetY,
                (float) offsetZ,
                particleData,
                a.getParticleValue().getParticleExtraData(),
                particlePath.getLongDistance(),
                null,
                areaTracker.getPlayers(),
                null
                );
    }

    private class ParticleSplineNode {
        private final ParticleValue particleValue;
        private final SplineNode splineNode;

        public ParticleSplineNode(ParticleValue particleValue, SplineNode splineNode) {
            this.particleValue = particleValue;
            this.splineNode = splineNode;
        }

        public ParticleValue getParticleValue() {
            return particleValue;
        }

        public SplineNode getSplineNode() {
            return splineNode;
        }
    }
}
