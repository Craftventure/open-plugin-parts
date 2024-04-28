package net.craftventure.core.animation;

import net.craftventure.core.CraftventureCore;
import net.craftventure.core.npc.NpcEntity;
import net.craftventure.core.npc.tracker.NpcEntityTracker;
import net.craftventure.core.utils.InterpolationUtils;
import net.craftventure.core.utils.SimpleInterpolator;
import net.craftventure.core.utils.VectorUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;


public class SquareJurassicDoor implements Door {
    private static final double deg360 = Math.toRadians(360);
    private static final double deg45 = Math.toRadians(45);
    private static final double deg135 = Math.toRadians(135);
    private static final double deg225 = Math.toRadians(225);
    private static final double deg315 = Math.toRadians(315);
    private final World world;
    private final JurassicLocation[] sourceLocations;
    private final boolean compensateAngle = true;
    private NpcEntity[] fallingBlocks;
    private final NpcEntityTracker npcEntityTracker;
    private final SimpleInterpolator simpleInterpolator;
    private BukkitRunnable runnable;

    public SquareJurassicDoor(World world, JurassicLocation[] sourceLocations, SimpleInterpolator simpleInterpolator, NpcEntityTracker npcEntityTracker) {
        this.world = world;
        this.sourceLocations = sourceLocations;
        this.simpleInterpolator = simpleInterpolator;
        this.npcEntityTracker = npcEntityTracker;
    }

    private void toLocation(Vector source, Location dest) {
        dest.setX(source.getX());
        dest.setY(source.getY());
        dest.setZ(source.getZ());
    }

    @Override
    public void open(final int ticks) {
        if (runnable != null) {
            runnable.cancel();
            runnable = null;
        }

        npcEntityTracker.startTracking();
//        Logger.console("Open");
//        animate(ticks, true);
        final Vector calculateVector = new Vector(0, 0, 0);
        final Location location = new Location(world, 0, 0, 0);
//        if (fallingBlocks == null) {
        createBlocks();
//        }
        runnable = new BukkitRunnable() {
            int currentTick = 0;

            @Override
            public void run() {
                currentTick++;
                double percentage = simpleInterpolator.interpolate(currentTick, 0, 1, ticks);
//                Logger.console("%" + percentage + " Y= " + sourceLocations[0].getY() + ", delta" + (moveHeight * percentage));
//                Logger.console("Y" + (sourceLocations[0].getY() + (moveHeight * percentage)));
                if (currentTick > ticks) {
                    for (JurassicLocation jurassicLocation : sourceLocations) {
                        jurassicLocation.getLocation(calculateVector, false, compensateAngle);
                        toLocation(calculateVector, location);
                        Location newLocation = location.clone();
                        newLocation.getBlock().setBlockData(jurassicLocation.blockData);
                    }
                    npcEntityTracker.stopTracking();
                    cancel();
                } else {
                    for (int i = 0; i < fallingBlocks.length; i++) {
                        double angle = InterpolationUtils.linearInterpolate(sourceLocations[i].startAngleRadian, sourceLocations[i].endAngleRadian, percentage);
                        sourceLocations[i].getLocation(calculateVector, angle, true);
                        fallingBlocks[i].move(calculateVector.getX(), calculateVector.getY(), calculateVector.getZ());
                    }
                }

                if (currentTick == 1) {
                    for (JurassicLocation jurassicLocation : sourceLocations) {
                        jurassicLocation.getLocation(calculateVector, true, compensateAngle);
                        toLocation(calculateVector, location);
                        location.getBlock().setType(Material.AIR);
                    }
                }
            }
        };
        runnable.runTaskTimer(CraftventureCore.getInstance(), 1L, 1L);
    }

    @Override
    public void close(final int ticks) {
        if (runnable != null) {
            runnable.cancel();
            runnable = null;
        }

        npcEntityTracker.startTracking();
//        Logger.console("Open");
//        animate(ticks, true);
        final Vector calculateVector = new Vector(0, 0, 0);
        final Location location = new Location(world, 0, 0, 0);
//        if (fallingBlocks == null) {
        createBlocks();
//        }
        runnable = new BukkitRunnable() {
            int currentTick = 0;

            @Override
            public void run() {
                currentTick++;
                double percentage = 1 - simpleInterpolator.interpolate(currentTick, 0, 1, ticks);
//                Logger.console("%" + percentage + " Y= " + sourceLocations[0].getY() + ", delta" + (moveHeight * percentage));
//                Logger.console("Y" + (sourceLocations[0].getY() + (moveHeight * percentage)));
                if (currentTick > ticks) {
                    for (JurassicLocation jurassicLocation : sourceLocations) {
                        jurassicLocation.getLocation(calculateVector, true, compensateAngle);
                        toLocation(calculateVector, location);
                        Location newLocation = location.clone();
                        newLocation.getBlock().setBlockData(jurassicLocation.blockData);
                    }
                    npcEntityTracker.stopTracking();
                    cancel();
                } else {
                    for (int i = 0; i < fallingBlocks.length; i++) {
                        double angle = InterpolationUtils.linearInterpolate(sourceLocations[i].startAngleRadian, sourceLocations[i].endAngleRadian, percentage);
                        sourceLocations[i].getLocation(calculateVector, angle, true);
                        fallingBlocks[i].move(calculateVector.getX(), calculateVector.getY(), calculateVector.getZ());
                    }
                }

                if (currentTick == 1) {
                    for (JurassicLocation jurassicLocation : sourceLocations) {
                        jurassicLocation.getLocation(calculateVector, false, compensateAngle);
                        toLocation(calculateVector, location);
                        location.getBlock().setType(Material.AIR);
                    }
                }
            }
        };
        runnable.runTaskTimer(CraftventureCore.getInstance(), 1L, 1L);
    }

    private void createBlocks() {
        if (fallingBlocks == null || fallingBlocks.length == 0) {
            fallingBlocks = new NpcEntity[sourceLocations.length];
            final Vector calculateVector = new Vector(0, 0, 0);
            final Location location = new Location(world, 0, 0, 0);
            for (int i = 0; i < fallingBlocks.length; i++) {
                sourceLocations[i].getLocation(calculateVector, true, compensateAngle);
                toLocation(calculateVector, location);
                fallingBlocks[i] = new NpcEntity("jurassicDoor", EntityType.FALLING_BLOCK, location);
                fallingBlocks[i].noGravity(true);
                fallingBlocks[i].setBlockData(sourceLocations[i].blockData);
                npcEntityTracker.addEntity(fallingBlocks[i]);
            }
        }
    }

    public static class JurassicLocation {
        private final Vector pivot;
        private final double offset;
        private final double startAngleRadian;
        private final double endAngleRadian;
        private final BlockData blockData;

        public JurassicLocation(Vector pivot, double offset, double startAngle, double endAngle, BlockData blockData) {
            this.pivot = pivot;
            this.offset = offset;
            this.startAngleRadian = Math.toRadians(startAngle);
            this.endAngleRadian = Math.toRadians(endAngle);
            this.blockData = blockData;
        }

        public void getLocation(Vector dest, double angleRadian, boolean compensateAngle) {
//            double deg90 = 1.57079633;
//            double deg180 = 1.57079633*2;
            double offset = this.offset;
            if (compensateAngle) {
                double absAngle = Math.abs(angleRadian) % deg360;
                boolean cos = absAngle < deg45 || (absAngle > deg135 && absAngle < deg225) || absAngle > deg315;
                offset *= Math.abs((0.5 / (cos ? Math.cos(angleRadian) : Math.sin(angleRadian)))) * 2;
                offset = Math.min(this.offset * 2, offset);
                offset = Math.max(-this.offset * 2, offset);
            }
            dest.setX(offset);
            dest.setY(0);
            dest.setZ(0);
            VectorUtils.rotateAroundAxisY(dest, angleRadian);
            dest.setX(pivot.getX() + dest.getX());
            dest.setY(pivot.getY() + dest.getY());
            dest.setZ(pivot.getZ() + dest.getZ());
        }

        public void getLocation(Vector dest, boolean currentlyClosed, boolean compensateAngle) {
            getLocation(dest, currentlyClosed ? startAngleRadian : endAngleRadian, compensateAngle);
        }
    }
}
