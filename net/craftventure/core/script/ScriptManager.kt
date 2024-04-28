package net.craftventure.core.script

import net.craftventure.chat.bungee.extension.plus
import net.craftventure.chat.bungee.util.CVChatColor
import net.craftventure.core.CraftventureCore
import net.craftventure.core.api.CvApi.gsonActor
import net.craftventure.core.api.CvApi.gsonExposed
import net.craftventure.core.ktx.util.Logger.capture
import net.craftventure.core.ktx.util.Logger.severe
import net.craftventure.core.ktx.util.Logger.warn
import net.craftventure.core.npc.NpcEntity
import net.craftventure.core.npc.actor.ActorPlayback
import net.craftventure.core.npc.actor.RecordingData
import net.craftventure.core.script.armature.ArmatureScript
import net.craftventure.core.script.particle.ParticleMap
import net.craftventure.core.script.particle.ParticlePlayback
import net.craftventure.core.script.particle.ParticleScript
import net.craftventure.core.serverevent.ScriptLoadedEvent
import net.craftventure.core.serverevent.ScriptUnloadedEvent
import net.craftventure.core.utils.GsonUtils.read
import net.craftventure.database.MainRepositoryProvider
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.CommandSender
import java.io.File
import java.io.FileNotFoundException

object ScriptManager {
    private val scripts =
        HashMap<ScriptSettings, ScriptController>()
    private const val DESCRIPTOR_NAME = "settings.json"

    val controllers: Collection<ScriptController>
        get() = scripts.values

    @JvmStatic
    fun init() {
        loadAll()
    }

    fun loadAll() {
        synchronized(scripts) {
            val scriptRootDir =
                File(CraftventureCore.getInstance().dataFolder, "data/scripts/")
            val groupDirs =
                scriptRootDir.listFiles { obj: File -> obj.isDirectory }
            if (groupDirs != null) {
                for (groupDir in groupDirs) {
                    val scriptDirs =
                        groupDir.listFiles { obj: File -> obj.isDirectory }
                    if (scriptDirs != null) {
                        for (scriptDir in scriptDirs) {
//                            long start = System.currentTimeMillis();
                            try {
                                if (scriptDir.name != "schematics") load(
                                    groupDir.name,
                                    scriptDir.name
                                )
                            } catch (e: FileNotFoundException) {
                                capture(e)
                            }
                        }
                    }
                }
            }
        }
    }

    fun unloadAll() {
        synchronized(scripts) {
            while (scripts.size > 0) {
                val settings = scripts.keys.iterator().next()
                unload(
                    scripts[settings]
                )
            }
        }
    }

    fun unload(scriptController: ScriptController?) {
        synchronized(scripts) {
            try {
                scriptController!!.stop()
                scriptController.onUnload()
            } catch (e: Exception) {
                capture(e)
            }
            for (scriptName in scripts.keys) {
                val controller = scripts[scriptName]
                if (controller === scriptController) {
                    scripts.remove(scriptName)
                    return
                }
            }
        }
    }

    @JvmStatic
    fun destroy() {
        unloadAll()
    }

    fun reloadAll() {
        unloadAll()
        loadAll()
    }

    fun displayList(commandSender: CommandSender, group: String?) {
        val output = Component.text()
        for (script in scripts.keys) {
            if (group != null && script.groupId != group) {
                continue
            }
            val scriptController = scripts[script]
            if (output.children().isNotEmpty()) output.append(Component.text("  "))
            output.append((if (scriptController!!.isPlaying) NamedTextColor.GREEN else NamedTextColor.RED) + "${script.groupId}/${script.name}(${scriptController.scriptCount})")
        }
        commandSender.sendMessage(output.build())
    }

    val groups: Set<String>
        get() {
            val groups: MutableSet<String> = HashSet()
            for (script in scripts.keys) {
                if (groups.contains(script.groupId)) {
                    continue
                }
                groups.add(script.groupId)
            }
            return groups
        }

    fun getScriptsForGroup(group: String?): Set<String> {
        val scripts: MutableSet<String> = HashSet()
        for (script in ScriptManager.scripts.keys) {
            if (script.groupId == null) continue
            if (!script.groupId.equals(group, ignoreCase = true)) continue
            if (scripts.contains(script.name)) continue
            scripts.add(script.name)
        }
        return scripts
    }

    fun displayGroups(commandSender: CommandSender) {
        val output = StringBuilder()
        output.append(CVChatColor.serverNotice)
        val groups: MutableSet<String> = HashSet()
        for (script in scripts.keys) {
            if (groups.contains(script.groupId)) {
                continue
            }
            groups.add(script.groupId)
            if (output.length > CVChatColor.serverNotice.toString().length) output.append("  ")
            output.append(script.groupId)
        }
        commandSender.sendMessage(output.toString())
    }

    @JvmStatic
    fun restart(groupId: String, name: String): Boolean {
        stop(groupId, name)
        return start(groupId, name)
    }

    @JvmStatic
    fun start(groupId: String, name: String): Boolean {
        val scriptController =
            getScriptController(groupId, name)
        if (scriptController != null) {
            return scriptController.start()
        }
        warn(
            String.format(
                "Failed to start groupId=%1\$s name=%2\$s because it was not loaded",
                groupId,
                name
            )
        )
        return false
    }

    @JvmStatic
    fun stop(groupId: String, name: String): Boolean {
        val scriptController =
            getScriptController(groupId, name)
        if (scriptController != null) {
            return scriptController.stop()
        }
        warn(
            String.format(
                "Failed to stop groupId=%1\$s name=%2\$s because it was not loaded",
                groupId,
                name
            )
        )
        return false
    }

    private fun listFiles(directory: File): Array<File> {
        try {
            val files =
                directory.listFiles { obj: File -> obj.isFile }
            if (files != null) return files
        } catch (e: Exception) {
            capture(e)
        }
        return arrayOf()
    }

    @Throws(FileNotFoundException::class, IllegalStateException::class)
    fun load(groupId: String, name: String): ScriptController {
        synchronized(scripts) {
            val scriptFolder =
                File(CraftventureCore.getInstance().dataFolder, "data/scripts/$groupId/$name")
            val scriptSettingsFile =
                File(scriptFolder, DESCRIPTOR_NAME)
            return if (scriptSettingsFile.exists()) {
                val scriptSettings =
                    read(gsonExposed, scriptSettingsFile, ScriptSettings::class.java)
                check(scriptSettings.isValid) {
                    String.format(
                        "ScriptSettings are invalid for group=%1\$s name=%2\$s",
                        groupId,
                        name
                    )
                }
                val existingScriptSettings =
                    getScriptDescriptor(groupId, name)
                if (existingScriptSettings != null) {
                    val scriptController =
                        getScriptController(groupId, name)
                    if (scriptController != null) {
                        scriptController.stop()
                        scriptController.onUnload()
                        scripts.remove(existingScriptSettings)
                    }
                }
                val areaTracker = scriptSettings.createAreaTracker()
                scriptSettings.groupId = groupId
                scriptSettings.name = name
                val scriptController = ScriptController(areaTracker, groupId, name)

                // ==============================================================================

                for (scriptFile in listFiles(File(scriptFolder, "composed"))) {
                    if (scriptFile.name.endsWith(".json")) {
                        val composedScript = ComposedScript(scriptFile)
                        scriptController.addScript(composedScript)
                    }
                }

                // ==============================================================================

                var actorScript: ActorScript? = null
                for (scriptFile in listFiles(File(scriptFolder, "npc"))) {
                    if (!scriptFile.name.endsWith(".json")) {
                        continue
                    }
                    if (actorScript == null) actorScript = ActorScript()
                    //                    Logger.console(String.format("Loading npc %1$s for groupId=%2$s name=%3$s", scriptFile.getName(), groupId, name));
                    try {
                        val recordingData =
                            read(gsonActor, scriptFile, RecordingData::class.java)
                        if (recordingData.getPreferredType() == null) {
                            severe(
                                "Preferred type was null for %s (requested %s)",
                                false,
                                scriptFile.path,
                                recordingData.preferredTypeName
                            )
                            continue
                        }
//                        Logger.debug("Loading ${scriptFile}")
//                        recordingData.equipment?.updateItems()
//                        Logger.debug(recordingData.equipment.toJson())

                        var location =
                            recordingData.getFirstLocation(Bukkit.getWorld(scriptSettings.world))
                        if (location == null) {
                            severe("WARNING: failed to retrieve location from NPC! Using fallback location")
                            location =
                                Location(Bukkit.getWorld("world"), 298.28, 48.0, -573.92, -293f, 1f)
                        }
                        val npcEntity = NpcEntity(
                            scriptFile.nameWithoutExtension, recordingData.getPreferredType()!!, location,
                            if (recordingData.getGameProfile() != null) MainRepositoryProvider.cachedGameProfileRepository
                                .findCached(recordingData.getGameProfile()) else null,
                        )
                        if (recordingData.getEquipment() != null) {
                            recordingData.getEquipment().equip(npcEntity)
                        }
                        val actorPlayback = ActorPlayback(npcEntity, recordingData)
                        actorPlayback.setUpdateTick(recordingData.getUpdateTick() ?: scriptSettings.updateTick.toLong())
                        actorScript.add(actorPlayback)
                        actorPlayback.onAnimationUpdate()
                        areaTracker!!.addEntity(npcEntity)
                    } catch (e: Exception) {
                        capture(e)
                        severe("Failed to load script ${scriptFile.path}", true)
                    }
                }
                if (actorScript != null && actorScript.isValid) scriptController.addScript(actorScript)

                // ==============================================================================

                for (scriptFile in listFiles(File(scriptFolder, "particle"))) {
//                    Logger.info(String.format("Loading particle %1$s for groupId=%2$s name=%3$s", scriptFile.getName(), groupId, name));
                    try {
                        val particleScript = ParticleScript()
                        val particleMap =
                            read(gsonActor, scriptFile, ParticleMap::class.java)
                        if (particleMap != null) {
                            for (particlePath in particleMap.paths) {
                                if (particlePath.isValid(groupId, name)) {
                                    val particlePlayback =
                                        ParticlePlayback(
                                            particlePath, areaTracker, scriptSettings.updateTick
                                                .toLong()
                                        )
                                    particleScript.add(particlePlayback)
                                }
                            }
                        }
                        if (particleScript.isValid) scriptController.addScript(particleScript)
                        //
                    } catch (e: Exception) {
                        capture(e)
                        severe("Failed to load script ${scriptFile.path}", true)
                    }
                }

                // ==============================================================================

                for (scriptFile in listFiles(File(scriptFolder, "action"))) {
//                    Logger.console(String.format("Loading action %1$s for groupId=%2$s name=%3$s", scriptFile.getName(), groupId, name));
                    try {
                        val scriptActionScript = ScriptActionScript()
                        val scriptFrameMap =
                            read(gsonActor, scriptFile, ScriptFrameMap::class.java)
                        if (scriptFrameMap != null) {
                            for (scriptActionFrame in scriptFrameMap.frames) {
                                scriptActionFrame.action!!.setGroupIdAndName(groupId, name)
                                scriptActionFrame.action.validate()
                            }
                            val scriptActionPlayback =
                                ScriptActionPlayback(scriptFrameMap, areaTracker)
                            scriptActionScript.add(scriptActionPlayback)
                        }
                        if (scriptActionScript.isValid) scriptController.addScript(scriptActionScript)

//                        for (ScriptActionFrame frame : scriptFrameMap.getFrames()) {
//                            if (frame.getAction() instanceof PlaceSchematicAction) {
//                                frame.getAction().execute(null);
//                            }
//                        }
//
                    } catch (e: Exception) {
                        capture(e)
                        severe("Failed to load script ${scriptFile.path}", true)
                    }
                }

                // ==============================================================================

                listFiles(File(scriptFolder, "armature")).filter { it.name.endsWith(".json") }.forEach { scriptFile ->
//                    Logger.debug("Loading $scriptFile")
                    try {
                        val armatureScript = ArmatureScript(scriptFile, groupId, name)
                        scriptController.addScript(armatureScript)
                    } catch (e: Exception) {
                        capture(e)
                        severe("Failed to load script ${scriptFile.path}", true)
                    }
                }

                // ==============================================================================

                scriptController.init()
                if (scriptSettings.isAutoStart) {
                    scriptController.start()
                }
                scripts[scriptSettings] = scriptController
                scriptController
            } else {
                throw FileNotFoundException(
                    String.format(
                        "Script not found groupId=%1\$s name=%2\$s",
                        groupId,
                        name
                    )
                )
            }
        }
    }

    fun getScriptDescriptor(groupId: String, name: String): ScriptSettings? {
        synchronized(scripts) {
            for (scriptDescriptor in scripts.keys) {
                if (scriptDescriptor.groupId.equals(groupId, ignoreCase = true) &&
                    scriptDescriptor.name.equals(name, ignoreCase = true)
                ) {
                    return scriptDescriptor
                }
            }
            return null
        }
    }

    @JvmStatic
    fun getScriptController(groupId: String, name: String): ScriptController? {
        synchronized(scripts) {
            for (scriptDescriptor in scripts.keys) {
                if (scriptDescriptor.groupId.equals(groupId, ignoreCase = true) &&
                    scriptDescriptor.name.equals(name, ignoreCase = true)
                ) {
                    return scripts[scriptDescriptor]
                }
            }
            return null
        }
    }

    fun broadcastUnloaded(script: ScriptController) {
        Bukkit.getPluginManager().callEvent(ScriptUnloadedEvent(script, script.groupId, script.name))
    }

    fun broadcastLoad(script: ScriptController) {
        Bukkit.getPluginManager().callEvent(ScriptLoadedEvent(script, script.groupId, script.name))
    }
}