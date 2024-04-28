package net.craftventure.jsontools

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PolymorphicHint(
    val keyName: String = "type",
    val types: Array<PolymorphicHintType>,
) {
    annotation class PolymorphicHintType(
        val key: String,
        val qualifiedName: String,
    )
}