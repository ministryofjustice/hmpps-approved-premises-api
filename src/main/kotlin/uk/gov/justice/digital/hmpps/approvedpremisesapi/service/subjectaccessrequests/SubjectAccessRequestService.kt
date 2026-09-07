package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.subjectaccessrequests

import org.json.JSONObject
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.service.Cas1SubjectAccessRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2SubjectAccessRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcSubjectAccessRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.Cas3SubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonProbationSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
class SubjectAccessRequestService(
  val cas1SubjectAccessRequestService: Cas1SubjectAccessRequestService,
  val cas2SubjectAccessRequestService: Cas2SubjectAccessRequestService,
  val cas2HdcSubjectAccessRequestService: Cas2HdcSubjectAccessRequestService,
  val cas3SubjectAccessRequestService: Cas3SubjectAccessRequestService,
) : HmppsPrisonProbationSubjectAccessRequestService {

  fun getSarResult(crn: String?, nomsNumber: String?, startDate: LocalDateTime?, endDate: LocalDateTime?): String? {
    val cas1 = cas1SubjectAccessRequestService.getSarResult(crn, nomsNumber, startDate, endDate)
    val cas2hdc = cas2HdcSubjectAccessRequestService.getSarResult(crn, nomsNumber, startDate, endDate)
    val cas2 = cas2SubjectAccessRequestService.getSarResult(crn, nomsNumber, startDate, endDate)
    val cas3 = cas3SubjectAccessRequestService.getSarResult(crn, nomsNumber, startDate, endDate)

    if (listOfNotNull(cas1, cas3, cas2hdc, cas2).isEmpty()) {
      return null
    }

    return """
      {
         "CAS1": ${ cas1 ?: "[]"},
         "CAS2-HDC": ${ cas2hdc ?: "[]"},
         "CAS2": ${ cas2 ?: "[]"},
         "CAS3": ${ cas3 ?: "[]"}
      }
    """.trimIndent()
  }

  override fun getContentFor(
    prn: String?,
    crn: String?,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): HmppsSubjectAccessRequestContent? {
    val sarResults =
      getSarResult(
        crn,
        prn,
        fromDate?.atStartOfDay(),
        toDate?.atTime(LocalTime.MAX),
      )

    if (sarResults == null) return null

    return HmppsSubjectAccessRequestContent(
      content = JSONObject(sarResults)
        .toMap().entries,
    )
  }
}
