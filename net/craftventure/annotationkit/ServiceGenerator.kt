package net.craftventure.annotationkit

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import java.io.OutputStream
import java.util.*
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class GenerateService(
    val serviceClass: KClass<*> = Unit::class,
)

internal class ServiceGenerator(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger,
    val options: Map<String, String>,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {

        val serviceClasses =
            resolver.getSymbolsWithAnnotation(GenerateService::class.qualifiedName!!)
                .filterIsInstance<KSClassDeclaration>()
                .filter(KSNode::validate)

        val classFileMap = serviceClasses
            .map { it.getServiceName() }
            .toSet()
            .map { className ->
                logger.info("Creating $className")
                className to codeGenerator.createNewFile(
                    Dependencies(false, *resolver.getAllFiles().toList().toTypedArray()),
                    "META-INF/services",
                    className,
                    extensionName = ""
                )
            }.associate { it }

        resolver.getSymbolsWithAnnotation(GenerateService::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .filter(KSNode::validate)
            .forEach { classDec ->
                val className = classDec.getServiceName()

                val file = classFileMap[className]!!
                file.write("${classDec.getReferenceName()}\n".toByteArray())
                classDec.accept(Visitor(file), Unit)
            }

        classFileMap.values.forEach { it.close() }

        return emptyList()
    }

    private fun KSClassDeclaration.getReferenceName(): String {
        val packageName = packageName.asString()
        val qualifiedName = qualifiedName!!.asString()
        val className = qualifiedName.replace(packageName, "").removePrefix(".")
        return packageName + "." + className.replace(".", "$")
    }

    private fun KSClassDeclaration.getServiceName(): String {
        val annotation = annotations.first {
            it.shortName.getShortName() == GenerateService::class.simpleName && it.annotationType.resolve().declaration
                .qualifiedName?.asString() == GenerateService::class.qualifiedName
        }
        val name = (annotation.arguments.first().value as KSType).declaration.qualifiedName!!.asString()
        return if (name == Unit::class.qualifiedName) {
            val superTypes = getAllSuperTypes()
//            if (superType.count() != 1) throw IllegalStateException("For ${qualifiedName?.asString()}, multiple supertypes were give. Specify the serviceclass manually in the annotation")
            return superTypes.first().declaration.qualifiedName?.asString()
                ?: throw IllegalStateException("For ${qualifiedName?.asString()}, no valid service classes were given")
        } else name
    }

    inner class Visitor(private val file: OutputStream) : KSVisitorVoid()
}

internal class ServiceGeneratorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment
    ): SymbolProcessor {
        return ServiceGenerator(environment.codeGenerator, environment.logger, environment.options)
    }
}