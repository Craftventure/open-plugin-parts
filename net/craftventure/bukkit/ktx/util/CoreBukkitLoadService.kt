package net.craftventure.bukkit.ktx.util

import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import net.craftventure.annotationkit.GenerateService
import net.craftventure.bukkit.ktx.json.*
import net.craftventure.core.ktx.json.MoshiBase
import net.craftventure.core.ktx.service.LoadService

@GenerateService
class CoreBukkitLoadService : LoadService {
    override fun init() {
        MoshiBase.withBuilder()
            .add(SimpleAreaAdapter())
            .add(LocationAdapter())
            .add(VectorAdapter())
            .add(PotionEffectTypeAdapter())
            .add(ParticleAdapter())
            .add(ColorAdapter())
            .add(
                PolymorphicJsonAdapterFactory.of(BoundingBoxProducer::class.java, "type")
                    .withSubtype(BoundingBoxProducer.SizedProducer::class.java, "sized")
                    .withSubtype(BoundingBoxProducer.SquareSizedProducer::class.java, "square_sized")
            )
            .add(
                PolymorphicJsonAdapterFactory.of(ParticleAdapter.ParticleOptionJson::class.java, "type")
                    .withSubtype(ParticleAdapter.DustOptionsJson::class.java, "dust_options")
                    .withSubtype(ParticleAdapter.DustTransitionJson::class.java, "dust_transition")
                    .withSubtype(ParticleAdapter.ItemStackJson::class.java, "itemstack")
                    .withSubtype(ParticleAdapter.BlockDataJson::class.java, "blockdata")
                    .withSubtype(ParticleAdapter.FloatJson::class.java, "float")
                    .withSubtype(ParticleAdapter.IntegerJson::class.java, "integer")
            )
    }
}