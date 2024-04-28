package net.craftventure.core.npc.json

import com.squareup.moshi.JsonClass
import net.craftventure.core.npc.EntityMetadata
import net.craftventure.withFlag

@JsonClass(generateAdapter = true)
data class JsonByteConverter(
    val value: Byte = 0,
    val flags: Set<String>? = null,
    val any: List<Set<String>>? = null,
) : EntityInteractorJson<Byte>() {
    @delegate:Transient
    private val resolvedFlags by lazy { flags?.map { interactor.resolveFlag(it) } }

    @delegate:Transient
    private val resolvedAnyFlags by lazy { any?.map { it.map { interactor.resolveFlag(it) } } }

    override fun apply(interactable: EntityMetadata.Interactable, interactor: EntityMetadata.Interactor<Byte>) {
        val flags =
            if (resolvedAnyFlags != null && resolvedAnyFlags!!.isNotEmpty()) resolvedAnyFlags!!.random() else resolvedFlags

        interactable.applyInteractor(interactor, flags?.let { flags ->
            var flag: Byte = 0
            flags.forEach { flag = flag withFlag it!! }
            flag
        } ?: value)
    }
}