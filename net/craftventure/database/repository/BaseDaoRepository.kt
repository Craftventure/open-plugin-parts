package net.craftventure.database.repository

import org.jooq.DAO
import org.jooq.DSLContext
import org.jooq.UpdatableRecord

abstract class BaseDaoRepository<R : UpdatableRecord<R>, P, T>(
    protected val dao: DAO<R, P, T>
) : BaseRepository<R>(
    dao.configuration(),
    dao.table,
    dao.dialect()
) {
    private val dsl: DSLContext
        get() = configuration.dsl()
}