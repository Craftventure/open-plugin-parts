package net.craftventure.core.ktx.json

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class JsonTest1(
    @Json(name = "Swagger")
    val nonNullableBoolean: Boolean,
    val nullableBoolean1: Boolean?,
    @Json(name = "Henk")
    val nullableBoolean2: Boolean?,
    val nullableBooleanWithDefault: Boolean? = false,
    /**
     * Javadoc in de code
     */
    val string: String,
    val nullableString: String?,
    val otherJsonClass: JsonTest1Sub,
    val otherJsonClassNullable: JsonTest1Sub? = null,
    val listOfStrings: List<String>,
    val listOfNullableStrings: List<String?>,
    val listOfJsonTest1Subs: List<JsonTest1Sub>,
    val listOfJsonTest1SubsNullable: List<JsonTest1Sub?>,
    val setOfBooleans: Set<Boolean>,
    val mapOfStringToBoolean: Map<String, Boolean>,
    val mapOfStringToNullableBoolean: Map<String, Boolean?>,
    val enumType: EnumTestType,
) {
    @JsonClass(generateAdapter = true)
    class JsonTest1Sub(
        val string: String,
        val nullableStringDefaultToNull: String? = null,
    )

    enum class EnumTestType {
        Value1,

        @Json(name = "value_2_name")
        Value2
    }
}