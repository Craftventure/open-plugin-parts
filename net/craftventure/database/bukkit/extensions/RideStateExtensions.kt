package net.craftventure.database.bukkit.extensions

import net.craftventure.bukkit.ktx.util.Translation
import net.craftventure.database.type.RideState

val RideState.stateTranslation: Translation
    get() = Translation.valueOf(stateTranslationTypeName)