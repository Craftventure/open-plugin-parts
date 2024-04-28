package net.craftventure.bukkit.ktx.util

import net.craftventure.bukkit.ktx.extension.updateMeta
import net.craftventure.chat.bungee.util.CVTextColor
import net.craftventure.chat.bungee.util.parseWithCvMessage
import net.craftventure.core.ktx.logging.logcat
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

open class ComponentBuilder<T> private constructor(
    val defaultColor: TextColor? = null,
    val maxWidth: Int? = null,
    val supportsNewLines: Boolean = true,
    val defaultStyle: Style? = null,
) {
    private val components = mutableListOf<Part>()

    protected fun components(): List<Part> = components

    fun startWithLines(components: List<Component>) {
        components.forEach {
            component(it)
            moveToBlankLine()
        }
    }

    fun addPart(part: Part): ComponentBuilder<T> {
        components += part
        return this
    }

    fun component(component: Component): ComponentBuilder<T> {
        components += ComponentPart(component)
        return this
    }

    @JvmOverloads
    fun text(
        text: String,
        color: TextColor? = defaultColor,
        addSpace: Boolean = true,
        tag: String? = null,
        usePreviousColor: Boolean = false,
        endWithBlankLine: Boolean = false,
        builder: (Component.() -> Component)? = null
    ): ComponentBuilder<T> {
        components += TextPart(text, color, addSpace, tag, usePreviousColor, builder)
        if (endWithBlankLine) moveToBlankLine()
        return this
    }

    fun moveToBlankLine(): ComponentBuilder<T> {
        components += MoveToBlankLine
        return this
    }

    fun emptyLines(count: Int = 1): ComponentBuilder<T> {
        components += EmptyLinePart(count)
        return this
    }

    sealed class Part {
        open fun addsContent(): Boolean = true
    }

    class TextPart(
        val text: String,
        val color: TextColor?,
        val addSpace: Boolean,
        val tag: String?,
        val usePreviousColor: Boolean,
        val builder: (Component.() -> Component)?
    ) : Part() {
        val parts = text.split(" ")
    }

    object MoveToBlankLine : Part() {
        override fun addsContent(): Boolean = false
    }

    class EmptyLinePart(val count: Int) : Part() {
        init {
            assert(count > 0)
        }


        override fun addsContent(): Boolean = false
    }

    class ComponentPart(val component: Component) : Part()

    fun buildLineComponents(applyDefaultStyle: Boolean = true): List<Component> {
        val items = mutableListOf<Component>()
        val lineSizes = mutableListOf<Int>()

        var currentLineComponent: Component? = null
        var currentLineText = ""
        var lastColor = defaultColor

        fun appendComponentToCurrentLine(text: String, component: Component) {
            if (currentLineComponent == null)
                currentLineComponent = if (applyDefaultStyle && defaultStyle != null)
                    Component.text().style(defaultStyle).append(component).build()
                else component
            else
                currentLineComponent = currentLineComponent!!.append(component)
            currentLineText += text
        }

        fun tryFinishCurrentLine(force: Boolean = false): Boolean {
//            if (debug)
//            logcat { "buildCurrentLine(): $currentLineText" }

            if (!force && (currentLineText.isEmpty() || currentLineComponent == null)) return false

            items.add(currentLineComponent!!)
            lineSizes.add(currentLineText.length)

            currentLineComponent = null
            currentLineText = ""
            return true
        }

        val components = this.components.toMutableList()
        while (components.lastOrNull()?.addsContent() == false) {
            components.removeLastOrNull()
        }
        while (components.firstOrNull()?.addsContent() == false) {
            components.removeFirstOrNull()
        }
        components.forEach { part ->
            if (debug)
                logcat { "[[ Part ${part.javaClass.simpleName} ]]" }
            when (part) {
                is EmptyLinePart -> {
                    if (!supportsNewLines) return@forEach

                    if (currentLineText.isNotEmpty()) {
                        if (debug)
                            logcat { "[EmptyLinePart] Building line (flushing current queue)" }
                        tryFinishCurrentLine(true)
                    }
                    while (!lineSizes.takeLast(part.count).all { it == 0 }) {
                        if (debug)
                            logcat { "[EmptyLinePart] Building line (count not matched)" }
                        appendComponentToCurrentLine("", Component.empty())
                        tryFinishCurrentLine(true)
                    }
                }

                MoveToBlankLine -> {
                    if (!supportsNewLines) return@forEach

                    if (currentLineText.isNotEmpty()) {
                        if (debug)
                            logcat { "[RequireNewLine] Building line (current one not empty)" }
                        tryFinishCurrentLine(true)
                    }
                }

                is TextPart -> {
                    var container = ""
                    if (currentLineText.isNotEmpty() && part.addSpace)
                        container += " "

                    if (debug)
                        logcat { "[TextPart] Starting with [$container] space=${part.addSpace}" }

                    fun appendContainerToLine(): Boolean {
                        if (container.isEmpty()) return false

                        val sourceComponent =
                            container.parseWithCvMessage().color(if (part.usePreviousColor) lastColor else part.color)
                        appendComponentToCurrentLine(
                            container,
                            part.builder?.invoke(sourceComponent) ?: sourceComponent
                        )
                        currentLineText += container
                        container = ""
                        return true
                    }

                    part.parts.forEach { part ->
                        val lines = part.replace("\r\n", "\n").split("\n")
//                        if (lines.size > 1)
//                        logcat { "Lines[${lines.size}]: ${lines.joinToString(", ")}" }
                        lines.forEachIndexed { index, line ->
                            if (supportsNewLines && maxWidth != null && currentLineText.length + container.length + 1 + line.length > maxWidth) {
//                                logcat { "Building currentLineText=${currentLineText.length} container=${container.length} line=${line.length} max=$maxWidth" }
                                appendContainerToLine()
                                tryFinishCurrentLine()
                                container = line
                            } else {
//                                logcat { "Appending currentLineText=${currentLineText.length} container=${container.length} line=${line.length} max=$maxWidth" }
                                if (container.isNotBlank())
                                    container += " "
                                container += line
                            }

                            if (supportsNewLines && index < lines.size - 1) {
                                if (debug)
                                    logcat { "[RequireNewLine] Building line (current one not empty)" }
                                appendContainerToLine()
                                if (!tryFinishCurrentLine()) {
                                    appendComponentToCurrentLine("", Component.empty())
                                    tryFinishCurrentLine(true)
                                }
                            }
                        }
                    }

                    if (container.isNotBlank())
                        appendContainerToLine()

                    lastColor = part.color ?: lastColor
                }

                is ComponentPart -> {
                    appendComponentToCurrentLine(
                        PlainTextComponentSerializer.plainText().serialize(part.component),
                        part.component
                    )
//                    items.add(Component.text("", lastColor ) + part.component)
                }
            }
        }

        tryFinishCurrentLine()

//        items.forEach {
//            logcat { GsonComponentSerializer.gson().serialize(it) }
//        }

        return items
    }

    fun buildSingleComponent(): Component {
        val components = buildLineComponents(applyDefaultStyle = false)
        if (components.isEmpty()) return Component.empty()
        val component = Component.text()
        if (defaultStyle != null)
            component.style(defaultStyle)
        components.forEach {
            component.append(it)
        }
        return component.build()
    }

    companion object {
        private val debug = false

        fun <T> ComponentBuilder<T>.resume(builder: ComponentBuilder<T>.() -> Unit): ComponentBuilder<T> {
            builder(this)
            return this
        }

        fun displayNameBuilder(builder: DisplayNameBuilder.() -> Unit) =
            DisplayNameBuilder().apply { builder(this) }.buildSingleComponent()

        fun loreBuilder(builder: LoreBuilder.() -> Unit) =
            LoreBuilder().apply { builder(this) }.buildLineComponents()

        fun chatBuilder(builder: ChatBuilder.() -> Unit) =
            ChatBuilder().apply { builder(this) }.buildSingleComponent()

        fun ItemStack.displayName(builder: DisplayNameBuilder): ItemStack {
            updateMeta<ItemMeta> {
                displayName(builder.buildSingleComponent())
            }
            return this
        }

        fun ItemStack.lore(builder: LoreBuilder): ItemStack {
            updateMeta<ItemMeta> {
                lore(builder.buildLineComponents())
            }
            return this
        }

        fun ItemStack.displayNameWithBuilder(builder: DisplayNameBuilder.() -> Unit): ItemStack {
            val result = this@Companion.displayNameBuilder(builder)
            updateMeta<ItemMeta> {
                displayName(result)
            }
            return this
        }

        fun ItemStack.loreWithBuilder(builder: LoreBuilder.() -> Unit): ItemStack {
            val result = this@Companion.loreBuilder(builder)
            updateMeta<ItemMeta> {
                lore(result)
            }
            return this
        }

        fun Entity.customNameWithBuilder(builder: DisplayNameBuilder.() -> Unit): Entity {
            val result = this@Companion.displayNameBuilder(builder)
            customName(result)
            return this
        }
    }

    class LoreBuilder : ComponentBuilder<LoreBuilder>(
        defaultColor = CVTextColor.MENU_DEFAULT_LORE,
        maxWidth = 50,
        supportsNewLines = true,
        defaultStyle = Style.style().decoration(TextDecoration.ITALIC, false).color(CVTextColor.MENU_DEFAULT_LORE)
            .build(),
    ) {
        fun textOnNewLine(
            text: String,
            color: TextColor? = defaultColor,
            addSpace: Boolean = true,
            tag: String? = null,
            usePreviousColor: Boolean = false,
            endWithBlankLine: Boolean = false,
            builder: (Component.() -> Component)? = null
        ): LoreBuilder {
            moveToBlankLine()
            text(
                text = text,
                color = color,
                addSpace = addSpace,
                tag = tag,
                usePreviousColor = usePreviousColor,
                endWithBlankLine = endWithBlankLine,
                builder = builder,
            )
            return this
        }

        fun action(text: String): LoreBuilder {
            val lastComponent = components().lastOrNull()
            if (lastComponent == null || (lastComponent is TextPart && lastComponent.tag == "action")) {
                moveToBlankLine()
            } else {
                emptyLines(1)
            }
            text(text, CVTextColor.MENU_DEFAULT_LORE_ACTION, tag = "action")
            return this
        }

        fun labeled(label: String, value: String): LoreBuilder {
            moveToBlankLine()
            text(label, CVTextColor.MENU_DEFAULT_LORE)
            text(" ")
            text(value, CVTextColor.MENU_DEFAULT_LORE_ACCENT)
            moveToBlankLine()
            return this
        }

        fun accented(
            text: String,
            addSpace: Boolean = true,
            tag: String? = null,
            usePreviousColor: Boolean = false,
            endWithBlankLine: Boolean = false,
            builder: (Component.() -> Component)? = null
        ): LoreBuilder {
            text(
                text,
                CVTextColor.MENU_DEFAULT_LORE_ACCENT,
                addSpace = addSpace,
                tag = tag,
                usePreviousColor = usePreviousColor,
                endWithBlankLine = endWithBlankLine,
                builder = builder
            )
            return this
        }

        fun error(
            text: String, addSpace: Boolean = true,
            tag: String? = null,
            usePreviousColor: Boolean = false,
            endWithBlankLine: Boolean = false,
            builder: (Component.() -> Component)? = null
        ): LoreBuilder {
            text(
                text,
                CVTextColor.MENU_DEFAULT_LORE_ERROR,
                addSpace = addSpace,
                tag = tag,
                usePreviousColor = usePreviousColor,
                endWithBlankLine = endWithBlankLine,
                builder = builder
            )
            return this
        }
    }

    class DisplayNameBuilder : ComponentBuilder<LoreBuilder>(
        defaultColor = CVTextColor.MENU_DEFAULT_TITLE,
        maxWidth = null,
        supportsNewLines = false,
        defaultStyle = Style.style().decoration(TextDecoration.ITALIC, false).build(),
    ) {
        fun accented(text: String): DisplayNameBuilder {
            text(text, CVTextColor.MENU_DEFAULT_TITLE_ACCENT)
            return this
        }

        fun subtle(text: String): DisplayNameBuilder {
            text(text, CVTextColor.subtle)
            return this
        }
    }

    class ChatBuilder : ComponentBuilder<LoreBuilder>(
        defaultColor = null,
        maxWidth = null,
        supportsNewLines = true,
        defaultStyle = null,
    )
}