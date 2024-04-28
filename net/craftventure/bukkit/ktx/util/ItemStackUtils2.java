package net.craftventure.bukkit.ktx.util;

import kotlin.Deprecated;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class ItemStackUtils2 {
    public static ItemStack setSkin(ItemStack item, String nick) {
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwner(nick);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack setAmount(ItemStack item, int amount) {
        item.setAmount(amount);
        return item;
    }

    public static ItemStack createHead(String nick) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        return setSkin(item, nick);
    }

    public static ItemStack unbreakable(ItemStack itemStack) {
        if (itemStack == null)
            return null;
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setUnbreakable(true);
        itemMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public static ItemStack hideAttributes(ItemStack itemStack) {
        if (itemStack == null)
            return null;
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public static ItemStack hideEnchants(ItemStack itemStack) {
        if (itemStack == null)
            return null;
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    @Deprecated(message = "Use components")
    public static ItemStack create(Material mat, String name) {
        return setDisplayName(new ItemStack(mat), name);
    }

    @Deprecated(message = "Use components")
    public static ItemStack setDisplayName(ItemStack stack, String name) {
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        stack.setItemMeta(meta);
        return stack;
    }

    @Deprecated(message = "Use components")
    public static String getDisplayName(ItemStack stack) {
        if (stack != null) {
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                if (meta.getDisplayName() != null)
                    return meta.getDisplayName();
            }
        }
        return "";
    }

    public static ItemStack addEnchantmentGlint(ItemStack item) {
        item.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 1);
        return item;
    }
}
