package net.craftventure.core.map.renderer

import net.craftventure.bukkit.ktx.extension.getHighestBlock
import net.craftventure.bukkit.ktx.extension.getMaterialColorRgb
import net.craftventure.core.async.executeSync
import net.craftventure.core.ktx.util.ColorUtils
import net.craftventure.core.map.CoordTranslator
import org.bukkit.Bukkit
import org.bukkit.Material
import java.awt.image.BufferedImage


class WorldMapRendererTask(
    private val worldCordTranslator: CoordTranslator.WorldCoordTranslator,
    private val minY: Int,
    private val maxY: Int,
    private val alpha: Double,
    private val finishListener: (WorldMapRendererTask) -> Unit
) : MapTask<MapEntryRenderer> {
    private var running: Boolean = false
    override fun isRunning(): Boolean = running

    var image: BufferedImage? = null

    override fun start(renderer: MapEntryRenderer) {
        if (running) return
        executeSync {
            val world = Bukkit.getWorld("world")!!
            if (image == null) {
                image = BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB_PRE)
            }
            for (x in 0 until 128) {
                for (y in 0 until 128) {
                    val worldX = worldCordTranslator.getWorldX(x).toInt()
                    val worldZ = worldCordTranslator.getWorldZ(y).toInt()

                    val block = world.getHighestBlock(worldX, worldZ, minY, maxY) { block ->
                        block.type != Material.AIR
                    }
//                    Logger.debug("x=${worldX.format(2)} y=${block?.location?.y?.format(2)} z=${worldZ.format(2)} type=${block?.type}")
                    if (block != null) {
                        val color = block.getMaterialColorRgb() ?: return@executeSync
                        val red = ColorUtils.red(color)
                        val green = ColorUtils.green(color)
                        val blue = ColorUtils.blue(color)
                        val preMultipliedColor =
                            ColorUtils.rgb((red * alpha).toInt(), (green * alpha).toInt(), (blue * alpha).toInt())
//                        val premultipliedColor = (((color shr 16 and 0xFF) * 0.5).toInt() shl 16) or
//                                (((color shr 8 and 0xFF) * 0.5).toInt() shl 8) or
//                                (((color and 0xFF) * 0.5).toInt())
//                        val premultipliedColor = (color and 0x00ffffff) or (0x80 shl 24)
//                            Logger.debug("Color at $x/$y ${color.toString(16)}")
                        image?.setRGB(x, y, preMultipliedColor)
                    }
                }
            }
//            ImageIO.write(image!!, "png", File(CraftventureCore.getInstance().dataFolder, "map.png"))
            finishListener(this)
        }
    }
}