package net.craftventure.core.utils

import net.craftventure.bukkit.ktx.MaterialConfig.PLACE_HOLDER
import net.craftventure.bukkit.ktx.extension.setColor
import net.craftventure.bukkit.ktx.plugin.PluginProvider.isTestServer
import net.craftventure.bukkit.ktx.util.BukkitColorUtils
import net.craftventure.bukkit.ktx.util.ItemStackUtils2
import net.craftventure.core.ktx.util.Logger.severe
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.bukkit.extensions.itemStack
import net.craftventure.extension.decodeBase64ToByteArray
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import java.io.StringReader
import java.util.*

object ItemStackUtils {
    fun fromVanillaString(itemDetails: String): ItemStack? {
        try {
//            logcat { "Splitting $itemDetails" }
            val splitted = itemDetails.split("{", limit = 2)
            if (splitted.size == 1) {
//                logcat { "Parsing material=${splitted[0]}" }
                return ItemStack(Bukkit.getUnsafe().getMaterial(splitted[0], Bukkit.getUnsafe().dataVersion))
            } else if (splitted.size == 2) {
//                logcat { "Parsing material=${splitted[0]} nbt=${splitted[1]}" }
                val material = Material.getMaterial(splitted[0].uppercase()) ?: Material.getMaterial(
                    splitted[0].split(":").last().uppercase()
                )!!
                val item = ItemStack(material)
                return Bukkit.getUnsafe().modifyItemStack(item, "{" + splitted[1])
            } else {
//                logcat { "Splitted with size${splitted.size} is unsupported" }
                return null
            }
//            val item = ItemArgument().parse(com.mojang.brigadier.StringReader(itemDetails))
//            return item.createItemStack(1, true).asBukkitCopy()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    @JvmStatic
    @JvmOverloads
    fun fromString(str: String?, logYmlErrors: Boolean = true): ItemStack? {
        if (str.isNullOrBlank() || str == "null") return null
        if (str.equals("placeholder", ignoreCase = true)) {
            return PLACE_HOLDER.clone()
        }

        if ("{" in str) {
            val vanillaItem = fromVanillaString(str)
            if (vanillaItem != null) return vanillaItem
        }

        val itemstackData = MainRepositoryProvider.itemStackDataRepository.findCached(str.lowercase(Locale.ROOT))
        if (itemstackData != null) {
//            Logger.console("Found item " + new GsonBuilder().create().toJson(itemstackData));
            return itemstackData.itemStack
        }
        try {
            val random = Random()
            val datas = str.split(",").toTypedArray()
            val material = Material.valueOf(datas[0].uppercase())
            val itemStack = ItemStack(material)
            for (i in datas.indices) {
                val data = datas[i]
                if (data.startsWith("data:")) {
                    try {
                        val value = data.replace("data:", "").toInt()
                        val meta = itemStack.itemMeta
                        meta.setCustomModelData(value)
                        itemStack.setItemMeta(meta)
                    } catch (e: Exception) {
                    }
                } else if (data.startsWith("model:")) {
                    try {
                        val value = data.replace("model:", "").toInt()
                        val meta = itemStack.itemMeta
                        meta.setCustomModelData(value)
                        itemStack.setItemMeta(meta)
                    } catch (e: Exception) {
                    }
                } else if (data.startsWith("enchanted:")) {
                    val value = data.replace("enchanted:", "")
                    if (value == "1" || value == "true") ItemStackUtils2.addEnchantmentGlint(itemStack)
                } else if (data.startsWith("color:")) {
                    val color = data.replace("color:", "")
                    val parsedColor = BukkitColorUtils.parseColor(color)
                    itemStack.setColor(parsedColor)
                }
            }
            return itemStack
        } catch (e: Exception) {
//            e.printStackTrace();
        }
        try {
            return ItemStack.deserializeBytes(str.decodeBase64ToByteArray())
        } catch (e: Exception) {
//            e.printStackTrace();
        }
        try {
            StringReader(str).use { reader ->
                val yamlConfiguration = YamlConfiguration()
                try {
                    yamlConfiguration.load(reader)
                    if (yamlConfiguration.isItemStack("item")) {
                        val item = migrateDurabilityToCustomModelData(yamlConfiguration.getItemStack("item"))
//                    val base64 = item!!.serializeAsBytes().encodeAsBase64ToString()
//                    if (logYmlErrors)
//                        warn("Still using YAML config ($str) replace with $base64")
                        //                Logger.capture(new IllegalStateException("Still using YAML config, replace with " + ByteArrayExtensionsKt.encodeAsBase64ToString(item.serializeAsBytes()) + ""));
                        return item
                    }
                } catch (e: Exception) {

                }
            }
        } catch (e: Exception) {
//            e.printStackTrace();
        }

        severe("Failed to create item for $str")
//        if (isTestServer()) Thread.dumpStack()
        return PLACE_HOLDER
    }
}