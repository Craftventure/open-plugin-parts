package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.tables.daos.EmojiDao
import net.craftventure.database.generated.cvdata.tables.pojos.Emoji
import net.craftventure.database.generated.cvdata.tables.records.EmojiRecord
import org.jooq.Configuration

class EmojiRepository(configuration: Configuration) : BaseIdRepository<EmojiRecord, Emoji, String>(
    EmojiDao(configuration),
    shouldCache = true
)