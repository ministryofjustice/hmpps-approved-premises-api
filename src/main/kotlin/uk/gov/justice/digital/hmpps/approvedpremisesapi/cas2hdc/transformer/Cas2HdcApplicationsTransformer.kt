package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcReferralHistory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2StatusUpdateNonAssignable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service.Cas2HdcUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OffenderManagementUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import java.util.UUID

@Component()
class Cas2HdcApplicationsTransformer(
  private val jsonMapper: JsonMapper,
  private val personTransformer: PersonTransformer,
  private val cas2HdcNomisUserTransformer: Cas2HdcNomisUserTransformer,
  private val cas2HdcStatusUpdateTransformer: Cas2HdcStatusUpdateTransformer,
  private val cas2HdcTimelineEventsTransformer: Cas2HdcTimelineEventsTransformer,
  private val cas2HdcAssessmentsTransformer: Cas2HdcAssessmentsTransformer,
  private val cas2HdcUserService: Cas2HdcUserService,
  private val offenderManagementUnitRepository: OffenderManagementUnitRepository,
  @Value($$"${url-templates.frontend.cas2.application}") private val cas2ApplicationUrl: String,

) {

  fun transformJpaToApi(jpa: Cas2ApplicationEntity, personInfo: PersonInfoResult): Cas2HdcApplication {
    val currentUser = jpa.currentPomUserId?.let { cas2HdcUserService.getNomisUserById(jpa.currentPomUserId!!, jpa.serviceOrigin) }
    val omu = jpa.currentPrisonCode?.let { offenderManagementUnitRepository.findByPrisonCode(it) }
    return Cas2HdcApplication(
      id = jpa.id,
      person = personTransformer.transformModelToPersonApi(personInfo),
      createdBy = cas2HdcNomisUserTransformer.transformJpaToApi(jpa),
      createdAt = jpa.createdAt.toInstant(),
      submittedAt = jpa.submittedAt?.toInstant(),
      data = if (jpa.data != null) jsonMapper.readTree(jpa.data) else null,
      document = if (jpa.document != null) jsonMapper.readTree(jpa.document) else null,
      status = getStatus(jpa),
      type = "CAS2",
      telephoneNumber = jpa.telephoneNumber,
      assessment = if (jpa.assessment != null) cas2HdcAssessmentsTransformer.transformJpaToApiRepresentation(jpa.assessment!!) else null,
      timelineEvents = cas2HdcTimelineEventsTransformer.transformApplicationToTimelineEvents(jpa),
      allocatedPomName = currentUser?.name,
      allocatedPomEmailAddress = currentUser?.email,
      currentPrisonName = omu?.prisonName ?: jpa.currentPrisonCode,
      isTransferredApplication = jpa.isTransferredApplication(),
      assignmentDate = jpa.currentAssignmentDate,
      omuEmailAddress = omu?.email,
      applicationOrigin = jpa.applicationOrigin,
      bailHearingDate = jpa.bailHearingDate,
    )
  }

  fun transformJpaSummaryToSummary(
    jpaSummary: Cas2ApplicationSummaryEntity,
    personName: String,
  ): Cas2HdcApplicationSummary = Cas2HdcApplicationSummary(
    id = jpaSummary.id,
    createdByUserId = UUID.fromString(jpaSummary.userId),
    createdByUserName = jpaSummary.userName,
    // BAIL-WIP The two allocated POM fields are left unchanged as it will currently ALWAYS be a nomis user.
    allocatedPomUserId = jpaSummary.allocatedPomUserId ?: UUID.fromString(jpaSummary.userId),
    allocatedPomName = jpaSummary.allocatedPomName ?: jpaSummary.userName,
    currentPrisonName = jpaSummary.currentPrisonCode?.let { offenderManagementUnitRepository.findByPrisonCode(it)?.prisonName }
      ?: jpaSummary.currentPrisonCode,
    assignmentDate = jpaSummary.assignmentDate?.toLocalDate() ?: jpaSummary.createdAt.toLocalDate(),
    createdAt = jpaSummary.createdAt.toInstant(),
    submittedAt = jpaSummary.submittedAt?.toInstant(),
    status = getStatusFromSummary(jpaSummary),
    latestStatusUpdate = cas2HdcStatusUpdateTransformer.transformJpaSummaryToLatestStatusUpdateApi(jpaSummary),
    type = "CAS2",
    hdcEligibilityDate = jpaSummary.hdcEligibilityDate,
    crn = jpaSummary.crn,
    nomsNumber = jpaSummary.nomsNumber,
    personName = personName,
    applicationOrigin = jpaSummary.applicationOrigin?.let { ApplicationOrigin.forValue(it) },
    bailHearingDate = jpaSummary.bailHearingDate,
  )

  fun transformJpaToCas2HdcReferralHistory(
    jpa: Cas2ApplicationEntity,
  ): Cas2HdcReferralHistory {
    val latestStatusUpdate = getReferralHistoryStatus(jpa)
    val rejectionReason = latestStatusUpdate
      ?.takeIf { it.label in listOf(Cas2StatusUpdateNonAssignable.REFERRAL_CANCELLED.label, Cas2StatusUpdateNonAssignable.REFERRAL_WITHDRAWN.label) }
      ?.label

    val omu = jpa.referringPrisonCode?.let { offenderManagementUnitRepository.findByPrisonCode(it) }
    val placementAddress = omu?.prisonName ?: jpa.referringPrisonCode

    return Cas2HdcReferralHistory(
      id = jpa.assessment!!.id,
      applicationId = jpa.id,
      type = ServiceType.CAS2,
      createdAt = jpa.submittedAt!!.toInstant(),
      status = jpa.statusUpdates!!.first().label,
      referralRejectionReason = rejectionReason,
      localAuthorityArea = placementAddress,
      pdu = jpa.preferredAreas,
      referredBy = jpa.createdByUser.name,
      placementAddress = placementAddress,
      placementStatus = latestStatusUpdate?.label,
      referralUrl = cas2ApplicationUrl.replace("#id", jpa.id.toString()),
    )
  }

  private fun getReferralHistoryStatus(jpa: Cas2ApplicationEntity): Cas2StatusUpdateEntity? = jpa.statusUpdates?.firstOrNull()

  private fun getStatus(entity: Cas2ApplicationEntity): ApplicationStatus {
    if (entity.submittedAt !== null) {
      return ApplicationStatus.submitted
    }

    return ApplicationStatus.inProgress
  }

  private fun getStatusFromSummary(summary: Cas2ApplicationSummaryEntity): ApplicationStatus = when {
    summary.submittedAt != null -> ApplicationStatus.submitted
    else -> ApplicationStatus.inProgress
  }

  fun serviceOriginFromText(serviceOrigin: String): Cas2ServiceOrigin = when (serviceOrigin.uppercase()) {
    "HDC" -> Cas2ServiceOrigin.HDC
    "BAIL" -> Cas2ServiceOrigin.BAIL
    else -> error("Unexpected service origin value $serviceOrigin")
  }
}
