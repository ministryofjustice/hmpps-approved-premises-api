package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2HdcSubjectAccessRequestRepository
import java.time.LocalDateTime

@Service
class Cas2HdcSubjectAccessRequestService(
  val cas2HdcSubjectAccessRequestRepository: Cas2HdcSubjectAccessRequestRepository,
) {

  fun getSarResult(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String? {
    val applicationsJson = cas2HdcSubjectAccessRequestRepository.getApplicationsJson(crn, nomsNumber, startDate, endDate, Cas2ServiceOrigin.HDC.toString())
    val applicationNotesJson =
      cas2HdcSubjectAccessRequestRepository.getApplicationNotes(crn, nomsNumber, startDate, endDate, Cas2ServiceOrigin.HDC.toString())
    val assessmentsJson = cas2HdcSubjectAccessRequestRepository.getAssessments(crn, nomsNumber, startDate, endDate, Cas2ServiceOrigin.HDC.toString())

    if (listOf(
        applicationsJson,
        applicationNotesJson,
        assessmentsJson,
      ).all { it == null }
    ) {
      return null
    }

    val result = """
      {
         "Applications": ${ applicationsJson ?: "[]"},
         "ApplicationNotes": ${ applicationNotesJson ?: "[]"},
         "Assessments": ${ assessmentsJson ?: "[]"},
      }
    """.trimIndent()

    return result
  }
}
