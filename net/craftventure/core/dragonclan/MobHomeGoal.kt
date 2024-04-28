package net.craftventure.core.dragonclan

import com.destroystokyo.paper.entity.ai.Goal
import com.destroystokyo.paper.entity.ai.GoalKey
import com.destroystokyo.paper.entity.ai.GoalType
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Monster
import java.util.*

class MobHomeGoal(
    private val mob: Monster,
    private val home: Location,
    private val travelRange: Double,
) : Goal<Monster> {
    private val key = GoalKey.of(Monster::class.java, NamespacedKey("craftventure", "mob_home"))
    private val types = EnumSet.of(GoalType.MOVE)
    private val distanceSquared = travelRange * travelRange

    override fun shouldActivate(): Boolean {
        return mob.location.distanceSquared(home) > distanceSquared ||
                (mob.target != null && mob.target!!.location.distanceSquared(home) > distanceSquared)
    }

    override fun getKey(): GoalKey<Monster> {
        return key
    }

    override fun getTypes(): EnumSet<GoalType> = types

    override fun tick() {
        super.tick()

        if (mob.target != null)
            mob.target = null

        if (mob.pathfinder.currentPath?.finalPoint != home)
            mob.pathfinder.moveTo(home)
    }
}