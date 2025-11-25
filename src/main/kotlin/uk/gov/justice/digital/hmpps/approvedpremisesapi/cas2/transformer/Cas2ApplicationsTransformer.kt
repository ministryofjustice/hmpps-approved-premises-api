package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ReferralHistory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service.Cas2UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OffenderManagementUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import java.util.UUID

@Component()
class Cas2ApplicationsTransformer(
  private val objectMapper: ObjectMapper,
  private val personTransformer: PersonTransformer,
  private val nomisUserTransformer: NomisUserTransformer,
  private val statusUpdateTransformer: StatusUpdateTransformer,
  private val timelineEventsTransformer: TimelineEventsTransformer,
  private val assessmentsTransformer: AssessmentsTransformer,
  private val cas2UserService: Cas2UserService,
  private val offenderManagementUnitRepository: OffenderManagementUnitRepository,
) {

  fun transformJpaToApi(jpa: Cas2ApplicationEntity, personInfo: PersonInfoResult): Cas2Application {
    val currentUser = jpa.currentPomUserId?.let { cas2UserService.getNomisUserById(jpa.currentPomUserId!!) }
    val omu = jpa.currentPrisonCode?.let { offenderManagementUnitRepository.findByPrisonCode(it) }
    return Cas2Application(
      id = jpa.id,
      person = personTransformer.transformModelToPersonApi(personInfo),
      createdBy = nomisUserTransformer.transformJpaToApi(jpa),
      createdAt = jpa.createdAt.toInstant(),
      submittedAt = jpa.submittedAt?.toInstant(),
      data = if (jpa.data != null) objectMapper.readTree(jpa.data) else null,
      document = if (jpa.document != null) objectMapper.readTree(jpa.document) else null,
      status = getStatus(jpa),
      type = "CAS2",
      telephoneNumber = jpa.telephoneNumber,
      assessment = if (jpa.assessment != null) assessmentsTransformer.transformJpaToApiRepresentation(jpa.assessment!!) else null,
      timelineEvents = timelineEventsTransformer.transformApplicationToTimelineEvents(jpa),
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
  ): Cas2ApplicationSummary = Cas2ApplicationSummary(
    id = jpaSummary.id,
    createdByUserId = UUID.fromString(jpaSummary.getCreatedById()),
    createdByUserName = jpaSummary.getCreatedByUsername(),
    // BAIL-WIP The two allocated POM fields are left unchanged as it will currently ALWAYS be a nomis user.
    allocatedPomUserId = jpaSummary.allocatedPomUserId ?: UUID.fromString(jpaSummary.userId),
    allocatedPomName = jpaSummary.allocatedPomName ?: jpaSummary.userName,
    currentPrisonName = jpaSummary.currentPrisonCode?.let { offenderManagementUnitRepository.findByPrisonCode(it)?.prisonName }
      ?: jpaSummary.currentPrisonCode,
    assignmentDate = jpaSummary.assignmentDate?.toLocalDate() ?: jpaSummary.createdAt.toLocalDate(),
    createdAt = jpaSummary.createdAt.toInstant(),
    submittedAt = jpaSummary.submittedAt?.toInstant(),
    status = getStatusFromSummary(jpaSummary),
    latestStatusUpdate = statusUpdateTransformer.transformJpaSummaryToLatestStatusUpdateApi(jpaSummary),
    type = "CAS2",
    hdcEligibilityDate = jpaSummary.hdcEligibilityDate,
    crn = jpaSummary.crn,
    nomsNumber = jpaSummary.nomsNumber,
    personName = personName,
    applicationOrigin = when (jpaSummary.applicationOrigin) {
      "courtBail" -> ApplicationOrigin.courtBail
      "prisonBail" -> ApplicationOrigin.prisonBail
      "homeDetentionCurfew" -> ApplicationOrigin.homeDetentionCurfew
      else -> error("Unexpected original value ${jpaSummary.applicationOrigin}")
    },
    bailHearingDate = jpaSummary.bailHearingDate,
  )

  fun transformJpaToCas2ReferralHistory(
    jpa: Cas2ApplicationEntity,
  ): Cas2ReferralHistory = Cas2ReferralHistory(
    id = jpa.assessment!!.id,
    applicationId = jpa.id,
    type = "CAS2",
    createdAt = jpa.submittedAt!!,
    status = jpa.statusUpdates!!.first().label,
  )

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
}
