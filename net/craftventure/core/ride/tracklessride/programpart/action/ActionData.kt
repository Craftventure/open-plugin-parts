package net.craftventure.core.ride.tracklessride.programpart.action

abstract class ActionData {
//    lateinit var type: String

    abstract fun toAction(): Action
}