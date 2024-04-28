package net.craftventure.core.ktx.extension

import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList

fun NodeList.forEach(action: (Node) -> Unit): Unit {
    for (i in 0 until length) {
        action(item(i))
    }
}

fun NodeList.toList(): List<Node> {
    val items = mutableListOf<Node>()
    forEach {
        items += it
    }
    return items
}

val Node.childElements: List<Element>
    get() = childNodes.toList().filterIsInstance(Element::class.java)