package net.craftventure.database.repository

import net.craftventure.core.ktx.extension.toUuid
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.RideCounterDao
import net.craftventure.database.generated.cvdata.tables.pojos.RideCounter
import net.craftventure.database.generated.cvdata.tables.records.RideCounterRecord
import org.jooq.Configuration
import java.time.LocalDateTime
import java.util.*

class RideCounterRepository(
    configuration: Configuration
) : BaseIdRepository<RideCounterRecord, RideCounter, UUID>(
    RideCounterDao(configuration),
) {
    fun reset(uuid: UUID) = withDslIgnoreErrors(false) { dsl ->
        dsl.deleteFrom(table)
            .where(Cvdata.CVDATA.RIDE_COUNTER.UUID.eq(uuid))
            .execute()
        true
    }

    fun get(uuid: UUID, rideName: String): RideCounter? {
        val ride = MainRepositoryProvider.rideRepository.cachedItems.firstOrNull { it.name == rideName } ?: return null
        return get(uuid, ride.id!!)
    }

    fun get(uuid: UUID, rideId: UUID): RideCounter? = withDslIgnoreErrors(null) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.RIDE_COUNTER.UUID.eq(uuid))
            .and(Cvdata.CVDATA.RIDE_COUNTER.RIDE_ID.eq(rideId))
            .fetchOneInto(dao.type)
    }

    fun setCounter(uuid: UUID, rideName: String, count: Int): Boolean {
        val ride = MainRepositoryProvider.rideRepository.cachedItems.firstOrNull { it.name == rideName } ?: return false
        return setCounter(uuid, ride.id!!, count)
    }

    fun setCounter(uuid: UUID, rideId: UUID, count: Int): Boolean = withDslIgnoreErrors(false) { dsl ->
        val ride = MainRepositoryProvider.rideRepository.findSilent(rideId)
        if (ride != null) {
            val updated = dsl.update(table)
                .set(Cvdata.CVDATA.RIDE_COUNTER.COUNT, count)
                .set(Cvdata.CVDATA.RIDE_COUNTER.LAST_AT, LocalDateTime.now())
                .where(Cvdata.CVDATA.RIDE_COUNTER.UUID.eq(uuid))
                .and(Cvdata.CVDATA.RIDE_COUNTER.RIDE_ID.eq(rideId))
                .execute() == 1

            if (updated) {
                return@withDslIgnoreErrors true
            } else {
                val rideCounter = RideCounter(
                    UUID.randomUUID(),
                    uuid,
                    rideId,
                    1,
                    LocalDateTime.now(),
                    LocalDateTime.now()
                )
                if (createSilent(rideCounter)) {
                    triggerListenerCreate(rideCounter)
                    return@withDslIgnoreErrors true
                }
            }
        }
        false
    }

    fun increaseCounter(uuid: UUID, rideName: String): Boolean {
        val ride = MainRepositoryProvider.rideRepository.cachedItems.firstOrNull { it.name == rideName } ?: return false
        return increaseCounter(uuid, ride.id!!)
    }

    fun increaseCounter(uuid: UUID, rideId: UUID): Boolean = withDslIgnoreErrors(false) { dsl ->
        val updated = dsl.update(table)
            .set(Cvdata.CVDATA.RIDE_COUNTER.COUNT, Cvdata.CVDATA.RIDE_COUNTER.COUNT.plus(1))
            .set(Cvdata.CVDATA.RIDE_COUNTER.LAST_AT, LocalDateTime.now())
            .where(Cvdata.CVDATA.RIDE_COUNTER.UUID.eq(uuid))
            .and(Cvdata.CVDATA.RIDE_COUNTER.RIDE_ID.eq(rideId))
            .execute() == 1

        if (updated) {
            val rideCounter = get(uuid, rideId)
            if (rideCounter != null) {
                triggerListenerUpdate(rideCounter)
            }
            return@withDslIgnoreErrors true
        } else {
            val rideCounter = RideCounter(UUID.randomUUID(), uuid, rideId, 1, LocalDateTime.now(), LocalDateTime.now())
            if (createSilent(rideCounter)) {
                return@withDslIgnoreErrors true
            }
        }
        false
    }

    fun findAllForUuid(uuid: UUID): List<RideCounter> = withDslIgnoreErrors(emptyList()) { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.RIDE_COUNTER.UUID.eq(uuid))
            .fetchInto(dao.type)
    }

    fun getTopCounters(rideName: String, limit: Long = 10): List<RideCounter> {
        val ride =
            MainRepositoryProvider.rideRepository.cachedItems.firstOrNull { it.name == rideName } ?: return emptyList()
        return getTopCounters(ride.id, limit)
    }

    fun getTopCounters(ride: UUID?, limit: Long = 10): List<RideCounter> = withDslIgnoreErrors(emptyList()) { dsl ->
        val query = if (ride != null) {
            dsl.selectFrom(table)
                .where(Cvdata.CVDATA.RIDE_COUNTER.RIDE_ID.eq(ride))
                .and(Cvdata.CVDATA.RIDE_COUNTER.UUID.notIn(MainRepositoryProvider.aegisBlackListRideRepository.cachedItems.map { it.uuid }))
        } else {
            dsl.selectFrom(table)
        }

        query.orderBy(Cvdata.CVDATA.RIDE_COUNTER.COUNT.desc())
            .limit(limit)
            .fetchInto(dao.type)
    }

    companion object {
        var counterNico = 6000
        val uuidNico = "7da4519c-3482-4681-9d67-769fb4b48193".toUuid()!!
    }
}