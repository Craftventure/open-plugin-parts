package net.craftventure.database.type

import org.jooq.impl.EnumConverter

enum class BankAccountType(
    val internalName: String,
    val singleName: String,
    val pluralName: String,
    val abbreviation: String,
    val startValue: Long,
    val supportRefunds: Boolean = false,
    val modelDataId: Int,
    val emoji: String = abbreviation,
) {
    VC(
        internalName = "vc",
        singleName = "VentureCoin",
        pluralName = "VentureCoins",
        abbreviation = "VC",
        startValue = 100,
        modelDataId = 1,
        supportRefunds = true,
        emoji = "\uE006",
    ),
    PIRATE_TICKET(
        internalName = "pt",
        singleName = "PirateTicket",
        pluralName = "PirateTickets",
        abbreviation = "PT",
        startValue = 0,
        modelDataId = 2,
    ),
    GOLDEN_PIRATE_TICKET(
        internalName = "gpt",
        singleName = "Golden PirateTicket",
        pluralName = "Golden PirateTickets",
        abbreviation = "GPT",
        startValue = 0,
        modelDataId = 3,
    ),
    PIRATE_COIN(
        internalName = "pc",
        singleName = "PirateCoin",
        pluralName = "PirateCoins",
        abbreviation = "PC",
        startValue = 0,
        modelDataId = 4,
    ),
    WINTERCOIN(
        internalName = "wc",
        singleName = "WinterCoin",
        pluralName = "WinterCoins",
        abbreviation = "WC",
        startValue = 0,
        modelDataId = 5,
    ),
    WINTER_TICKETS(
        internalName = "wt",
        singleName = "Winter Ticket",
        pluralName = "Winter Tickets",
        abbreviation = "WT",
        startValue = 0,
        modelDataId = 6,
    );

    fun nameForAmount(amount: Int) = nameForAmount(amount.toLong())
    fun nameForAmount(amount: Long) = when (amount) {
        1L -> singleName
        else -> pluralName
    }

    companion object {
        fun find(value: String): BankAccountType? {
            return values().firstOrNull {
                it.internalName.equals(value, true) ||
                        it.name.equals(value, true)
            }
        }

        class Converter : EnumConverter<String, BankAccountType>(String::class.java, BankAccountType::class.java)
    }
}