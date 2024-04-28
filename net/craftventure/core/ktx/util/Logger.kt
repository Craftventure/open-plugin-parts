package net.craftventure.core.ktx.util

import java.util.regex.Pattern


@Deprecated(message = "")
object Logger {
    private val CALL_STACK_INDEX = 3
    private val ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$")
    var tree: Tree? = null

    private val tag: String
        get() {
            val stackTrace = Thread.currentThread().stackTrace
            return try {
                var tag = stackTrace[CALL_STACK_INDEX]
                for (i in CALL_STACK_INDEX until stackTrace.size) {
                    val trace = stackTrace[i]
                    if (trace.className != javaClass.name) {
                        tag = trace
                        break
                    }
                }
                createStackElementTag(tag)
            } catch (e: Exception) {
                e.printStackTrace()
                "?"
            }
        }

    @JvmStatic
    fun getCallerLine(): String {
        val stackTrace = Thread.currentThread().stackTrace
        return try {
            var tag = stackTrace[CALL_STACK_INDEX]
            for (i in CALL_STACK_INDEX until stackTrace.size) {
                val trace = stackTrace[i]
                if (trace.className != javaClass.name) {
                    tag = trace
                    break
                }
            }
            return createStackElementTag(tag)
        } catch (e: Exception) {
            "?"
        }
    }

    @JvmStatic
    @JvmOverloads
    fun miniTrace(traceLevel: Int = 5, thread: Thread = Thread.currentThread()): String {
        val stackTrace = thread.stackTrace
        return try {
            var tag = ""
            for (i in CALL_STACK_INDEX until Math.min(stackTrace.size, CALL_STACK_INDEX + traceLevel)) {
                val trace = stackTrace[i]
                if (trace.className != javaClass.name) {
                    if (tag.isNotEmpty())
                        tag += " > "
                    tag += createStackElementTag(trace)
                }
            }
            return tag
        } catch (e: Exception) {
            "?"
        }
    }

    private fun doLog(
        tag: String,
        level: Level,
        log: String,
        logToCrew: Boolean = false,
        vararg params: Any? = emptyArray()
    ) {
        tree?.doLog(tag, level, log, logToCrew, *params)
    }

    @JvmStatic
    @JvmOverloads
    fun log(level: Level, log: String, logToCrew: Boolean = false, vararg params: Any? = emptyArray()) {
        doLog(tag, level, log, logToCrew, *params)
    }

    @JvmStatic
    @JvmOverloads
    fun info(log: String, logToCrew: Boolean = false, vararg params: Any? = emptyArray()) {
        doLog(tag, Level.INFO, log, logToCrew, *params)
    }

    @JvmStatic
    @JvmOverloads
    fun warn(log: String, logToCrew: Boolean = false, vararg params: Any? = emptyArray()) {
        doLog(tag, Level.WARNING, log, logToCrew, *params)
    }

    @JvmStatic
    @JvmOverloads
    fun severe(log: String, logToCrew: Boolean = false, vararg params: Any? = emptyArray()) {
        doLog(tag, Level.SEVERE, log, logToCrew, *params)
    }

    @JvmStatic
    @JvmOverloads
    fun debug(log: String, logToCrew: Boolean = false, vararg params: Any? = emptyArray()) {
        doLog(tag, Level.DEBUG, log, logToCrew, *params)
    }

    @JvmOverloads
    @JvmStatic
    fun capture(t: Throwable, print: Boolean = true) {
        if (print)
            t.printStackTrace()
        tree?.captureException(t)
    }

    @JvmStatic
    private fun createStackElementTag(element: StackTraceElement): String {
        var tag = element.className
        val m = ANONYMOUS_CLASS.matcher(tag)
        if (m.find()) {
            tag = m.replaceAll("")
        }
        tag = tag.substring(tag.lastIndexOf('.') + 1) + ":" + element.lineNumber
        return tag
    }

    enum class Level(
        val tag: String
    ) {
        SEVERE("E"),
        WARNING("W"),
        INFO("I"),
        DEBUG("D")
    }

    interface Tree {
        fun captureException(throwable: Throwable)

        fun doLog(
            tag: String,
            level: Level,
            log: String,
            logToCrew: Boolean = false,
            vararg params: Any? = emptyArray()
        )
    }
}
