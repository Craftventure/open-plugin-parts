package net.craftventure.database.type

import org.jooq.impl.EnumConverter


enum class ConsumableType(
    val displayName: String,
    val consumeUponUsage: Boolean,
    val allowOwningMultiple: Boolean = true
) {
    SINGLE(displayName = "permanent", consumeUponUsage = false, allowOwningMultiple = false),
    CONSUMABLE(displayName = "consumable", consumeUponUsage = true),
    CUSTOMIZABLE(displayName = "customizable", consumeUponUsage = false, allowOwningMultiple = true);

    companion object {
        class Converter : EnumConverter<String, ConsumableType>(String::class.java, ConsumableType::class.java)
    }
}
