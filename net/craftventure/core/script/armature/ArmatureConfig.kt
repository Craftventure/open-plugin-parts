package net.craftventure.core.script.armature

import com.squareup.moshi.JsonClass
import net.craftventure.core.utils.ItemStackUtils

@JsonClass(generateAdapter = true)
data class ArmatureConfig(
    val animation: String,
    val initial_models: List<ModelConfig>
) {
    @JsonClass(generateAdapter = true)
    data class ModelConfig(
        val joint: String,
        val model: String
    ) {
        val modelStack by lazy { ItemStackUtils.fromString(model) }

        val regex = joint.toRegex()
    }

    fun getModel(jointName: String) = initial_models.firstOrNull { it.regex.matches(jointName) }
}