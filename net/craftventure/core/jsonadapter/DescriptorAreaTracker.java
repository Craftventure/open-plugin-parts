package net.craftventure.core.jsonadapter;

import com.google.gson.annotations.Expose;
import net.craftventure.bukkit.ktx.area.SimpleArea;
import net.craftventure.core.npc.tracker.NpcAreaTracker;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;


public class DescriptorAreaTracker {
    @Expose
    private double xMin;
    @Expose
    private double xMax;
    @Expose
    private double yMin;
    @Expose
    private double yMax;
    @Expose
    private double zMin;
    @Expose
    private double zMax;

    public DescriptorAreaTracker(double xMin, double xMax, double yMin, double yMax, double zMin, double zMax) {
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
        this.zMin = zMin;
        this.zMax = zMax;
    }

    public double getxMin() {
        return xMin;
    }

    public double getxMax() {
        return xMax;
    }

    public double getyMin() {
        return yMin;
    }

    public double getyMax() {
        return yMax;
    }

    public double getzMin() {
        return zMin;
    }

    public double getzMax() {
        return zMax;
    }

    public NpcAreaTracker getAreaTracker(String worldName) {
        World world = Bukkit.getWorld(worldName);
//        Logger.console("Creating tracker of " + xMin + ">" + yMin + ">" + zMin + " <><> " + xMax + ">" + yMax + ">" + zMax + " in " + worldName);
        return new NpcAreaTracker(new SimpleArea(new Location(world, xMin, yMin, zMin), new Location(world, xMax, yMax, zMax)));
    }
}
