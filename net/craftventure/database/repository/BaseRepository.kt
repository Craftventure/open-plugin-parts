package net.craftventure.database.repository

import net.craftventure.core.ktx.util.Logger
import org.jooq.*
import org.jooq.impl.DSL
import java.io.File
import java.sql.Connection

abstract class BaseRepository<R : Record>(
    val configuration: Configuration,
    val table: Table<R>,
    val queryDialect: SQLDialect
) {
    fun <T> withDsl(
        configuration: Configuration = this.configuration,
        action: (context: DSLContext) -> T
    ): T {
        return action(configuration.dsl())
    }

    fun <T> withDslIgnoreErrors(
        default: T,
        configuration: Configuration = this.configuration,
        action: (context: DSLContext) -> T
    ): T {
        return try {
            action(configuration.dsl())
        } catch (e: Exception) {
            Logger.capture(e)
            default
        }
    }

    fun backup(to: File) {
        to.printWriter().use { writer ->
            withDsl { dsl ->
                dsl.selectFrom(table)
                    .fetch()
                    .formatJSON(writer)
            }
        }
    }

    fun items() = withDsl { it.fetch(table) }

    fun count() = withDsl { it.fetchCount(table) }

    protected fun Connection.usingDsl(sqlDialect: SQLDialect) = DSL.using(this, sqlDialect)
}