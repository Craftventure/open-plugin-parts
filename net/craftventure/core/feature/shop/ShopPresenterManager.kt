package net.craftventure.core.feature.shop

import net.craftventure.core.CraftventureCore
import net.craftventure.core.feature.shop.dto.ShopPresenterDto
import net.craftventure.core.ktx.json.CvMoshi
import net.craftventure.core.ktx.util.Logger
import java.io.File
import java.io.FileFilter

class ShopPresenterManager private constructor() {
    private val shops = mutableSetOf<ShopPresenter>()

    fun reload() {
        Logger.debug("Reloading shops")
        clearShops()
//        areaConfigList.clear()
        loadShops(File(CraftventureCore.getInstance().dataFolder, "data/shops"))
    }

    private fun clearShops() {
        shops.forEach { it.stop() }
        shops.clear()
    }

    private fun loadShops(directory: File) {
        val rootDirectories = directory.listFiles(FileFilter { it.isDirectory }) ?: return
        val adapter = CvMoshi.adapter(ShopPresenterDto::class.java)
        for (rootDirectory in rootDirectories) {
            val shopDirectories = rootDirectory.listFiles(FileFilter { it.isDirectory }) ?: continue
            shopDirectories.forEach { shopDirectory ->
                val shopConfigFile = File(shopDirectory, "shop.json")
                if (!shopConfigFile.exists() || !shopConfigFile.isFile) return@forEach

                if (shopConfigFile.isFile) {
                    try {
                        val dto = adapter.fromJson(shopConfigFile.readText())
                        if (dto != null) {
                            val shop = ShopPresenter("${rootDirectory.name}/${shopDirectory.name}", shopDirectory, dto)
                            shops.add(shop)
                            shop.start()
                        }
                    } catch (e: Exception) {
                        Logger.capture(e)
                        Logger.severe("ShopPresenterDto > Failed to load " + shopConfigFile.path, logToCrew = false)
                    }
                }
            }
        }
    }

    companion object {
        private val manager by lazy { ShopPresenterManager() }

        @JvmStatic
        fun getInstance() = manager
    }
}