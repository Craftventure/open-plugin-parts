package net.craftventure.core.ride.tracklessride

import net.craftventure.core.ride.tracklessride.programpart.data.TagContext

class BaseTagContainer : TagContainer {
    private val tags = hashMapOf<TagContext, MutableSet<String>>()

    override fun addTag(context: TagContext, tag: String) {
        val set = tags.getOrPut(context) { mutableSetOf() }
//        Logger.debug("Add tag $tag to $context")
        set.add(tag)
    }

    override fun removeTag(context: TagContext, tag: String) {
        val set = tags.getOrPut(context) { mutableSetOf() }
//        Logger.debug("Removing tag $tag in $context")
        set.remove(tag)
    }

    override fun hasTag(context: TagContext, tag: String): Boolean = tags[context]?.contains(tag) == true

    override fun clear(context: TagContext) {
        tags.remove(context)
//        Logger.debug("Clear $context")
    }

    override fun get(context: TagContext): Set<String> = tags[context] ?: emptySet()
}