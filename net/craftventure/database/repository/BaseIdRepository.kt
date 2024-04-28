package net.craftventure.database.repository

import net.craftventure.core.ktx.util.Logger
import org.jooq.DAO
import org.jooq.UpdatableRecord
import org.jooq.exception.DataAccessException
import java.util.concurrent.Semaphore

abstract class BaseIdRepository<R : UpdatableRecord<R>, P, T>(
    dao: DAO<R, P, T>,
    val shouldCache: Boolean = false
) : BaseDaoRepository<R, P, T>(dao) {
    val semaphore = Semaphore(1, true)
    var hasLoadedCache = false
        private set

    var cachedItems: List<P> = emptyList()
        private set

    private val listeners = hashSetOf<Listener<P>>()

    fun all() = withDslIgnoreErrors(emptyList()) { dsl ->
        dsl.selectFrom(table)
            .fetchInto(dao.type)
    }

    fun addUpdateListener(listener: Listener<P>): () -> Boolean {
        listeners.add(listener)
        return { listeners.remove(listener) }
    }

    fun addListener(listener: Listener<P>): () -> Boolean {
        listeners.add(listener)
        return { listeners.remove(listener) }
    }

    fun removeListener(listener: Listener<P>) {
        listeners.remove(listener)
    }

    protected fun triggerListenerMerge(items: List<P>) {
        listeners.forEach { it.onChange(items, Listener.ChangeType.Merge) }
        invalidateCaches()
        // TODO: Implement recreating caches here
    }

    protected fun triggerListenerMerge(item: P) {
        listeners.forEach { it.onChange(item, Listener.ChangeType.Merge) }
        invalidateCaches()
        // TODO: Implement recreating caches here
    }

    protected fun triggerListenerCreate(items: List<P>) {
        listeners.forEach { it.onChange(items, Listener.ChangeType.Insert) }
        invalidateCaches()
        // TODO: Implement recreating caches here
    }

    protected fun triggerListenerCreate(item: P) {
        listeners.forEach { it.onChange(item, Listener.ChangeType.Insert) }
        invalidateCaches()
        // TODO: Implement recreating caches here
    }

    protected fun triggerListenerDelete(items: List<P>) {
        listeners.forEach { it.onChange(items, Listener.ChangeType.Delete) }
        invalidateCaches()
        // TODO: Implement recreating caches here
    }

    protected fun triggerListenerDelete(item: P) {
        listeners.forEach { it.onChange(item, Listener.ChangeType.Delete) }
        invalidateCaches()
        // TODO: Implement recreating caches here
    }

    protected fun triggerListenerUpdate(items: List<P>) {
        listeners.forEach { it.onChange(items, Listener.ChangeType.Update) }
        invalidateCaches()
        // TODO: Implement recreating caches here
    }

    protected fun triggerListenerUpdate(item: P) {
        listeners.forEach { it.onChange(item, Listener.ChangeType.Update) }
        invalidateCaches()
        // TODO: Implement recreating caches here
    }

    fun itemsPojo(): List<P> = withDsl { it.selectFrom(table).fetchInto(dao.type) }
    fun itemsPojoCursored(action: (P) -> Unit) {
        withDsl {
            it.selectFrom(table).stream().forEach {
                val item = it.into(dao.type)
                if (item != null) {
                    action(item)
                }
            }
        }
    }

    fun clearCache() {
        cachedItems = emptyList()
        hasLoadedCache = false
    }

    fun invalidateCaches() {
        hasLoadedCache = false

        listeners.forEach { it.invalidateCaches() }
    }

    fun requireCache() {
        if (!hasLoadedCache) {
            prepareCache(true)
        }
    }

    @Throws(DataAccessException::class)
    private fun prepareCache(force: Boolean = false) {
        semaphore.acquire()
        try {
            if (hasLoadedCache && !force) return
            cachedItems = dao.findAll()
            hasLoadedCache = true
            onAfterCache()
            listeners.forEach {
                it.onChange(cachedItems, Listener.ChangeType.Refresh)
            }
        } finally {
            semaphore.release()
        }
    }

    protected open fun onAfterCache() {

    }

    @JvmOverloads
    fun findCached(id: T, loadIfCacheInvalid: Boolean = false, idHelper: (P) -> T = { dao.getId(it) }): P? {
        if (loadIfCacheInvalid && !hasLoadedCache) {
            try {
                prepareCache()
            } catch (e: Exception) {
                Logger.capture(e)
            }
        }
        return cachedItems.firstOrNull { idHelper(it) == id }
    }

    open fun update(items: List<P>): Boolean {
        return try {
            dao.update(items)
            triggerListenerUpdate(items)
            true
        } catch (e: Exception) {
            Logger.capture(e)
            false
        }
    }

    open fun update(item: P): Boolean {
        return try {
            dao.update(item)
            triggerListenerUpdate(item)
            true
        } catch (e: Exception) {
            Logger.capture(e)
            false
        }
    }

    open fun createOrUpdate(item: P) {
        return try {
            dao.insert(item)
            triggerListenerCreate(item)
        } catch (e: Exception) {
            e.printStackTrace()
            dao.update(item)
            triggerListenerUpdate(item)
        }
    }

    open fun createOrUpdateSilent(item: P): Boolean {
        return try {
            dao.insert(item)
            triggerListenerCreate(item)
            true
        } catch (e: Exception) {
            try {
                dao.update(item)
                triggerListenerUpdate(item)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    open fun create(item: P) = createSilent(item)
    open fun createSilent(item: P): Boolean =
        try {
            dao.insert(item)
            triggerListenerCreate(item)
            true
        } catch (e: Exception) {
//            e.printStackTrace()
            false
        }

    @Deprecated("Use merge instead")
    open fun createIfNotExists(items: List<P>) =
        try {
            dao.insert(items)
            triggerListenerCreate(items)
        } catch (e: Exception) {
//            Logger.capture(e)
        }

    open fun createIfNotExistsMerged(item: P) =
        try {
            dao.merge(item)
            triggerListenerMerge(item)
            true
        } catch (e: Exception) {
//            Logger.capture(e)
            false
        }

    open fun createIfNotExistsMerged(items: List<P>) =
        try {
            dao.merge(items)
            triggerListenerMerge(items)
            true
        } catch (e: Exception) {
//            Logger.capture(e)
            false
        }

    open fun findSilent(id: T) = try {
        find(id)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    open fun find(id: T): P? = dao.findById(id)

    fun delete(item: P) = try {
        dao.delete(item)
        triggerListenerDelete(item)
        true
    } catch (e: Exception) {
        Logger.capture(e)
        false
    }

    open fun deleteById(id: T) = dao.deleteById(id)

    /**
     * This listener does not guarantee to be called every time, it's actually up to the repository to implement this where needed/wanted
     */
    abstract class Listener<P> {
        fun onChange(items: List<P>, type: ChangeType) = items.forEach { onChange(it, type) }
        fun onChange(item: P, type: ChangeType) {
            when (type) {
                ChangeType.Merge -> onMerge(item)
                ChangeType.Update -> onUpdate(item)
                ChangeType.Insert -> onInsert(item)
                ChangeType.Delete -> onDelete(item)
                ChangeType.Refresh -> onRefresh(item)
            }
        }

        open fun onMerge(item: P) {}
        open fun onUpdate(item: P) {}
        open fun onInsert(item: P) {}
        open fun onDelete(item: P) {}
        open fun onRefresh(item: P) {}

        open fun invalidateCaches() {}

        enum class ChangeType {
            Merge,
            Update,
            Insert,
            Delete,
            Refresh,
        }
    }

    abstract class UpdateListener<P> : Listener<P>() {
        override fun onInsert(item: P) {
            onUpdated(item)
        }

        override fun onUpdate(item: P) {
            onUpdated(item)
        }

        override fun onMerge(item: P) {
            onUpdated(item)
        }

        abstract fun onUpdated(item: P)
    }
}