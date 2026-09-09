package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.external

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ExternalApplicationDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ExternalSubmittedApplicationDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2Cohort

@Service
class Cas2ExternalApplicationService(
  private val cas2ApplicationRepository: Cas2ApplicationRepository,
  @Value("\${url-templates.frontend.cas2v2.application}") private val applicationUrlTemplate: String,
  @Value("\${url-templates.frontend.cas2v2.submitted-application-overview}") private val submittedApplicationUrlTemplate: String,
) {

  fun getSuitableApplicationByCrn(crn: String): Cas2SuitableApplication? = cas2ApplicationRepository.findLatestApplication(crn, Cas2Cohort.isr())
    ?.let { mostRecent ->
      val uiUrl = if (mostRecent.submittedAt != null) {
        submittedApplicationUrlTemplate.replace("#applicationId", mostRecent.id.toString())
      } else {
        applicationUrlTemplate.replace("#id", mostRecent.id.toString())
      }
      Cas2SuitableApplication(
        uiUrl = uiUrl,
        application = Cas2ExternalApplicationDto(
          id = mostRecent.id,
          status = mostRecent.statusUpdates?.firstOrNull()?.label,
        ),
        id = mostRecent.id,
        submittedApplication = if (mostRecent.submittedAt != null) {
          Cas2ExternalSubmittedApplicationDto(
            latestAssessmentStatus = mostRecent.statusUpdates?.firstOrNull()?.label,
            submittedAt = mostRecent.submittedAt!!,
          )
        } else {
          null
        },
      )
    }
}
