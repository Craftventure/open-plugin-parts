package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.TagGroupDao
import net.craftventure.database.generated.cvdata.tables.pojos.TagGroup
import net.craftventure.database.generated.cvdata.tables.records.TagGroupRecord
import org.jooq.Configuration
import java.util.*

class TagGroupRepository(
    configuration: Configuration
) : BaseIdRepository<TagGroupRecord, TagGroup, UUID>(
    TagGroupDao(configuration)
) {
    @Throws(Exception::class)
    fun getByTag(tag: String) = withDsl { dsl ->
        dsl.selectFrom(table)
            .where(Cvdata.CVDATA.TAG_GROUP.TAG.eq(tag))
            .query
            .fetchOneInto(TagGroup::class.java)
    }

    @Throws(Exception::class)
    fun create(
        id: UUID,
        tag: String,
        channelId: Long,
        creatorId: Long,
        trackingMessageId: Long?,
        message: String?
    ): Boolean {
        return withDsl { dsl ->
            val tagGroup = dsl.newRecord(Cvdata.CVDATA.TAG_GROUP)
            tagGroup.id = id
            tagGroup.tag = tag
            tagGroup.channelId = channelId
            tagGroup.creator = creatorId
            tagGroup.trackingMessageId = trackingMessageId
            tagGroup.message = message

            val insert = dsl.insertInto(table)
                .set(tagGroup.intoMap())
                .onDuplicateKeyIgnore()
                .execute()
            insert == 1
        }
    }

    @Throws(Exception::class)
    fun delete(tag: String) {

    }
}