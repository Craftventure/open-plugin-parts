package net.craftventure.core.script.fixture.fountain

import net.craftventure.core.script.fixture.Location
import net.craftventure.core.script.fixture.property.DoubleProperty
import net.craftventure.core.script.fixture.property.IntProperty

class Bloom(
    name: String,
    location: Location
) : Fountain(
    name,
    location,
    "fountain:bloom"
) {

    init {
        properties.add(DoubleProperty("pressure", 0.0, min = 0.0, max = 2.0))
        properties.add(DoubleProperty("pitch", -50.0))
        properties.add(IntProperty("rays", 0, 1, 10))
    }

    override fun destroy() {}
}
