package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class JacksonConfiguration {
  /**
   * This is the legacy jackson 2 mapper, once we've moved
   * to jackson 3 we can remove this class as the
   * spring3 managed defaults are fine
   *
   * note - in Jackson 3 both of the disabled settings below are
   * disabled by default.
   *
   * see https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md
   */
  @Bean
  @Primary
  fun jsonMapper(): JsonMapper = JsonMapper().apply {
    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    findAndRegisterModules()
  }
}
