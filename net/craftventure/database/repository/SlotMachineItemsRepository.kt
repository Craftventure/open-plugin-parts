package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.tables.daos.SlotMachineItemDao
import net.craftventure.database.generated.cvdata.tables.pojos.SlotMachineItem
import net.craftventure.database.generated.cvdata.tables.records.SlotMachineItemRecord
import org.jooq.Configuration
import java.util.*

class SlotMachineItemsRepository(
    configuration: Configuration
) : BaseIdRepository<SlotMachineItemRecord, SlotMachineItem, UUID>(
    SlotMachineItemDao(configuration),
    shouldCache = true
)