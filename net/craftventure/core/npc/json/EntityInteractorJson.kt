package net.craftventure.core.npc.json

import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import net.craftventure.core.ktx.json.MoshiBase
import net.craftventure.core.ktx.logging.logcat
import net.craftventure.core.npc.EntityMetadata
import net.minecraft.network.syncher.EntityDataSerializers

sealed class EntityInteractorJson<T : Any> {
    @Suppress("UNCHECKED_CAST")
    protected val interactor
        get() = registeredInteractors[type]!! as EntityMetadata.Interactor<T>
    lateinit var type: String

    fun applyTo(interactable: EntityMetadata.Interactable) {
        val interactor = this.interactor
        apply(interactable, interactor)
    }

    internal abstract fun apply(interactable: EntityMetadata.Interactable, interactor: EntityMetadata.Interactor<T>)

    companion object {
        private var hasInitialised = false

        private var polyMorphicAdapter = PolymorphicJsonAdapterFactory.of(EntityInteractorJson::class.java, "type")

        fun handle(interactor: EntityMetadata.InteractorProvider) {
            interactor.javaClass.declaredFields.forEach { field ->
                if (EntityMetadata.Interactor::class.java.isAssignableFrom(field.type)) {
                    try {
                        field.isAccessible = true
                        @Suppress("UNCHECKED_CAST")
                        val value = field.get(interactor.javaClass) as EntityMetadata.Interactor<Any>
                        val interactorName = "${interactor.internalName}/${value.name}"
                        val serializer = value.serializer

//                            println("Adding $interactorName of type ${dataClass.simpleName}")
                        when {
                            serializer === EntityDataSerializers.INT ->
                                polyMorphicAdapter = polyMorphicAdapter.withSubtype(
                                    JsonIntegerConverter::class.java,
                                    interactorName
                                )
                            serializer === EntityDataSerializers.BOOLEAN ->
                                polyMorphicAdapter = polyMorphicAdapter.withSubtype(
                                    JsonBooleanConverter::class.java,
                                    interactorName
                                )
                            serializer === EntityDataSerializers.BYTE ->
                                polyMorphicAdapter = polyMorphicAdapter.withSubtype(
                                    JsonByteConverter::class.java,
                                    interactorName
                                )
                            serializer === EntityDataSerializers.FLOAT ->
                                polyMorphicAdapter = polyMorphicAdapter.withSubtype(
                                    JsonFloatConverter::class.java,
                                    interactorName
                                )
                            serializer === EntityDataSerializers.COMPONENT ->
                                polyMorphicAdapter = polyMorphicAdapter.withSubtype(
                                    JsonChatComponentConverter::class.java,
                                    interactorName
                                )
                            serializer === EntityDataSerializers.OPTIONAL_COMPONENT ->
                                polyMorphicAdapter = polyMorphicAdapter.withSubtype(
                                    JsonOptionalChatComponentConverter::class.java,
                                    interactorName
                                )
                            serializer === EntityDataSerializers.ROTATIONS ->
                                polyMorphicAdapter = polyMorphicAdapter.withSubtype(
                                    JsonRotationsConverter::class.java,
                                    interactorName
                                )
                            serializer === EntityDataSerializers.ITEM_STACK ->
                                polyMorphicAdapter = polyMorphicAdapter.withSubtype(
                                    JsonItemStackConverter::class.java,
                                    interactorName
                                )
                            serializer === EntityDataSerializers.POSE ->
                                polyMorphicAdapter = polyMorphicAdapter.withSubtype(
                                    JsonPoseConverter::class.java,
                                    interactorName
                                )
                            serializer === EntityDataSerializers.VILLAGER_DATA ->
                                polyMorphicAdapter = polyMorphicAdapter.withSubtype(
                                    JsonVillagerDataConverter::class.java,
                                    interactorName
                                )
                            serializer === EntityDataSerializers.COMPOUND_TAG ->
                                polyMorphicAdapter = polyMorphicAdapter.withSubtype(
                                    JsonCompoundConverter::class.java,
                                    interactorName
                                )
                            else -> logcat(LogPriority.DEBUG) { "Type unsupported for JSON: ${serializer.javaClass.name} ($interactorName)" }
                        }

                        registeredInteractors[interactorName] = value
//                            println(" - ${value.entityTypeSpecificFlagOffset} (${value.absoluteIndex}) name=${value.name} type=${value.dataClazz.simpleName} optional=${value.optional}")
//                            println("   $interactorName/${value.name}")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        fun init() {
            if (hasInitialised) return

            MoshiBase.withBuilder().add(polyMorphicAdapter)

            hasInitialised = true
        }

        private val registeredInteractors = hashMapOf<String, EntityMetadata.Interactor<Any>>()
    }
}