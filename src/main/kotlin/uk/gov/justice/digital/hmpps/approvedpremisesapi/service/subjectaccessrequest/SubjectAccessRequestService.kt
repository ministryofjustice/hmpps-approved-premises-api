package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.subjectaccessrequest

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.SubjectAccessRequestRepository
import java.time.LocalDateTime

@Service
class SubjectAccessRequestService(
  val objectMapper: ObjectMapper,
  val subjectAccessRequestRepository: SubjectAccessRequestRepository,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun getSarResult(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String {
    val getApprovedPremisesApplicationsJson = subjectAccessRequestRepository.getApprovedPremisesApplicationsJson(crn, nomsNumber, startDate, endDate)
    val result = """
      {
        "approvedPremisesApplications": $getApprovedPremisesApplicationsJson
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
