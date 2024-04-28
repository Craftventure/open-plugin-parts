package net.craftventure.database.extension

import org.jooq.Query

fun <T : Query> T.execute(expect: Int = 1, successAction: (Int) -> Unit): Int {
    return this.execute().also {
        if (it == expect) {
            successAction(it)
        }
    }
}