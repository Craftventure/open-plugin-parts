package net.craftventure.jsontools

import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File
import java.util.*

class BuilderProcessor(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger,
    val options: Map<String, String>,
) : SymbolProcessor {
    val root = "https://jsonschema.craftventure.net/model"
    private val json = Json { prettyPrint = true }

    private val outputDirectory = options["jsontools.output"]!!.let { File(it) }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(JsonClass::class.qualifiedName!!)
        val ret = symbols.filter { !it.validate() }.toList()
        symbols
            .filter { it is KSClassDeclaration && it.validate() }
            .filter { it.annotations.none { it.annotationType.resolve().declaration.qualifiedName?.asString() == Transient::class.qualifiedName } }
            .forEach { it.accept(BuilderVisitor(), Unit) }
        return emptyList()
    }

    private fun typeToJson(typeReference: KSTypeReference): JsonObject {
        val resolvedType = typeReference.resolve()
        val typeDeclaration = resolvedType.declaration
        val typeClassName = typeDeclaration.qualifiedName?.asString()
        val isJsonType =
            typeDeclaration.annotations.any { it.annotationType.resolve().declaration.qualifiedName?.asString() == JsonClass::class.qualifiedName }
        val isNullable = resolvedType.isMarkedNullable

        val polymorphicHint =
            typeDeclaration.annotations.firstOrNull { it.annotationType.resolve().declaration.qualifiedName?.asString() == PolymorphicHint::class.qualifiedName }

//        logger.warn(
//            "type=${resolvedType} ${typeDeclaration.closestClassDeclaration()?.classKind}"
//        )

//        val jsonName =
//            typeDeclaration.annotations.filter { it.annotationType.resolve().declaration.qualifiedName?.asString() == Json::class.qualifiedName }
//                .firstNotNullOfOrNull { ksAnnotation ->
//                    val nameArgument = ksAnnotation.arguments.first { it.name?.asString() == "name" }
//                    nameArgument.value as? String
//                }

        val jsonProperty = buildJsonObject {
//            property.docString?.let {
//                put("description", it.trim())
//            }
            if (isJsonType) {
                put("\$ref", "${root}/${typeClassName}.json")
            } else {
                when {
                    polymorphicHint != null -> {
                        put("description", "Polymorphic type not properly supported yet: ${typeClassName}")
//                        put("anyOf", buildJsonArray {
//                            buildJsonObject {
//                                put(
//                                    "\$ref",
//                                    "https://jsonschema.craftventure.net/model/net.craftventure.core.ride.tracklessride.config.CarGroupConfig.json"
//                                )
//                            }
//                            buildJsonObject {
//                                put(
//                                    "\$ref",
//                                    "https://jsonschema.craftventure.net/model/net.craftventure.core.ride.tracklessride.config.CarGroupConfig.json"
//                                )
//                            }
//                        })
//                        throw IllegalStateException("Unsupported polymorphic type $typeClassName")
                    }
                    typeClassName == Boolean::class.qualifiedName -> {
                        put("type", buildJsonArray {
                            add("boolean")
                            if (isNullable)
                                add("null")
                        })
                    }
                    typeClassName == Int::class.qualifiedName -> {
                        put("type", buildJsonArray {
                            add("integer")
                            if (isNullable)
                                add("null")
                        })
                    }
                    typeClassName == Float::class.qualifiedName -> {
                        put("type", buildJsonArray {
                            add("number")
                            if (isNullable)
                                add("null")
                        })
                    }
                    typeClassName == Double::class.qualifiedName -> {
                        put("type", buildJsonArray {
                            add("number")
                            if (isNullable)
                                add("null")
                        })
                    }
                    typeClassName == String::class.qualifiedName -> {
                        put("type", buildJsonArray {
                            add("string")
                            if (isNullable)
                                add("null")
                        })
                    }
                    typeClassName == Optional::class.qualifiedName -> {
                        val listType = typeReference.element!!.typeArguments[0]
                        val json = typeToJson(listType.type!!)
                        val jsonType = json["type"]
//                        val nullable = listType.type!!.resolve().isMarkedNullable
                        if (jsonType != null)
                            put("type", jsonType)
                        else
                            put("description", "Unsupported type $listType")
                    }
                    typeClassName == Set::class.qualifiedName -> {
                        put("type", buildJsonArray {
                            add("array")
                            if (isNullable)
                                add("null")
                        })
                        put("uniqueItems", true)
                        val listType = typeReference.element!!.typeArguments[0]
                        val json = typeToJson(listType.type!!)
                        put("items", buildJsonArray {
                            add(json)
                        })
                    }
                    typeClassName == Map::class.qualifiedName -> {
                        put("type", buildJsonArray {
                            add("object")
                            if (isNullable)
                                add("null")
                        })
                        put("additionalProperties", typeToJson(typeReference.element!!.typeArguments[1].type!!))
                    }
                    typeDeclaration.closestClassDeclaration()?.classKind == ClassKind.ENUM_CLASS -> {
                        val values = typeDeclaration.closestClassDeclaration()!!.declarations
                            .filter { it.closestClassDeclaration()?.classKind == ClassKind.ENUM_ENTRY }
                            .toList()
                        val valueNames = values.map { property ->
                            val jsonName =
                                property.annotations.filter { it.annotationType.resolve().declaration.qualifiedName?.asString() == Json::class.qualifiedName }
                                    .firstNotNullOfOrNull { ksAnnotation ->
                                        val nameArgument =
                                            ksAnnotation.arguments.first { it.name?.asString() == "name" }
                                        nameArgument.value as? String
                                    }
                            jsonName ?: property.simpleName.asString()
                        }
//                        valueNames.forEach { value ->
//                            logger.warn("  - Value $value")
//                        }
                        put("enum", buildJsonArray {
                            valueNames.forEach { add(it) }
                        })
                    }
                    typeClassName == List::class.qualifiedName -> {
                        put("type", buildJsonArray {
                            add("array")
                            if (isNullable)
                                add("null")
                        })
                        if (typeReference.element != null) {
                            val listType = typeReference.element!!.typeArguments[0]
                            val json = typeToJson(listType.type!!)
                            put("items", buildJsonArray {
                                add(json)
                            })
                        }

//                        throw IllegalStateException("failed $argumentType isJsonType=$isJsonType")
                    }
                    typeClassName == Array::class.qualifiedName -> {
                        put("type", buildJsonArray {
                            add("array")
                            if (isNullable)
                                add("null")
                        })
                        val listType = typeReference.element!!.typeArguments[0]
                        val json = typeToJson(listType.type!!)
                        put("items", buildJsonArray {
                            add(json)
                        })

//                        throw IllegalStateException("failed $argumentType isJsonType=$isJsonType")
                    }
                    else -> {
                        put("description", "Type unknown: ${typeClassName}")
//                        throw IllegalStateException("Unsupported type $typeClassName")
                    }
                }
            }
        }
        return jsonProperty
    }

    inner class BuilderVisitor : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
//            logger.warn("Class ${classDeclaration.packageName.asString()}.${classDeclaration.simpleName.asString()}")
            classDeclaration.primaryConstructor!!.accept(this, data)

            val requiredProperties = hashSetOf<String>()

            val properties = buildJsonObject {
                put("\$schema", buildJsonObject {
                    put("type", "string")
                })

                classDeclaration.declarations.filterIsInstance<KSPropertyDeclaration>().forEach { property ->
                    val json = typeToJson(property.type)

                    if (property.annotations.firstOrNull { it.annotationType.resolve().declaration.qualifiedName?.asString() == Transient::class.qualifiedName } != null) {
                        return@forEach
                    }
                    val jsonName =
                        property.annotations.filter { it.annotationType.resolve().declaration.qualifiedName?.asString() == Json::class.qualifiedName }
                            .firstNotNullOfOrNull { ksAnnotation ->
                                val nameArgument = ksAnnotation.arguments.first { it.name?.asString() == "name" }
                                nameArgument.value as? String
                            }

                    val name = jsonName ?: property.simpleName.asString()

                    val hasDefault = classDeclaration.getConstructors()
                        .any { it.parameters.any { it.name?.asString() == property.simpleName.asString() && it.hasDefault } }
                    if (!hasDefault && !property.type.resolve().isMarkedNullable)
                        requiredProperties += name

                    put(name, json)
                }
            }


            val schema = buildJsonObject {
                put("\$schema", "http://json-schema.org/draft-07/schema")
                put("\$id", classDeclaration.qualifiedName!!.asString())
                put("type", "object")
                put("title", classDeclaration.simpleName.asString())
                put("properties", properties)
                putJsonArray("required") {
                    requiredProperties.forEach(this::add)
                }
            }
//            logger.warn("Schema ${schema}")
            val file = File(outputDirectory, "${classDeclaration.qualifiedName!!.asString()}.json")
            file.writeText(json.encodeToString(schema))
        }
    }

}

class BuilderProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment
    ): SymbolProcessor {
        return BuilderProcessor(environment.codeGenerator, environment.logger, environment.options)
    }
}