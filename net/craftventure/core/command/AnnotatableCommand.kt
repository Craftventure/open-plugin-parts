package net.craftventure.core.command

interface AnnotatableCommand {
    fun isAnnotatable(command: List<String>): AnnotationType

    enum class AnnotationType {
        VALID, INVALID, FORBIDDEN
    }
}