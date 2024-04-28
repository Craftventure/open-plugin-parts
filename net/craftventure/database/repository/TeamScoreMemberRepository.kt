package net.craftventure.database.repository

import net.craftventure.database.generated.cvdata.Cvdata
import net.craftventure.database.generated.cvdata.tables.daos.TeamScoreMemberDao
import net.craftventure.database.generated.cvdata.tables.pojos.TeamScoreMember
import net.craftventure.database.generated.cvdata.tables.records.TeamScoreMemberRecord
import org.jooq.Configuration
import java.util.*

class TeamScoreMemberRepository(
    configuration: Configuration
) : BaseIdRepository<TeamScoreMemberRecord, TeamScoreMember, UUID>(
    TeamScoreMemberDao(configuration)
) {
    fun getMembersForTeam(team: UUID) = withDslIgnoreErrors(emptyList()) { dsl ->
        dsl.selectFrom(table).where(Cvdata.CVDATA.TEAM_SCORE_MEMBER.TEAM.eq(team)).fetchInto(dao.type)
    }
}