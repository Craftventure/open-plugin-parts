package net.craftventure.core.ride.tracklessride

import net.craftventure.core.ride.tracklessride.programpart.data.TagContext

interface TagContainer {
    fun addTag(context: TagContext = TagContext.GLOBAL, tag: String)
    fun removeTag(context: TagContext = TagContext.GLOBAL, tag: String)
    fun hasTag(context: TagContext = TagContext.GLOBAL, tag: String): Boolean
    fun clear(context: TagContext)
    fun get(context: TagContext): Set<String>
}