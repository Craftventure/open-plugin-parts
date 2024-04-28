package net.craftventure.core.script.particle;

import com.google.gson.annotations.Expose;
import net.craftventure.bukkit.ktx.extension.ColorExtensionsKt;
import net.craftventure.core.ktx.util.Logger;
import net.craftventure.core.utils.ItemStackUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;


public class ParticleValue {
    @Expose
    private long at;
    @Expose
    private double tension;
    @Expose
    private double x;
    @Expose
    private double y;
    @Expose
    private double z;
    @Expose
    private double offsetX;
    @Expose
    private double offsetY;
    @Expose
    private double offsetZ;
    @Expose
    private double particleData;
    @Expose
    private int particleCount;
    @Expose
    @Nullable
    private String blockData;
    @Expose
    @Nullable
    private String itemData;

    @Expose
    @Nullable
    private String dustColor;
    @Expose
    @Nullable
    private Float dustSize;

    @Nullable
    private Particle.DustOptions cachedDustOptions;
    @Nullable
    private BlockData cachedBlockData;
    @Nullable
    private ItemStack cachedItemStack;

    @Nullable
    public Object getParticleExtraData() {
        Particle.DustOptions dustOptions = getDustOptions();
        if (dustOptions != null) return dustOptions;
        ItemStack itemStack = getItemStack();
        if (itemStack != null) return itemStack;
        BlockData data = getBlockData();
        return data;
    }

    @Nullable
    public Particle.DustOptions getDustOptions() {
        if (cachedDustOptions == null && this.dustColor != null && this.dustSize != null) {
            try {
                Color color = ColorExtensionsKt.colorFromHex(this.dustColor);
                cachedDustOptions = new Particle.DustOptions(color, this.dustSize);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return cachedDustOptions;
    }

    @Nullable
    public ItemStack getItemStack() {
        if (cachedItemStack == null && itemData != null) {
            try {
                cachedItemStack = ItemStackUtils.fromString(itemData);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                Logger.debug("Illegal itemdata %s: %s", true, itemData, e.getMessage());
            }
        }
        return cachedItemStack;
    }

    @Nullable
    public BlockData getBlockData() {
        if (cachedBlockData == null && blockData != null) {
            try {
                try {
                    cachedBlockData = Bukkit.getServer().createBlockData(blockData);
                } catch (IllegalArgumentException e) {
//                    e.printStackTrace();
                }
                cachedBlockData = Material.valueOf(blockData.toUpperCase()).createBlockData();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                Logger.debug("Illegal blockdata %s: %s", true, blockData, e.getMessage());
            }
        }
        return cachedBlockData;
    }

    public boolean isValid() {
        return true;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public double getOffsetZ() {
        return offsetZ;
    }

    public double getParticleData() {
        return particleData;
    }

    public int getParticleCount() {
        return particleCount;
    }

    public long getAt() {
        return at;
    }

    public double getTension() {
        return tension;
    }
}
