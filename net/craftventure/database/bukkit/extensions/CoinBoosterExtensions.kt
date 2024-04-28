package net.craftventure.database.bukkit.extensions

import net.craftventure.core.ktx.util.DateUtils
import net.craftventure.database.generated.cvdata.tables.pojos.CoinBooster


fun CoinBooster.describe() = "${coinBoosterType!!.prefix}$value"
fun CoinBooster.describeWithDuration() =
    "${coinBoosterType!!.prefix}$value for ${DateUtils.format(duration!!.toLong(), "?")}"