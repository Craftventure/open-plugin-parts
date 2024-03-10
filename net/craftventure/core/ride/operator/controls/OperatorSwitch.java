package net.craftventure.core.ride.operator.controls;

import net.craftventure.bukkit.ktx.MaterialConfig;
import net.craftventure.bukkit.ktx.extension.ColorExtensionsKt;
import net.craftventure.bukkit.ktx.extension.ItemStackExtensionsKt;
import org.bukkit.Color;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class OperatorSwitch extends OperatorControl {
    private final Color onData;
    private final Color onDataEnabled;
    private final Color offData;
    private final Color offDataEnabled;

    private boolean on;
    private final Map<String, Object> metadata = new HashMap<>();
    private Type type = Type.OPEN_CLOSED;

    public OperatorSwitch(String id) {
        this(id, ControlColors.getGREEN(), ControlColors.getRED());
    }

    public OperatorSwitch(String id, Color onData, Color offData) {
        this(id, onData, offData, ControlColors.getGREEN_BRIGHT(), ControlColors.getRED_BRIGHT());
    }

    public OperatorSwitch(String id, Color onData, Color offData, Color onDataEnabled, Color offDataEnabled) {
        super(id);
        this.onData = onData;
        this.offData = offData;
        this.onDataEnabled = onDataEnabled;
        this.offDataEnabled = offDataEnabled;
    }

    public Color getOnData() {
        return onData;
    }

    public Color getOffData() {
        return offData;
    }

    public Color getOnDataEnabled() {
        return onDataEnabled;
    }

    public Color getOffDataEnabled() {
        return offDataEnabled;
    }

    public Type getType() {
        return type;
    }

    public OperatorSwitch setType(Type type) {
        if (this.type != type) {
            this.type = type;
            invalidate();
        }
        return this;
    }

    @Override
    public String getKind() {
        return "switch";
    }

    public boolean isOn() {
        return on;
    }

    public OperatorSwitch setOn(boolean on) {
        if (this.on != on) {
            this.on = on;
            invalidate();
        }
        return this;
    }

    /**
     * Toggle the switch if the control is enabled
     *
     * @return
     */
    public OperatorSwitch toggle() {
        if (isEnabled()) {
            this.on = !this.on;
            invalidate();
        }
        return this;
    }

    /**
     * Toggle the switch even if its disabled
     *
     * @return
     */
    public OperatorSwitch forceToggle() {
        this.on = !this.on;
        invalidate();
        return this;
    }

    @Override
    public ItemStack representAsItemStack() {
        ItemStack itemStack = ((isOn()) ?
                MaterialConfig.INSTANCE.getGUI_SWITCH_ON() :
                MaterialConfig.INSTANCE.getGUI_SWITCH_OFF()).clone();
//        ItemStack itemStack = !isEnabled() ? new ItemStack(Material.CONCRETE, 1, ControlColor.GRAY.getColorCode()) :
//                new ItemStack(isOn() ? onMaterial : offMaterial, 1, isOn() ? onData.getColorCode() : offData.getColorCode());
        ItemStackExtensionsKt.displayName(itemStack, getName());
        itemStack.lore(List.of(getDescription()));
        itemStack.setAmount(displayCount);
        ItemStackExtensionsKt.hideAttributes(itemStack);
        ItemStackExtensionsKt.hidePotionEffects(itemStack);
        ItemStackExtensionsKt.setColor(itemStack, isOn() ? isEnabled() ? onDataEnabled : onData : isEnabled() ? offDataEnabled : offData);
        return itemStack;
    }

    @Nullable
    @Override
    public Map<String, Object> getMetadata() {
        metadata.put("on", on);
        metadata.put("color_on", ColorExtensionsKt.toHexColor(isEnabled() ? onDataEnabled : onData));
        metadata.put("color_off", ColorExtensionsKt.toHexColor(isEnabled() ? offDataEnabled : offData));
        metadata.put("type", type.name().toLowerCase());
        return metadata;
    }

    public enum Type {
        ON_OFF, OPEN_CLOSED, LOCK_UNLOCK, FORWARDS_BACKWARDS
    }
}
