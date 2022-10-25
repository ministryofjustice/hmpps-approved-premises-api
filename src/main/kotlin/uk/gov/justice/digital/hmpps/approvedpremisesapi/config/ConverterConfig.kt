package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.convert.EnumConverterFactory

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
 * @see uk.gov.justice.digital.hmpps.approvedpremisesapi.convert.EnumConverterFactory
 */
@Configuration
class ConverterConfig : WebMvcConfigurer {
  override fun addFormatters(registry: FormatterRegistry) = registry.addConverterFactory(EnumConverterFactory())
}
