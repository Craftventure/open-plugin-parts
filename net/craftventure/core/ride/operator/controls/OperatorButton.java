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


public class OperatorButton extends OperatorControl {
    private Type type;
    private final Color colorOnFlashing;
    private Color colorOn;
    private Color colorOff;
    private boolean isFlashing = false;
    private final Map<String, Object> metadata = new HashMap<>();

    public OperatorButton(String id, Type type) {
        this(id, type, ControlColors.getGREEN(), ControlColors.getRED());
    }

    public OperatorButton(String id, Type type, Color colorOn, Color colorOff) {
        this(id, type, colorOn, colorOff, ControlColors.getGREEN_BRIGHT());
    }

    public OperatorButton(String id, Type type, Color colorOn, Color colorOff, Color colorOnFlashing) {
        super(id);
        this.type = type;
        this.colorOn = colorOn;
        this.colorOff = colorOff;
        this.colorOnFlashing = colorOnFlashing;
    }

    public Color getColorOn() {
        return colorOn;
    }

    public OperatorButton setColorOn(Color colorOn) {
        if (this.colorOn != colorOn) {
            this.colorOn = colorOn;
            invalidate();
        }
        return this;
    }

    public Color getColorOff() {
        return colorOff;
    }

    public OperatorButton setColorOff(Color colorOff) {
        if (this.colorOff != colorOff) {
            this.colorOff = colorOff;
            invalidate();
        }
        return this;
    }

    public OperatorButton setFlashing(boolean isFlashing) {
        if (this.isFlashing != isFlashing) {
            this.isFlashing = isFlashing;
            invalidate();
        }
        return this;
    }

    public boolean isFlashing() {
        return isFlashing;
    }

    @Override
    public String getKind() {
        return "button";
    }

    public OperatorButton setType(Type type) {
        if (this.type != type) {
            this.type = type;
            invalidate();
        }
        return this;
    }

    @Override
    public ItemStack representAsItemStack() {
        ItemStack itemStack;
        if (isEnabled()) {
            if (isFlashing)
                itemStack = MaterialConfig.INSTANCE.getGUI_BUTTON_BLINKING().clone();
            else
                itemStack = MaterialConfig.INSTANCE.getGUI_BUTTON_ON().clone();
        } else {
            itemStack = MaterialConfig.INSTANCE.getGUI_BUTTON_OFF().clone();
        }

        if (type == Type.E_STOP) {
            itemStack = MaterialConfig.INSTANCE.getGUI_ESTOP_OFF().clone();
//            itemStack.setType(Material.FLINT_AND_STEEL);
//            itemStack.setDurability((short) 0);
        } else if (type == Type.E_STOP_ACTIVATED) {
            itemStack = MaterialConfig.INSTANCE.getGUI_ESTOP_ON().clone();
//            itemStack.setType(Material.TNT);
//            itemStack.setDurability((short) 0);
        }

        ItemStackExtensionsKt.displayName(itemStack, getName());
        itemStack.lore(List.of(getDescription()));
        itemStack.setAmount(displayCount);
        ItemStackExtensionsKt.hideAttributes(itemStack);
        ItemStackExtensionsKt.hidePotionEffects(itemStack);
        ItemStackExtensionsKt.setColor(itemStack, isEnabled() ? isFlashing ? colorOnFlashing : colorOn : colorOff);
        return itemStack;
    }

    @Nullable
    @Override
    public Map<String, Object> getMetadata() {
        metadata.put("type", type.name().toLowerCase());
        metadata.put("flashing", isFlashing);
        metadata.put("color", ColorExtensionsKt.toHexColor((isEnabled()) ? colorOn.asRGB() : colorOff.asRGB()));
        return metadata;
    }

    public enum Type {
        DEFAULT,
        DISPATCH,
        E_STOP,
        E_STOP_ACTIVATED,
    }
}
