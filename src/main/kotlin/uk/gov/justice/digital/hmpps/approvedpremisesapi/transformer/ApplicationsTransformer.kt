package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OfflineApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail

@Component
class ApplicationsTransformer(
  private val objectMapper: ObjectMapper,
  private val personTransformer: PersonTransformer,
  private val risksTransformer: RisksTransformer
) {
  fun transformJpaToApi(jpa: ApplicationEntity, offenderDetailSummary: OffenderDetailSummary, inmateDetail: InmateDetail): Application = when (jpa) {
    is ApprovedPremisesApplicationEntity -> ApprovedPremisesApplication(
      id = jpa.id,
      person = personTransformer.transformModelToApi(offenderDetailSummary, inmateDetail),
      createdByUserId = jpa.createdByUser.id,
      schemaVersion = jpa.schemaVersion.id,
      outdatedSchema = !jpa.schemaUpToDate,
      createdAt = jpa.createdAt.toInstant(),
      submittedAt = jpa.submittedAt?.toInstant(),
      isWomensApplication = jpa.isWomensApplication,
      isPipeApplication = jpa.isPipeApplication,
      arrivalDate = jpa.arrivalDate?.toInstant(),
      data = if (jpa.data != null) objectMapper.readTree(jpa.data) else null,
      document = if (jpa.document != null) objectMapper.readTree(jpa.document) else null,
      risks = if (jpa.riskRatings != null) risksTransformer.transformDomainToApi(jpa.riskRatings!!, jpa.crn) else null,
      status = getStatus(jpa)
    )
    is TemporaryAccommodationApplicationEntity -> TemporaryAccommodationApplication(
      id = jpa.id,
      person = personTransformer.transformModelToApi(offenderDetailSummary, inmateDetail),
      createdByUserId = jpa.createdByUser.id,
      schemaVersion = jpa.schemaVersion.id,
      outdatedSchema = !jpa.schemaUpToDate,
      createdAt = jpa.createdAt.toInstant(),
      submittedAt = jpa.submittedAt?.toInstant(),
      data = if (jpa.data != null) objectMapper.readTree(jpa.data) else null,
      document = if (jpa.document != null) objectMapper.readTree(jpa.document) else null,
      status = getStatus(jpa)
    )
    else -> throw RuntimeException("Unrecognised application type when transforming: ${jpa::class.qualifiedName}")
  }

  fun transformJpaToApi(jpa: OfflineApplicationEntity, offenderDetailSummary: OffenderDetailSummary, inmateDetail: InmateDetail) = OfflineApplication(
    id = jpa.id,
    person = personTransformer.transformModelToApi(offenderDetailSummary, inmateDetail),
    createdAt = jpa.createdAt.toInstant(),
    submittedAt = jpa.submittedAt.toInstant()
  )

  private fun getStatus(entity: ApplicationEntity): ApplicationStatus {
    val latestAssessment = entity.getLatestAssessment()

    if (entity is ApprovedPremisesApplicationEntity) {
      return when {
        latestAssessment?.submittedAt != null && latestAssessment.decision == AssessmentDecision.REJECTED -> ApplicationStatus.rejected
        latestAssessment?.submittedAt != null && latestAssessment.decision == AssessmentDecision.ACCEPTED && entity.getLatestPlacementRequest() == null -> ApplicationStatus.pending
        latestAssessment?.submittedAt != null && latestAssessment.decision == AssessmentDecision.ACCEPTED && entity.getLatestBooking() == null -> ApplicationStatus.awaitingPlacement
        latestAssessment?.submittedAt != null && latestAssessment.decision == AssessmentDecision.ACCEPTED && entity.getLatestBooking() != null -> ApplicationStatus.placed
        latestAssessment?.clarificationNotes?.any { it.response == null } == true -> ApplicationStatus.requestedFurtherInformation
        entity.submittedAt !== null -> ApplicationStatus.submitted
        else -> ApplicationStatus.inProgress
      }
    }

    return when {
      latestAssessment?.clarificationNotes?.any { it.response == null } == true -> ApplicationStatus.requestedFurtherInformation
      entity.submittedAt !== null -> ApplicationStatus.submitted
      else -> ApplicationStatus.inProgress
    }
  }
}
