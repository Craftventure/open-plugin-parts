package net.craftventure.core.script.fixture.fountain

import net.craftventure.core.script.fixture.Location
import net.craftventure.core.script.fixture.property.DoubleProperty

class OarsmanJet(
    name: String,
    location: Location
) : Fountain(
    name,
    location,
    "fountain:oarsmanjet"
) {
    init {
        properties.add(DoubleProperty("pressure", 0.0, min = 0.0, max = 2.0))
        properties.add(DoubleProperty("heading", 0.0))
        properties.add(DoubleProperty("pitch", -90.0))
    }

    override fun destroy() {}
}
