package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.external

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2AssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ExternalSubmittedApplicationDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2StaffDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2SuitableApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2UserTypeDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2Cohort
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserType

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
        createdAt = mostRecent.createdAt,
        cohort = mostRecent.cohort?.apiType,
        createdBy = Cas2StaffDto(
          username = mostRecent.createdByUser.username,
          deliusStaffCode = mostRecent.createdByUser.deliusStaffCode,
          name = mostRecent.createdByUser.name,
          nomisStaffId = mostRecent.createdByUser.nomisStaffId,
          userType = when (mostRecent.createdByUser.userType) {
            Cas2UserType.DELIUS -> Cas2UserTypeDto.DELIUS
            Cas2UserType.NOMIS -> Cas2UserTypeDto.NOMIS
            Cas2UserType.EXTERNAL -> Cas2UserTypeDto.EXTERNAL
          },
        ),
      )
    }

  private fun getSubmittedApplication(mostRecent: Cas2ApplicationEntity) = if (mostRecent.submittedAt != null) {
    val statusUpdate = mostRecent
      .statusUpdates
      ?.firstOrNull()

    val offerDeclinedReason = if (statusUpdate?.status == Cas2AssessmentStatus.OFFER_DECLINED) {
      statusUpdate.statusUpdateDetails?.first()?.statusDetail(
        statusId = statusUpdate.statusId,
        service = ServiceName.cas2v2,
      )?.name
    } else {
      null
    }

    val cancelledReason = if (statusUpdate?.status == Cas2AssessmentStatus.CANCELLED) {
      statusUpdate.statusUpdateDetails?.first()?.statusDetail(
        statusId = statusUpdate.statusId,
        service = ServiceName.cas2v2,
      )?.name
    } else {
      null
    }

    Cas2ExternalSubmittedApplicationDto(
      latestAssessmentStatus = statusUpdate?.status?.apiName,
      submittedAt = mostRecent.submittedAt!!,
      offerDeclinedReason = offerDeclinedReason,
      cancelledReason = cancelledReason,
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
