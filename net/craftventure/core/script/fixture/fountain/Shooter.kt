package net.craftventure.core.script.fixture.fountain

import net.craftventure.core.script.fixture.Location
import net.craftventure.core.script.fixture.property.DoubleProperty

class Shooter(
    name: String,
    location: Location
) : Fountain(
    name,
    location,
    "fountain:shooter"
) {
    init {
        properties += DoubleProperty("pressure", 0.0, min = 0.0, max = 2.0)
    }

    override fun destroy() {}
}
