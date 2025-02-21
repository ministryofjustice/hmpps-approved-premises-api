package uk.gov.justice.digital.hmpps.approvedpremisesapi.convert

import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.converter.ConverterFactory
import org.springframework.stereotype.Component
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * Allows Spring to correctly deserialize enums coming in as controller parameters when the "public" value (as defined
 * in the specification) doesn't match the "implementation" value (the name given to it by the generator, which must be
 * a legal Kotlin identifier).
 *
 * This is a workaround, and ideally such conversion logic should be provided by the "kotlin-spring" OpenAPI Generator
 * profile.
 *
 * A PR exists to implement this for the "JavaSpring" profile
 * [here](https://github.com/OpenAPITools/openapi-generator/pull/13349)
 * but not for Kotlin as far as I could see.
 *
 * @see uk.gov.justice.digital.hmpps.approvedpremisesapi.config.ConverterConfig
 */
@Component
class EnumConverterFactory : ConverterFactory<String, Enum<*>> {
  class EnumConverter<T : Enum<*>>(private val targetType: Class<T>) : Converter<String, T> {
    override fun convert(source: String): T? = targetType.enumConstants.firstOrNull {
      it.getOpenApiValueOrDefault() == source
    }

    @Suppress("UNCHECKED_CAST")
    private fun T.getOpenApiValueOrDefault(): String {
      // OpenAPI-generated enums will have a field named 'value', but not all enums will.
      // If it's some other enum then default to the name of the enum value.
      val valueProperty = this::class.declaredMemberProperties.firstOrNull { it.name == "value" } as KProperty1<T, String>?
      return if (valueProperty == null) {
        this.name
      } else {
        val access = valueProperty.isAccessible
        valueProperty.isAccessible = true
        val result = valueProperty.get(this)
        valueProperty.isAccessible = access
        result
      }
    }
  }

  override fun <T : Enum<*>> getConverter(targetType: Class<T>): Converter<String, T> = EnumConverter(targetType)
}
