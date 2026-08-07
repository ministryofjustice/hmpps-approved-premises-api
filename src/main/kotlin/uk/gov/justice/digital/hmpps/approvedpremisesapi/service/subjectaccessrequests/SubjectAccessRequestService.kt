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
    val approvedPremises = cas1SubjectAccessRequestService.getSarResult(crn, nomsNumber, startDate, endDate)
    val temporaryAccommodation = cas3SubjectAccessRequestService.getSarResult(crn, nomsNumber, startDate, endDate)
    val shortTermAccommodation = cas2HdcSubjectAccessRequestService.getSarResult(crn, nomsNumber, startDate, endDate)
    val bailAccommodation = cas2SubjectAccessRequestService.getSarResult(crn, nomsNumber, startDate, endDate)

    if (listOf(approvedPremises, temporaryAccommodation, shortTermAccommodation, bailAccommodation).all { it == null }) {
      return null
    }

    return """
      {
         "CAS1": ${ approvedPremises ?: "[]"},
         "CAS2-HDC": ${ shortTermAccommodation ?: "[]"},
         "CAS2": ${ bailAccommodation ?: "[]"},
         "CAS3": ${temporaryAccommodation ?: "[]"}
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
