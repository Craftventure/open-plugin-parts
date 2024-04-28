package net.craftventure.database

import net.craftventure.core.ktx.logging.logcat
import org.jooq.ExecuteContext
import org.jooq.impl.DSL
import org.jooq.impl.DefaultExecuteListener

class DatabaseLoggerListener : DefaultExecuteListener() {
    var debug = false

    override fun exception(ctx: ExecuteContext) {
        if (!debug) return

        if (ctx.query() != null) {
            val sql =
                DSL.using(ctx.configuration()).renderInlined(ctx.query()).takeIf { it.isNotBlank() } ?: ctx.sql()
            logcat(LogPriority.INFO) { "SQL/Execute: $sql" }
        }
    }

    override fun renderEnd(ctx: ExecuteContext) {
        if (!debug) return

        if (ctx.query() != null) {
            val sql =
                DSL.using(ctx.configuration()).renderInlined(ctx.query()).takeIf { it.isNotBlank() } ?: ctx.sql()
            logcat(LogPriority.INFO) { "SQL/Execute: $sql" }
        }
    }
}