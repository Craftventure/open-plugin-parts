package net.craftventure.core.npc.actor

import com.google.gson.annotations.Expose
import com.squareup.moshi.JsonClass
import net.craftventure.database.MainRepositoryProvider
import net.craftventure.database.generated.cvdata.tables.pojos.CachedGameProfile

@JsonClass(generateAdapter = true)
class TalkingHeads(
    @field:Expose val profileGroups: Array<ProfileGroup> = emptyArray(),
    @field:Expose val periods: Array<Period> = emptyArray(),
    @field:Expose val fallbackProfileGroupId: Int = 1
) {
    fun getCurrentProfileGroup(atMilliseconds: Long): ProfileGroup? {
        val targetProfileGroupId =
            periods.firstOrNull { it.start <= atMilliseconds && it.stop - 100 > atMilliseconds }?.id
                ?: fallbackProfileGroupId
//        Logger.debug("Target group for $atMilliseconds is $targetProfileGroupId for ${periods.map { "${it.start} to ${it.stop} wit ${it.id}" }}")
        return profileGroups.firstOrNull { it.id == targetProfileGroupId }
    }

    @JsonClass(generateAdapter = true)
    class ProfileGroup(
        @field:Expose val id: Int = 1,
        @field:Expose val profiles: Array<String> = emptyArray()
    ) {
        @Transient
        private var cachedProfiles: Array<CachedGameProfile>? = null

        fun getCachedProfiles(): Array<CachedGameProfile> {
            if (cachedProfiles == null)
                cachedProfiles = profiles
                    .map { MainRepositoryProvider.cachedGameProfileRepository.findCached(it) }
                    .filterNotNull()
                    .toTypedArray()
            return cachedProfiles!!
        }
    }

    @JsonClass(generateAdapter = true)
    class Period(
        @field:Expose val id: Int = 1,
        @field:Expose val start: Long = 0,
        @field:Expose val stop: Long = 0
    )
}
