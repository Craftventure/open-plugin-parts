package net.craftventure.core.animation.dae

import net.craftventure.core.animation.armature.Armature
import net.craftventure.core.animation.armature.Joint
import net.craftventure.core.animation.armature.JointKeyFrame
import net.craftventure.core.ktx.extension.childElements
import net.craftventure.core.ktx.extension.toList
import net.craftventure.core.ktx.util.Logger
import net.craftventure.core.ktx.util.Logger.tree
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory

// Heavily inspired/based on https://youtu.be/z0jb1OBw45I

object DaeLoader {
    fun load(document: Document, errorName: String): Array<Armature> {
        val collada = document.documentElement.apply { normalize() }

        if (collada.getAttribute("version") != "1.4.1")
            throw IllegalStateException("Collada version not supported")

        val scenes = collada.getElementsByTagName("library_visual_scenes").toList()
            .flatMap { it.childElements.toList() }
            .filter {
                //                Logger.debug("Scene tag: ${it.tagName}")
                it.tagName == "visual_scene"
            }

        if (collada.getElementsByTagName("library_geometries").length > 0 || collada.getElementsByTagName("library_materials").length > 0) {
            Logger.warn("Armature $errorName has geometry and/or materials. Export only the armatures", true)
        }

        val animations = collada.getElementsByTagName("library_animations").toList()
            .flatMap { it.childElements.toList() }
            .filter { it.tagName == "animation" }

//        Logger.debug("Scenes: ${scenes.size}")

        val armatureRoots = scenes.flatMap {
            // Root nodes
            it.childElements.toList().filter {
                it.childElements.toList()
                    .filter { it.tagName == "node" }
                    .filter {
                        //                        Logger.debug("Node type: ${it.getAttribute("type")}")
                        it.getAttribute("type") == "JOINT"
                    }
                    .isNotEmpty()
            }
        }

//        Logger.debug("Roots: ${armatureRoots.size}/${armatureRoots.map { it.getAttribute("id") }}")

        val armatures = armatureRoots.map { armatureElement ->
            val id = armatureElement.getAttribute("id")
            val name = armatureElement.getAttribute("name")
            val matrixElement =
                armatureElement.childElements.firstOrNull {
                    it.tagName == "matrix" && it.getAttribute("sid") == "transform"
                }
            val matrix = (if (matrixElement != null) parseMatrix(matrixElement) else null) ?: Matrix4x4()
//            Logger.debug("Matrix: $matrix")

            val joints =
                armatureElement.childElements.filter {
                    it.tagName == "node" && it.getAttribute("type") == "JOINT"
                }.mapNotNull { parseJoint(it) }

            val allJoints = joints.flatMap { it.childrenRecursive() + it }
//            Logger.debug("Total of ${allJoints.size} joints")
            allJoints.forEach { joint ->
                val animationNode = findAnimationNode(animations, joint.id)
                if (animationNode != null) {
//                    Logger.debug("Parsing animation for ${joint.id}")
                    val timeSource = getSourceForType(animationNode, "TIME")
                    val transformSource = getSourceForType(animationNode, "TRANSFORM")
                    val interpolationSource = getSourceForType(animationNode, "INTERPOLATION")

                    if (timeSource != null && transformSource != null && interpolationSource != null) {
                        val keyframes =
                            timeSource.getElementsByTagName("float_array").item(0).textContent.split(" ")
                                .map { it.toDouble() }
                        val transforms =
                            transformSource.getElementsByTagName("float_array").item(0).textContent.split(" ")
                                .map { it.toDouble() }.let { it.chunked(16).map { matrixFromDoubles(it) } }
                        val interpolations =
                            interpolationSource.getElementsByTagName("Name_array").item(0).textContent.split(" ")

                        val keyframeCount = keyframes.size
                        val jointKeyframes = (0 until keyframeCount).map {
                            JointKeyFrame(interpolations[it], transforms[it], keyframes[it])
                        }
                        joint.animation = jointKeyframes.sortedBy { it.time }.toTypedArray()
//                        Logger.debug("Parsed animation for ${joint.id} with root ")
//                        Logger.debug("Keyframes: $keyframes")
//                        Logger.debug("Matrices: ${transforms.size} / ${transforms.joinToString(", ")}")
//                        Logger.debug("Interpolations: $interpolations")
                    }
                }
            }

//            Logger.debug("Joints for $id/$name: ${joints.joinToString("\n")}} with matrix=$matrix")

            Armature(id, name, joints.toMutableList(), matrix)
        }

        return armatures.toTypedArray()
    }

    private fun getSourceForType(animation: Element, type: String): Element? {
        animation.childElements.forEach {
            val techniqueCommon = it.getElementsByTagName("technique_common").item(0) as? Element ?: return@forEach
            val accessor = techniqueCommon.getElementsByTagName("accessor").item(0) as? Element ?: return@forEach
            val param = accessor.getElementsByTagName("param").item(0) as? Element ?: return@forEach
            if (param.getAttribute("name") == type) {
                return it
            }

        }
        return null
    }

    private fun findAnimationNode(animationsRoots: List<Element>, id: String): Element? {
        animationsRoots.forEach {
            it.childElements.filter { it.tagName == "animation" }.forEach {
                val channel = it.getElementsByTagName("channel").toList().firstOrNull() as? Element
                if (channel?.getAttribute("target")?.startsWith("$id/transform") == true) {
                    return it
                }
            }
        }
        return null
    }

    private fun parseJoint(element: Element): Joint? {
        val id = element.getAttribute("id")
        val name = element.getAttribute("name")
        val matrixElement =
            element.childElements.firstOrNull {
                it.tagName == "matrix" && it.getAttribute("sid") == "transform"
            }
        val matrix = (if (matrixElement != null) parseMatrix(matrixElement) else null) ?: Matrix4x4()
//        Logger.debug("Matrix for joint $id/$name: $matrix")

        val joints =
            element.childElements
                .filter {
                    it.tagName == "node" && it.getAttribute("type") == "JOINT"
                }.mapNotNull { parseJoint(it) }
        return Joint(id, name, matrix, joints.toMutableList())
    }

    private fun parseMatrix(element: Element): Matrix4x4? {
        return element.textContent?.split(" ")?.map { it.toDouble() }?.let {
            matrixFromDoubles(it)
        }
    }

    private fun matrixFromDoubles(doubles: List<Double>): Matrix4x4 {
        if (doubles.size != 16) throw IllegalStateException("Input consisted ${doubles.size} instead of 16")
        return Matrix4x4(
            doubles[0],
            doubles[1],
            doubles[2],
            doubles[3],
            doubles[4],
            doubles[5],
            doubles[6],
            doubles[7],
            doubles[8],
            doubles[9],
            doubles[10],
            doubles[11],
            doubles[12],
            doubles[13],
            doubles[14],
            doubles[15]
        )
    }
}

fun main() {
    tree = object : Logger.Tree {
        private val locale by lazy { Locale.US }//Locale("nl", "NL") }
        override fun captureException(throwable: Throwable) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun doLog(tag: String, level: Logger.Level, log: String, logToCrew: Boolean, vararg params: Any?) {
            val message = "[$tag] " + prepareMessage(log, *params)
            println(message)
        }

        private fun prepareMessage(log: String, vararg params: Any? = emptyArray()): String {
            return try {
                if (params.isNotEmpty())
                    log.format(locale = locale, args = *params)
                else
                    log
            } catch (e: Exception) {
//            e.printStackTrace()
                log
            }
        }

    }

    val fXmlFile = File("C:\\Users\\joey_\\Desktop\\export\\2bones.dae")
    val dbFactory = DocumentBuilderFactory.newInstance()
    val dBuilder = dbFactory.newDocumentBuilder()
    val doc = dBuilder.parse(fXmlFile)

    val armatures = DaeLoader.load(doc, "test")
    Logger.debug("Parsed armatures ${armatures.joinToString(", ") { it.id }}")

    val matrix = Matrix4x4()
    matrix.rotate(
        Quaternion(
            -0.7071068286895752,
            0.0,
            0.0,
            0.7071068286895752
        )
    )
    matrix.translate(0.0, 1.0, 0.0)
    Logger.debug(matrix.toString())
//    Logger.debug(armatures.joinToString("\n\n\n"))
}