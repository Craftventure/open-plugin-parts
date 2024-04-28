package net.craftventure.core.script.action

import com.google.gson.annotations.Expose
import com.squareup.moshi.JsonClass
import net.craftventure.bukkit.ktx.manager.FeatureManager
import net.craftventure.core.CraftventureCore
import net.craftventure.core.async.AsyncTask
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.ktx.util.Logger.severe
import net.craftventure.core.npc.tracker.NpcEntityTracker
import net.craftventure.core.utils.PasteManager.PasteRequest
import net.craftventure.core.utils.PasteManager.queue
import org.bukkit.Bukkit
import org.bukkit.util.Vector
import java.io.File

@JsonClass(generateAdapter = true)
data class PlaceSchematicAction(
    @Expose var noAir: Boolean = true,
    @Expose var fastMode: Boolean = false,
    @Expose var entities: Boolean = false,
    @Expose var offsetIsAbsolute: Boolean = false,
    @Expose var world: String = "world",
    @Expose var offsetX: Int = 0,
    @Expose var offsetY: Int = 0,
    @Expose var offsetZ: Int = 0,
    @Expose var name: String? = null,
) : ScriptAction() {

    constructor(groupId: String?, sceneName: String?) : this() {
        this.groupId = groupId
        this.sceneName = sceneName
    }

//    init {
//        executeSync(5) { paste() }
//    }

    override fun validate(): Boolean {
        return true
    }

    fun withName(name: String): PlaceSchematicAction {
        this.name = name
        return this
    }

    fun fastMode(fastMode: Boolean): PlaceSchematicAction {
        this.fastMode = fastMode
        return this
    }

    fun noAir(noAir: Boolean): PlaceSchematicAction {
        this.noAir = noAir
        return this
    }

    //    public PlaceSchematicAction entities(boolean entities) {
//        this.entities = entities;
//        return this;
//    }
    override fun execute(entityTracker: NpcEntityTracker?) {
        if (!FeatureManager.isFeatureEnabled(FeatureManager.Feature.SCENE_ACTION_SCHEMATIC_PASTING)) return
        object : AsyncTask() {
            override fun doInBackground() {
                try {
                    paste()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.executeNow()
    }

    @Throws(Exception::class)
    fun paste() {
        if (!FeatureManager.isFeatureEnabled(FeatureManager.Feature.AUTOMATED_SCHEMATIC_PASTING)) return
        var schematicFile = File(
            CraftventureCore.getInstance().dataFolder,
            "data/scripts/$groupId/$sceneName/schematics/$name.schem"
        )
        if (!schematicFile.exists()) {
            schematicFile = File(
                CraftventureCore.getInstance().dataFolder,
                "data/scripts/$groupId/$sceneName/schematics/$name.schematic"
            )
        }
        if (!schematicFile.exists()) {
            schematicFile = File(
                CraftventureCore.getInstance().dataFolder,
                "data/scripts/$groupId/schematics/$name.schem"
            )
        }
        if (!schematicFile.exists()) {
            schematicFile = File(
                CraftventureCore.getInstance().dataFolder,
                "data/scripts/$groupId/schematics/$name.schematic"
            )
        }
        if (schematicFile.exists()) {
            queue(
                PasteRequest(
                    Bukkit.getWorld("world")!!,
                    schematicFile,
                    Vector(
                        offsetX,
                        offsetY,
                        offsetZ
                    ),
                    !offsetIsAbsolute,
                    noAir,
                    entities,
                    false
                )
            )
        } else {
            severe("Tried to paste '$name' but did not exists as schematic in /plugins/WorldEdit/schematics/$name.schematic at ${Logger.miniTrace()}")
        }
    }

    override val actionTypeId: Int
        get() = Type.PLACE_SCHEMATIC
}