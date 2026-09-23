package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.external

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ExternalSubmittedApplicationDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity
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

      Cas2SuitableApplication(
        uiUrl = getUiUrl(mostRecent),
        id = mostRecent.id,
        submittedApplication = getSubmittedApplication(mostRecent),
      )
    }

  private fun getSubmittedApplication(mostRecent: Cas2ApplicationEntity) = if (mostRecent.submittedAt != null) {
    val latestAssessmentStatus = mostRecent
      .statusUpdates
      ?.firstOrNull()
      ?.assessmentStatus

    Cas2ExternalSubmittedApplicationDto(
      latestAssessmentStatus = latestAssessmentStatus,
      submittedAt = mostRecent.submittedAt!!,
    )
  } else {
    null
  }

  private fun getUiUrl(mostRecent: Cas2ApplicationEntity) = if (mostRecent.submittedAt != null) {
    submittedApplicationUrlTemplate.replace("#applicationId", mostRecent.id.toString())
  } else {
    applicationUrlTemplate.replace("#id", mostRecent.id.toString())
  }
}
