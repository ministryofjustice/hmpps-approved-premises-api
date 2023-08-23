package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OfflineApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSummary as ApiApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationSummary as ApiApprovedPremisesApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationSummary as ApiCas2ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplicationSummary as ApiTemporaryAccommodationApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSummary as DomainApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationSummary as DomainApprovedPremisesApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity as DomainCas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationSummary as DomainCas2ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity as DomainTemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationSummary as DomainTemporaryAccommodationApplicationSummary

@Component
class ApplicationsTransformer(
  private val objectMapper: ObjectMapper,
  private val personTransformer: PersonTransformer,
  private val risksTransformer: RisksTransformer,
) {
  fun transformJpaToApi(jpa: ApplicationEntity, personInfo: PersonInfoResult.Success): Application {
    val latestAssessment = jpa.getLatestAssessment()

    return when (jpa) {
      is ApprovedPremisesApplicationEntity -> ApprovedPremisesApplication(
        id = jpa.id,
        person = personTransformer.transformModelToPersonApi(personInfo),
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
        risks = if (jpa.riskRatings != null) {
          risksTransformer.transformDomainToApi(
            jpa.riskRatings!!,
            jpa.crn,
          )
        } else {
          null
        },
        status = getStatus(jpa, latestAssessment),
        assessmentDecision = transformJpaDecisionToApi(latestAssessment?.decision),
        type = "CAS1",
      )

      is DomainTemporaryAccommodationApplicationEntity -> TemporaryAccommodationApplication(
        id = jpa.id,
        person = personTransformer.transformModelToPersonApi(personInfo),
        createdByUserId = jpa.createdByUser.id,
        schemaVersion = jpa.schemaVersion.id,
        outdatedSchema = !jpa.schemaUpToDate,
        createdAt = jpa.createdAt.toInstant(),
        submittedAt = jpa.submittedAt?.toInstant(),
        data = if (jpa.data != null) objectMapper.readTree(jpa.data) else null,
        document = if (jpa.document != null) objectMapper.readTree(jpa.document) else null,
        risks = if (jpa.riskRatings != null) {
          risksTransformer.transformDomainToApi(
            jpa.riskRatings!!,
            jpa.crn,
          )
        } else {
          null
        },
        status = getStatus(jpa, latestAssessment),
        type = "CAS3",
      )

      is DomainCas2ApplicationEntity -> Cas2Application(
        id = jpa.id,
        person = personTransformer.transformModelToPersonApi(personInfo),
        createdByUserId = jpa.createdByUser.id,
        schemaVersion = jpa.schemaVersion.id,
        outdatedSchema = !jpa.schemaUpToDate,
        createdAt = jpa.createdAt.toInstant(),
        submittedAt = jpa.submittedAt?.toInstant(),
        data = if (jpa.data != null) objectMapper.readTree(jpa.data) else null,
        document = if (jpa.document != null) objectMapper.readTree(jpa.document) else null,
        risks = if (jpa.riskRatings != null) {
          risksTransformer.transformDomainToApi(
            jpa.riskRatings!!,
            jpa.crn,
          )
        } else {
          null
        },
        status = getStatus(jpa, latestAssessment),
        type = "CAS2",
      )

      else -> throw RuntimeException("Unrecognised application type when transforming: ${jpa::class.qualifiedName}")
    }
  }

  fun transformDomainToApiSummary(domain: DomainApplicationSummary, personInfo: PersonInfoResult.Success): ApiApplicationSummary = when (domain) {
    is DomainApprovedPremisesApplicationSummary -> {
      val riskRatings =
        if (domain.getRiskRatings() != null) objectMapper.readValue<PersonRisks>(domain.getRiskRatings()!!) else null

      ApiApprovedPremisesApplicationSummary(
        id = domain.getId(),
        person = personTransformer.transformModelToPersonApi(personInfo),
        createdByUserId = domain.getCreatedByUserId(),
        createdAt = domain.getCreatedAt().toInstant(),
        submittedAt = domain.getSubmittedAt()?.toInstant(),
        isWomensApplication = domain.getIsWomensApplication(),
        isPipeApplication = domain.getIsPipeApplication(),
        arrivalDate = domain.getArrivalDate()?.toInstant(),
        risks = if (riskRatings != null) risksTransformer.transformDomainToApi(riskRatings, domain.getCrn()) else null,
        status = getStatusFromSummary(domain),
        type = "CAS1",
      )
    }

    is DomainTemporaryAccommodationApplicationSummary -> {
      val riskRatings =
        if (domain.getRiskRatings() != null) objectMapper.readValue<PersonRisks>(domain.getRiskRatings()!!) else null

      ApiTemporaryAccommodationApplicationSummary(
        id = domain.getId(),
        person = personTransformer.transformModelToPersonApi(personInfo),
        createdByUserId = domain.getCreatedByUserId(),
        createdAt = domain.getCreatedAt().toInstant(),
        submittedAt = domain.getSubmittedAt()?.toInstant(),
        risks = if (riskRatings != null) risksTransformer.transformDomainToApi(riskRatings, domain.getCrn()) else null,
        status = getStatusFromSummary(domain),
        type = "CAS3",
      )
    }

    is DomainCas2ApplicationSummary -> {
      val riskRatings =
        if (domain.getRiskRatings() != null) objectMapper.readValue<PersonRisks>(domain.getRiskRatings()!!) else null

      ApiCas2ApplicationSummary(
        id = domain.getId(),
        person = personTransformer.transformModelToPersonApi(personInfo),
        createdByUserId = domain.getCreatedByUserId(),
        createdAt = domain.getCreatedAt().toInstant(),
        submittedAt = domain.getSubmittedAt()?.toInstant(),
        risks = if (riskRatings != null) risksTransformer.transformDomainToApi(riskRatings, domain.getCrn()) else null,
        status = getStatusFromSummary(domain),
        type = "CAS2",
      )
    }

    else -> throw RuntimeException("Unrecognised application type when transforming: ${domain::class.qualifiedName}")
  }

  fun transformJpaToApi(jpa: OfflineApplicationEntity, personInfo: PersonInfoResult.Success) = OfflineApplication(
    id = jpa.id,
    person = personTransformer.transformModelToPersonApi(personInfo),
    createdAt = jpa.createdAt.toInstant(),
    type = "Offline",
  )

  private fun getStatus(entity: ApplicationEntity, latestAssessment: AssessmentEntity?): ApplicationStatus {
    if (entity is ApprovedPremisesApplicationEntity) {
      return when {
        entity.isInapplicable == true -> ApplicationStatus.inapplicable
        entity.isWithdrawn == true -> ApplicationStatus.withdrawn
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

  private fun getStatusFromSummary(entity: DomainApplicationSummary): ApplicationStatus {
    if (entity is DomainApprovedPremisesApplicationSummary) {
      return when {
        entity.getSubmittedAt() != null && entity.getLatestAssessmentDecision() == AssessmentDecision.REJECTED -> ApplicationStatus.rejected
        entity.getSubmittedAt() != null && entity.getLatestAssessmentDecision() == AssessmentDecision.ACCEPTED && !entity.getHasPlacementRequest() -> ApplicationStatus.pending
        entity.getSubmittedAt() != null && entity.getLatestAssessmentDecision() == AssessmentDecision.ACCEPTED && !entity.getHasBooking() -> ApplicationStatus.awaitingPlacement
        entity.getSubmittedAt() != null && entity.getLatestAssessmentDecision() == AssessmentDecision.ACCEPTED && entity.getHasBooking() -> ApplicationStatus.placed
        entity.getLatestAssessmentHasClarificationNotesWithoutResponse() -> ApplicationStatus.requestedFurtherInformation
        entity.getSubmittedAt() !== null -> ApplicationStatus.submitted
        else -> ApplicationStatus.inProgress
      }
    }

    if (entity is DomainCas2ApplicationSummary) {
      return when {
        entity.getSubmittedAt() != null -> ApplicationStatus.submitted
        else -> ApplicationStatus.inProgress
      }
    }

    return when {
      entity.getLatestAssessmentHasClarificationNotesWithoutResponse() -> ApplicationStatus.requestedFurtherInformation
      entity.getSubmittedAt() !== null -> ApplicationStatus.submitted
      else -> ApplicationStatus.inProgress
    }
  }

  fun transformJpaDecisionToApi(decision: AssessmentDecision?) = when (decision) {
    AssessmentDecision.ACCEPTED -> uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision.accepted
    AssessmentDecision.REJECTED -> uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision.rejected
    null -> null
  }
}
