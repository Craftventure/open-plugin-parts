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


public class OperatorLed extends OperatorControl {
    private Color color;
    private boolean flashing = false;
    private final Map<String, Object> metadata = new HashMap<>();

    public OperatorLed(String id, Color color) {
        super(id);
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    public OperatorLed setColor(Color color) {
        if (this.color != color) {
            this.color = color;
            invalidate();
        }
        return this;
    }

    public OperatorLed setFlashing(boolean flashing) {
        if (this.flashing != flashing) {
            this.flashing = flashing;
            invalidate();
        }
        return this;
    }

    @Override
    public boolean isClickable() {
        return false;
    }

    @Override
    public String getKind() {
        return "led";
    }

    public boolean isFlashing() {
        return flashing;
    }

    @Override
    public ItemStack representAsItemStack() {
        ItemStack itemStack;
//        if (color == ControlColor.WHITE || color == ControlColor.RED) {
        if (isEnabled() && isFlashing()) {
            itemStack = MaterialConfig.INSTANCE.getGUI_BUTTON_BLINKING().clone();
        } else if (isEnabled()) {
            itemStack = MaterialConfig.INSTANCE.getGUI_BUTTON_ON().clone();
        } else {
            itemStack = MaterialConfig.INSTANCE.getGUI_BUTTON_OFF().clone();
        }
//        } else {
//            if (isEnabled() && isFlashing()) {
//                itemStack = MaterialConfig.INSTANCE.getGUI_GREEN_BLINKING();
//            } else if (isEnabled()) {
//                itemStack = MaterialConfig.INSTANCE.getGUI_GREEN_ON();
//            } else {
//                itemStack = MaterialConfig.INSTANCE.getGUI_GREEN_OFF();
//            }
//        }
//        ItemStack itemStack = new ItemStack(material, 1, color.getColorCode());
        ItemStackExtensionsKt.displayName(itemStack, getName());
        itemStack.lore(List.of(getDescription()));
        itemStack.setAmount(displayCount);
        ItemStackExtensionsKt.hideAttributes(itemStack);
        ItemStackExtensionsKt.hidePotionEffects(itemStack);
        ItemStackExtensionsKt.setColor(itemStack, color);
        return itemStack;
    }

    @Nullable
    @Override
    public Map<String, Object> getMetadata() {
        metadata.put("flashing", flashing);
        metadata.put("color", ColorExtensionsKt.toHexColor(color));
        return metadata;
    }
}
