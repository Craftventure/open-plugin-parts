package net.craftventure.core.ktx.service

import net.craftventure.annotationkit.GenerateService
import net.craftventure.core.ktx.json.*

@GenerateService
class CoreLoadService : LoadService {
    override fun init() {
        MoshiBase.withBuilder()
            .add(OptionalJsonAdapterFactory())
            .add(BaseJsonAdapters())
            .add(DurationJson::class.java, KotlinDurationAdapter())
    }
}