package net.craftventure.core.script.api;

import net.craftventure.core.CraftventureCore;
import net.craftventure.core.npc.NpcEntity;
import net.craftventure.core.npc.tracker.NpcEntityTracker;
import net.craftventure.core.script.fountain.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;


public class FountainsApi {
    private List<Fountain> fountains = new ArrayList<>();
    public Location center;

    public List<Fountain> getFountains() {
        return fountains;
    }

    protected void cleanup() {
        for (int i = 0; i < fountains.size(); i++) {
            Fountain fountain = fountains.get(i);
            fountain.cleanup();
        }
    }

    public void resetFountains() {
        for (int i = 0; i < fountains.size(); i++) {
            Fountain fountain = fountains.get(i);
            fountain.reset();
        }
    }

    public void onUpdate(double showSeconds) {
        for (int i = 0; i < fountains.size(); i++) {
            Fountain fountain = fountains.get(i);
            fountain.update(showSeconds);
        }
    }

    public void shoot(final Location location, final int blockCount, final double pressure, final NpcEntityTracker npcEntityTracker, final int lifeTimeTicks) {
        shoot(location, blockCount, pressure, npcEntityTracker, lifeTimeTicks, Material.WATER.createBlockData());
    }

    public void shoot(final Location location, final int blockCount, final double pressure, final NpcEntityTracker npcEntityTracker, final int lifeTimeTicks, BlockData blockData) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(CraftventureCore.getInstance(), new Runnable() {
            public Location loc = location;

            @Override
            public void run() {
                loc = loc.clone();
                Vector vector = new Vector(0, 0, 0);
                for (int i = 0; i < blockCount; i++) {
                    final NpcEntity npcEntity = new NpcEntity(EntityType.FALLING_BLOCK, loc)
                            .setBlockData(blockData);
                    npcEntityTracker.addEntity(npcEntity);
                    vector.setY(pressure);
                    npcEntity.velocity(vector);
                    loc.setY(loc.getY() + 1);

                    Bukkit.getScheduler().scheduleSyncDelayedTask(CraftventureCore.getInstance(), () -> npcEntityTracker.removeEntity(npcEntity), lifeTimeTicks);
                }
            }
        });
    }

    public OarsmanJet createOarsmanJet(Location loc, double heading, double pitch, double pressure, final NpcEntityTracker entityTracker, int lifeTimeTicks) {
        OarsmanJet oarsmanJet = new OarsmanJet(loc, heading, pitch, pressure, entityTracker, lifeTimeTicks);
        fountains.add(oarsmanJet);
        return oarsmanJet;
    }

    public ContinuousShooter createContinuousShooter(Location loc, double pressure, final NpcEntityTracker entityTracker, int lifeTimeTicks) {
        ContinuousShooter continuousShooter = new ContinuousShooter(loc, pressure, entityTracker, lifeTimeTicks);
        fountains.add(continuousShooter);
        return continuousShooter;
    }

    public BloomJet createBloomJet(Location loc, int rayAmount, double pitch, double pressure, final NpcEntityTracker entityTracker, int lifeTimeTicks) {
        BloomJet bloomJet = new BloomJet(loc, rayAmount, pitch, pressure, entityTracker, lifeTimeTicks);
        fountains.add(bloomJet);
        return bloomJet;
    }

    public LillyJet createLillyJet(Location loc, int rayAmount, double pitch, double pressure, final NpcEntityTracker entityTracker, int lifeTimeTicks) {
        LillyJet lillyJet = new LillyJet(loc, rayAmount, pitch, pressure, entityTracker, lifeTimeTicks);
        fountains.add(lillyJet);
        return lillyJet;
    }

    public void compileFountains() {
        for (Fountain fountain : fountains) {
            fountain.compile();
        }
    }
}
