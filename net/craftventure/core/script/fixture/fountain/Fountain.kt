package net.craftventure.core.script.fixture.fountain

import net.craftventure.core.ktx.extension.toOptional
import net.craftventure.core.script.fixture.Fixture
import net.craftventure.core.script.fixture.Location
import net.craftventure.core.script.fixture.property.BooleanProperty
import net.craftventure.core.script.timeline.KeyFrameEasing

abstract class Fountain(
    name: String,
    location: Location,
    kind: String
) : Fixture(
    name,
    location,
    kind
) {
    init {
        properties.add(
            BooleanProperty(
                "play",
                false,
                inEasingOverride = KeyFrameEasing.PREVIOUS.toOptional(),
                outEasingOverride = KeyFrameEasing.PREVIOUS.toOptional()
            )
        )
    }
}