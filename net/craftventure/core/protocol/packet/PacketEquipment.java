package net.craftventure.core.protocol.packet;

import com.comphenix.packetwrapper.WrapperPlayServerEntityEquipment;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import net.craftventure.core.ktx.util.Logger;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;


public class PacketEquipment extends AbstractPacket {
    private int entityId;
    private Slot slot;
    private ItemStack itemStack;

    public PacketEquipment(int entityId, Slot slot, ItemStack itemStack) {
        this.entityId = entityId;
        this.slot = slot;
        this.itemStack = itemStack;
    }

    public int getEntityId() {
        return entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    public Slot getSlot() {
        return slot;
    }

    public void setSlot(Slot slot) {
        this.slot = slot;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    public void sendPlayer(Player player) {
        WrapperPlayServerEntityEquipment wrapperPlayServerEntityEquipment = new WrapperPlayServerEntityEquipment();
        wrapperPlayServerEntityEquipment.setEntityID(entityId);
        wrapperPlayServerEntityEquipment.setItems(Collections.singletonList(new Pair(slot.itemSlot(), itemStack)));
        try {
            wrapperPlayServerEntityEquipment.sendPacket(player);
        } catch (Exception e) {
            Logger.capture(e);
        }
    }

    public enum Slot {
        MAIN_HAND(EnumWrappers.ItemSlot.MAINHAND),
        FEET(EnumWrappers.ItemSlot.FEET),
        OFFHAND(EnumWrappers.ItemSlot.OFFHAND),
        LEGS(EnumWrappers.ItemSlot.LEGS),
        CHEST(EnumWrappers.ItemSlot.CHEST),
        HEAD(EnumWrappers.ItemSlot.HEAD);

        private final EnumWrappers.ItemSlot itemSlot;

        Slot(EnumWrappers.ItemSlot itemSlot) {
            this.itemSlot = itemSlot;
        }

        public EnumWrappers.ItemSlot itemSlot() {
            return itemSlot;
        }
    }
}
