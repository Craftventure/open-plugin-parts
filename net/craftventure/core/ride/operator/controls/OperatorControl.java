package net.craftventure.core.ride.operator.controls;

import net.craftventure.audioserver.packet.PacketOperatorDefinition;
import net.craftventure.bukkit.ktx.extension.PlayerExtensionKt;
import net.craftventure.bukkit.ktx.util.SoundUtils;
import net.craftventure.bukkit.ktx.util.Translation;
import net.craftventure.core.CraftventureCore;
import net.craftventure.core.ride.operator.OperableRide;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;


public abstract class OperatorControl {
    private boolean invalidated = true;
    private ControlListener controlListener;
    private final String id;
    private Component name;
    private Component description;
    private boolean enabled = true;
    private long lastUpdate = System.currentTimeMillis();
    @Nullable
    private Object owner;
    private String permission;
    private int sort = 0;
    private String group = null;
    private String groupDisplayName = null;
    protected int displayCount = 1;

    public OperatorControl(String id) {
        this.id = id;
    }

    public boolean canUse(@Nullable Player player) {
        return player == null || permission == null || player.hasPermission(permission);
    }

    public int getSort() {
        return sort;
    }

    public OperatorControl setSort(int sort) {
        this.sort = sort;
        return this;
    }

    public String getGroup() {
        return group;
    }

    public OperatorControl setGroup(String group) {
        this.group = group;
        return this;
    }

    public String getGroupDisplayName() {
        return groupDisplayName;
    }

    public OperatorControl setGroupDisplayName(String groupDisplayName) {
        this.groupDisplayName = groupDisplayName;
        return this;
    }

    public OperatorControl setPermission(String permission) {
        this.permission = permission;
        return this;
    }

    public String getPermission() {
        return permission;
    }

    public OperatorControl setOwner(@Nullable Object owner) {
        this.owner = owner;
        return this;
    }

    @Nullable
    public Object getOwner() {
        return owner;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public abstract String getKind();

    public Component getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public int getDisplayCount() {
        return displayCount;
    }

    public OperatorControl setDisplayCount(int displayCount) {
        if (this.displayCount != displayCount) {
            this.displayCount = displayCount;
            invalidate();
        }
        return this;
    }

    public OperatorControl setName(TextColor color, String name) {
        return setName(Component.text(name, color));
    }

    public OperatorControl setName(Component name) {
        if (this.name == null || !this.name.equals(name)) {
            this.name = name;
            invalidate();
        }
        return this;
    }

    public Component getDescription() {
        return description;
    }

    public OperatorControl setDescription(TextColor color, String name) {
        return setDescription(Component.text(name, color));
    }

    public OperatorControl setDescription(Component description) {
        if (this.description == null || !this.description.equals(description)) {
            this.description = description;
            invalidate();
        }
        return this;
    }

    public boolean isClickable() {
        return true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public OperatorControl setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            invalidate();
        }
        return this;
    }

    public OperatorControl enable() {
        if (!this.enabled) {
            this.enabled = true;
            invalidate();
        }
        return this;
    }

    public OperatorControl disable() {
        if (this.enabled) {
            this.enabled = false;
            invalidate();
        }
        return this;
    }

    @Nullable
    public Map<String, Object> getMetadata() {
        return null;
    }

    public void invalidate() {
        invalidated = true;
        lastUpdate = System.currentTimeMillis();
    }

    public boolean isInvalidated() {
        return invalidated;
    }

    public void update() {
        invalidated = false;
    }

    public void setControlListener(ControlListener controlListener) {
        this.controlListener = controlListener;
    }

    public boolean click(OperableRide operableRide, @Nullable Player operator) {
        if (controlListener != null) {
            if (operator == null) {
                return click(operableRide, null, null);
            }
            for (int i = 0; i < operableRide.getTotalOperatorSpots(); i++) {
                Player player = operableRide.getOperatorForSlot(i);
                if (PlayerExtensionKt.isCrew(operator) || player == operator || (player == null && CraftventureCore.getOperatorManager().startOperating(operableRide, operator, i))) {
                    return click(operableRide, operator, i);
                }
            }
        }
        return false;
    }

    private boolean click(OperableRide operableRide, @Nullable Player operator, @Nullable Integer operatorSlot) {
        if (!canUse(operator)) {
            if (operator != null)
                operator.sendMessage(Translation.OPERATOR_CONTROL_NO_PERMISSION.getTranslation(operator));
            return false;
        }

        if (isEnabled() && isClickable()) {
            if (operator != null)
                operator.playSound(operator.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1, 1);
            if (controlListener != null)
                controlListener.onClick(operableRide, operator, this, operatorSlot);
            return true;
        } else if (!isEnabled()) {
            if (operator != null)
                operator.playSound(operator.getLocation(), SoundUtils.INSTANCE.getGUI_ERROR(), SoundCategory.MASTER, 1, 1);
        }
        return false;
    }

    public abstract ItemStack representAsItemStack();

    public interface ControlListener {
        void onClick(@NotNull OperableRide operableRide, @Nullable Player player, @NotNull OperatorControl operatorControl, @Nullable Integer operatorSlot);
    }

    public PacketOperatorDefinition.OperatorControlModel toModel(String rideId) {
        return new PacketOperatorDefinition.OperatorControlModel(
                getId(),
                PlainTextComponentSerializer.plainText().serialize(getName()),
                rideId,
                getKind(),
                isEnabled(),
                getMetadata(),
                getSort(),
                getGroup(),
                getGroupDisplayName()
        );
    }
}
