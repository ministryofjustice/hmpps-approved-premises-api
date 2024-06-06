package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.SarRepository

@Service
class SarService(
  val objectMapper: ObjectMapper,
  val sarRepository: SarRepository,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun getSarResult(crn: String): String {
    val applicationJson = sarRepository.getApplicationsJson(crn)

    val result = """
      {
        "applications": $applicationJson
      }
    """.trimIndent()

    if (log.isDebugEnabled) {
      val prettyPrintJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
        objectMapper.readValue(result, Any::class.java),
      )
      log.debug("SAR result is $prettyPrintJson")
    }

    return result
  }
}
