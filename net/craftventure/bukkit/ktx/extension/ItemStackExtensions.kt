package net.craftventure.bukkit.ktx.extension

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import net.md_5.bungee.api.chat.BaseComponent
import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.*
import org.bukkit.persistence.PersistentDataType

fun ItemStack.removeAllEnchantments(): ItemStack {
    for (enchantment in this.enchantments.keys) {
        removeEnchantment(enchantment)
    }
    return this
}

inline fun <reified T : ItemMeta> ItemStack.updateMeta(force: Boolean = true, action: T.() -> Unit): ItemStack {
    val hasMeta = force || hasItemMeta()
    if (hasMeta) {
        val meta = itemMeta as? T
        if (meta != null) {
            action(meta)
            itemMeta = meta
        }
    }
    return this
}

fun coloredItem(type: Material, color: Color, customModelData: Int? = null): ItemStack {
    val item = ItemStack(type)
    item.updateMeta<ItemMeta> { setCustomModelData(customModelData) }
    item.setColor(color)
    return item
}

fun ItemStack.getColor(): Color? {
    val meta = itemMeta
    return when (meta) {
        is FireworkEffectMeta -> meta.effect?.colors?.firstOrNull()
        is PotionMeta -> meta.color
        is LeatherArmorMeta -> meta.color
        else -> null
    }
}

fun ItemStack.setColor(color: Color): ItemStack {
    val meta = itemMeta
    when (meta) {
        is FireworkEffectMeta -> this.updateMeta<FireworkEffectMeta> { setColor(color) }
        is PotionMeta -> this.updateMeta<PotionMeta> { setColor(color) }
        is LeatherArmorMeta -> updateMeta<LeatherArmorMeta> { setColor(color) }
    }
    return this
}

fun ItemStack.takeIfNotAir() = takeIf { it.type != Material.AIR }

fun ItemStack.applyAllHideItemFlags(): ItemStack {
    ItemFlag.values().forEach {
        if (it.name.startsWith("HIDE_"))
            this.addItemFlags(it)
    }
    return this
}

fun ItemStack.unbreakable(): ItemStack {
    val itemMeta = itemMeta
    itemMeta?.isUnbreakable = true
    itemMeta?.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
    setItemMeta(itemMeta)
    return this
}

fun ItemStack.setDamage(damage: Int): ItemStack {
    val itemMeta = itemMeta
    if (itemMeta is Damageable) {
        itemMeta.damage = damage
        setItemMeta(itemMeta)
    }
    return this
}

fun ItemStack.setLeatherArmorColor(color: Color): ItemStack {
    if (this.itemMeta is LeatherArmorMeta) {
        val meta = this.itemMeta as LeatherArmorMeta
        meta.setColor(color)
        this.itemMeta = meta
    }
    return this
}

fun ItemStack.chargeColor(rgbColor: Int): ItemStack {
    if (this.itemMeta is FireworkEffectMeta) {
        val meta = this.itemMeta as FireworkEffectMeta
        val r = rgbColor shr 16 and 0xFF
        val g = rgbColor shr 8 and 0xFF
        val b = rgbColor shr 0 and 0xFF

        meta.effect = FireworkEffect.builder().withColor(Color.fromRGB(r, g, b)).build()
        this.itemMeta = meta
    }
    return this
}

fun ItemStack.clearMetaKey(key: NamespacedKey): ItemStack {
    updateMeta<ItemMeta> {
        persistentDataContainer.remove(key)
    }
    return this
}

fun <T : Any> ItemStack.setMeta(key: NamespacedKey, value: T, type: PersistentDataType<T, T>): ItemStack {
    updateMeta<ItemMeta> {
        persistentDataContainer.set(key, type, value)
    }
    return this
}

fun <T> ItemStack.getMeta(key: NamespacedKey, type: PersistentDataType<T, T>): T? =
    if (hasItemMeta()) itemMeta?.persistentDataContainer?.get(key, type)
    else null

@Deprecated(message = "Use builders")
fun ItemStack.displayName(components: Array<BaseComponent>): ItemStack {
    val meta = itemMeta
    meta?.setDisplayNameComponent(components)
    this.itemMeta = meta
    return this
}

fun ItemStack.displayName(component: Component): ItemStack {
    val meta = itemMeta
    meta?.displayName(
        Component.text().style(Style.style().decoration(TextDecoration.ITALIC, false).build()).append(component).build()
    )
    this.itemMeta = meta
    return this
}

@Deprecated(message = "Use builders")
fun ItemStack.displayName(displayName: String?): ItemStack {
    val meta = itemMeta
    meta?.setDisplayName(displayName)
    this.itemMeta = meta
    return this
}

fun ItemStack.hideAttributes(): ItemStack {
    val itemMeta = this.itemMeta
    itemMeta?.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
    this.itemMeta = itemMeta
    return this
}

fun ItemStack.hideEnchants(): ItemStack {
    val itemMeta = this.itemMeta
    itemMeta?.addItemFlags(ItemFlag.HIDE_ENCHANTS)
    this.itemMeta = itemMeta
    return this
}

fun ItemStack.hideDestroys(): ItemStack {
    val itemMeta = this.itemMeta
    itemMeta?.addItemFlags(ItemFlag.HIDE_DESTROYS)
    this.itemMeta = itemMeta
    return this
}

fun ItemStack.hidePlacedOn(): ItemStack {
    val itemMeta = this.itemMeta
    itemMeta?.addItemFlags(ItemFlag.HIDE_PLACED_ON)
    this.itemMeta = itemMeta
    return this
}

fun ItemStack.hideUnbreakable(): ItemStack {
    val itemMeta = this.itemMeta
    itemMeta?.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
    this.itemMeta = itemMeta
    return this
}

fun ItemStack.hidePotionEffects(): ItemStack {
    val itemMeta = this.itemMeta
    itemMeta?.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS)
    this.itemMeta = itemMeta
    return this
}