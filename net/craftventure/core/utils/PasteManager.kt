package net.craftventure.core.utils

//import org.primesoft.asyncworldedit.api.worldedit.IThreadSafeEditSession
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitWorld
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.ForwardExtentCopy
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.session.ClipboardHolder
import net.craftventure.bukkit.ktx.plugin.PluginProvider
import net.craftventure.core.async.executeAsync
import net.craftventure.core.async.executeSync
import net.craftventure.core.ktx.util.Logger
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.util.Vector
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean


object PasteManager {
    private var DEBUG = false

    //    private val queue = ConcurrentLinkedQueue<PreparedRequest>()
    private var enabled = AtomicBoolean(false)

    fun updatePaste(request: PasteRequest) {
//        if (true) {
//            Logger.debug("Formats: ${ClipboardFormats.getAll().joinToString(",\n") {
//                "${it.name} ${it.aliases.joinToString(",")} ${it.fileExtensions.joinToString(",")}"
//            }}")
//            return
//        }
        if (!Bukkit.isPrimaryThread()) {
            executeSync { updatePaste(request) }
            return
        }

        try {
            val file = File(request.file.path + ".new.schem")
            if (file.exists()) return
            val loadFormat = ClipboardFormats.findByFile(request.file)
                ?: throw IllegalStateException("Failed to find schematics format for upgrading ${request.file}")
            Logger.debug("Loading ${request.file} of format $loadFormat")
            val clipboard = loadFormat.getReader(request.file.inputStream()).use { it.read() }

            val world = BukkitWorld(request.world)

            val atLocation = if (request.offsetIsRelative) {
                clipboard.origin.add(
                    request.offset.blockX,
                    request.offset.blockY,
                    request.offset.blockZ
                )
            } else {
                BlockVector3.at(
                    request.offset.blockX,
                    request.offset.blockY,
                    request.offset.blockZ
                )
            }

            WorldEdit.getInstance().editSessionFactory.getEditSession(world, -1).use { editSession ->
                val clipboardHolder = ClipboardHolder(clipboard)
                val paste = clipboardHolder
                    .createPaste(editSession)
                    .to(atLocation)
                    .ignoreAirBlocks(request.ignoreAirBlock)
                val operation = paste.build() as ForwardExtentCopy

                operation.isCopyingEntities = request.copyEntities
                operation.isRemovingEntities = request.removingEntities

                Logger.debug("Pasting ${operation} affecting ${operation.affected} with transform ${clipboardHolder.transform}...")
                Operations.complete(operation)
            }

            WorldEdit.getInstance().editSessionFactory.getEditSession(world, -1).use { editSession ->
                val newClipboard = BlockArrayClipboard(clipboard.region)
                newClipboard.origin = clipboard.origin
                val copy = ForwardExtentCopy(editSession, clipboard.region, atLocation, newClipboard, clipboard.origin)
                copy.isCopyingEntities = request.copyEntities
                copy.isRemovingEntities = request.removingEntities
                Operations.complete(copy)
                editSession.close()

                val saveFormat = BuiltInClipboardFormat.SPONGE_SCHEMATIC
//            val file = File(request.file.path + ".new.${saveFormat.fileExtensions.firstOrNull()}")
                file.createNewFile()
                saveFormat.getWriter(request.file.outputStream()).use { it.write(newClipboard) }
                Logger.debug(
                    "Schematic ${request.file} upgraded to $saveFormat affected ${copy.affected} region=${clipboard.region} ${clipboard.origin}"
                )
            }
        } catch (e: Exception) {
            Logger.capture(e)
        }
    }

    @JvmStatic
    fun init() {
        executeSync(2) {
            enabled.set(true)
        }
    }

    @JvmStatic
    fun queue(request: PasteRequest) {
//        Logger.debug("Past request: ${request.file}")
        if (!enabled.get()) {
//            Logger.debug("Not enabled, requeueing: ${request.file}")
            executeSync(5) { queue(request) }
            return
        }
//        if (CraftventureCore.isTestServer()) {
//            executeSync { loadIntoQueue(request) }
//            return
//        }
        if (PluginProvider.isOnMainThread()) {
//            Logger.debug("On main thread, requeueing: ${request.file}")
            executeAsync {
                queue(request)
            }
        } else {
//            Logger.debug("Executing: ${request.file}")
            loadIntoQueue(request)
        }
    }

    private fun loadIntoQueue(request: PasteRequest) {
        try {
//            updatePaste(request)
            val format = ClipboardFormats.findByFile(request.file)
                ?: throw IllegalStateException("Failed to find schematics format for ${request.file}")
            if (DEBUG)
                Logger.debug("Reading ${request.file}")
            val clipboard = request.file.inputStream().use { schematicStream ->
                format.getReader(schematicStream).read()
            }
//            Logger.debug("File loaded")
            val world = BukkitWorld(request.world)

            val editSession = WorldEdit.getInstance().editSessionFactory.getEditSession(world, -1)
            editSession.setFastMode(false)

            val atLocation = if (request.offsetIsRelative) {
                clipboard.origin.add(
                    request.offset.blockX,
                    request.offset.blockY,
                    request.offset.blockZ
                )
            } else {
                BlockVector3.at(
                    request.offset.blockX,
                    request.offset.blockY,
                    request.offset.blockZ
                )
            }

            if (DEBUG)
                Logger.debug("Pasting ${request.file}")

            val clipboardHolder = ClipboardHolder(clipboard)
            val paste = clipboardHolder
                .createPaste(editSession)
                .to(atLocation)
                .ignoreAirBlocks(request.ignoreAirBlock)
            val operation = paste.build()

            if (operation is ForwardExtentCopy) {
                operation.isCopyingEntities = request.copyEntities
                operation.isRemovingEntities = request.removingEntities
            }

//                val a = AsyncWorldEditMain.getInstance().api.operations.chunkOperations.createPaste(
//                        AsyncWorldEditMain.getInstance().api.playerManager.consolePlayer,
//                        Location(clipboard, atLocation.toVector3()),
//                        world,
//                        null,
//                        clipboardHolder,
//                        request.ignoreAirBlock,
//                        true,
//                        false
//                )
            val isThreadSafe = false
//                    Bukkit.getPluginManager().getPlugin("AsyncWorldEdit") != null && editSession is IThreadSafeEditSession
//                Logger.debug("isThreadSafe=$isThreadSafe main=${CraftventureCore.isOnMainThread()}")
            if (isThreadSafe) {
//                    Logger.debug("Executing async")
//                    executeSync {
                editSession.use { editSession ->
                    Operations.complete(operation)
//                        Logger.debug("Done ${request.file}")
                }
//                    }
            } else {
//                    Logger.debug("Executing sync ${request.file}")
                executeSync {
                    editSession.use { editSession ->
                        //                        Logger.debug(
//                            "Pasting to ${atLocation.x.format(2)} ${atLocation.y.format(2)} ${atLocation.z.format(
//                                2
//                            )}"
//                        )
                        Operations.complete(operation)
//                        Logger.debug("Done ${request.file}")
                    }
                }
            }
//                Logger.debug("Finished ${request.file} async=$isThreadSafe")
        } catch (e: Exception) {
            Logger.capture(e)
        }
    }

    class PasteRequest(
        val world: World,
        val file: File,
        val offset: Vector = Vector(),
        val offsetIsRelative: Boolean = true,
        val ignoreAirBlock: Boolean = false,
        val copyEntities: Boolean = false,
        val removingEntities: Boolean = false
    )
}