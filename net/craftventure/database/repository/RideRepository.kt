package net.craftventure.database.repository

import net.craftventure.core.ktx.util.Logger
import net.craftventure.database.extension.execute
import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.RideDao
import net.craftventure.database.generated.cvdata.tables.pojos.Ride
import net.craftventure.database.generated.cvdata.tables.records.RideRecord
import net.craftventure.database.type.RideState
import org.jooq.Configuration
import java.util.*

class RideRepository(
    configuration: Configuration
) : BaseIdRepository<RideRecord, Ride, UUID>(
    RideDao(configuration),
    shouldCache = true
) {
    fun getState(name: String): RideState? = getByName(name)?.state

    fun getByName(name: String): Ride? = withDsl {
        it.selectFrom(table)
            .where(Cvdata.CVDATA.RIDE.NAME.eq(name))
            .limit(1)
            .fetchInto(dao.type)
            .firstOrNull()
    }

    fun setState(ride: Ride, rideState: RideState): Boolean {
        try {
            return withDsl { dsl ->
                dsl.update(table)
                    .set(Cvdata.CVDATA.RIDE.STATE, rideState)
                    .where(Cvdata.CVDATA.RIDE.ID.eq(ride.id))
                    .execute {
                        findSilent(ride.id!!)?.let {
                            triggerListenerUpdate(it)
                        }
                    } == 1
            }
        } catch (e: Exception) {
            Logger.capture(e)
        }
        return false
    }
}