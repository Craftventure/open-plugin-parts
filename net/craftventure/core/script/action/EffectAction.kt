package net.craftventure.core.script.action

import com.google.gson.annotations.Expose
import com.squareup.moshi.JsonClass
import net.craftventure.core.effect.EffectManager
import net.craftventure.core.npc.tracker.NpcEntityTracker
import net.craftventure.core.script.ScriptManager


@JsonClass(generateAdapter = true)
data class EffectAction(
    @Expose val type: Type,
    @Expose val subtype: Subtype,
    @Expose val group: String?,
    @Expose val name: String?
) : ScriptAction() {
    override fun validate(): Boolean {
        return true
    }

    override fun execute(entityTracker: NpcEntityTracker?) {
//        Logger.info("Executing action effect with $type $subtype $group $name")
        when (type) {
            Type.SCRIPT -> {
                if (group != null && name != null) {
                    when (subtype) {
                        Subtype.START -> ScriptManager.start(group, name)
                        Subtype.STOP -> ScriptManager.stop(group, name)
                    }
                }
            }
            Type.SPECIAL_EFFECT -> {
                if (name != null) {
                    when (subtype) {
                        Subtype.START -> EffectManager.findByName(name)?.play()
                        Subtype.STOP -> EffectManager.findByName(name)?.stop()
                    }
                }
            }
        }
    }

    override val actionTypeId: Int
        get() = ScriptAction.Type.EFFECT

    enum class Type {
        SCRIPT, SPECIAL_EFFECT
    }

    enum class Subtype {
        START, STOP
    }
}
