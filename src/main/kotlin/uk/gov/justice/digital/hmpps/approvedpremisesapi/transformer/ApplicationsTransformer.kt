package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OfflineApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawableType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.asApiType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1ApplicationUserDetailsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSummary as ApiApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationStatus as ApiApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationSummary as ApiApprovedPremisesApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TemporaryAccommodationApplicationSummary as ApiTemporaryAccommodationApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationSummary as DomainApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationSummary as DomainApprovedPremisesApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationSummary as DomainCas2ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity as DomainTemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationSummary as DomainTemporaryAccommodationApplicationSummary

@Component
class ApplicationsTransformer(
  private val objectMapper: ObjectMapper,
  private val personTransformer: PersonTransformer,
  private val risksTransformer: RisksTransformer,
  private val apAreaTransformer: ApAreaTransformer,
  private val cas1ApplicationUserDetailsTransformer: Cas1ApplicationUserDetailsTransformer,
) {

  final val applicationStatuses = mapOf(
    ApiApprovedPremisesApplicationStatus.assesmentInProgress to ApprovedPremisesApplicationStatus.ASSESSMENT_IN_PROGRESS,
    ApiApprovedPremisesApplicationStatus.started to ApprovedPremisesApplicationStatus.STARTED,
    ApiApprovedPremisesApplicationStatus.submitted to ApprovedPremisesApplicationStatus.SUBMITTED,
    ApiApprovedPremisesApplicationStatus.rejected to ApprovedPremisesApplicationStatus.REJECTED,
    ApiApprovedPremisesApplicationStatus.awaitingAssesment to ApprovedPremisesApplicationStatus.AWAITING_ASSESSMENT,
    ApiApprovedPremisesApplicationStatus.unallocatedAssesment to ApprovedPremisesApplicationStatus.UNALLOCATED_ASSESSMENT,
    ApiApprovedPremisesApplicationStatus.awaitingPlacement to ApprovedPremisesApplicationStatus.AWAITING_PLACEMENT,
    ApiApprovedPremisesApplicationStatus.placementAllocated to ApprovedPremisesApplicationStatus.PLACEMENT_ALLOCATED,
    ApiApprovedPremisesApplicationStatus.inapplicable to ApprovedPremisesApplicationStatus.INAPPLICABLE,
    ApiApprovedPremisesApplicationStatus.withdrawn to ApprovedPremisesApplicationStatus.WITHDRAWN,
    ApiApprovedPremisesApplicationStatus.requestedFurtherInformation to ApprovedPremisesApplicationStatus.REQUESTED_FURTHER_INFORMATION,
    ApiApprovedPremisesApplicationStatus.pendingPlacementRequest to ApprovedPremisesApplicationStatus.PENDING_PLACEMENT_REQUEST,
  )

  val reversedStatusMap: Map<ApprovedPremisesApplicationStatus, ApiApprovedPremisesApplicationStatus> =
    this.applicationStatuses.entries.associateBy({ it.value }) { it.key }

  fun transformApiApprovedPremisesApplicationStatusToJpa(
    apiStatus: ApiApprovedPremisesApplicationStatus?,
  ): ApprovedPremisesApplicationStatus? = this.applicationStatuses[apiStatus]

  fun transformJpaToApi(jpa: ApplicationEntity, personInfo: PersonInfoResult): Application {
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
        status = this.reversedStatusMap[jpa.status] ?: ApiApprovedPremisesApplicationStatus.started,
        assessmentDecision = transformJpaDecisionToApi(latestAssessment?.decision),
        assessmentId = latestAssessment?.id,
        assessmentDecisionDate = latestAssessment?.submittedAt?.toLocalDate(),
        personStatusOnSubmission = personTransformer.inmateStatusToPersonInfoApiStatus(
          InmateStatus.entries.firstOrNull { it.name == jpa.inmateInOutStatusOnSubmission },
        ),
        type = "CAS1",
        apArea = jpa.apArea?.let { apAreaTransformer.transformJpaToApi(it) },
        applicantUserDetails = jpa.applicantUserDetails?.let { cas1ApplicationUserDetailsTransformer.transformJpaToApi(it) },
        caseManagerIsNotApplicant = jpa.caseManagerIsNotApplicant,
        caseManagerUserDetails = jpa.caseManagerUserDetails?.let { cas1ApplicationUserDetailsTransformer.transformJpaToApi(it) },
        apType = jpa.apType?.asApiType(),
      )

      is DomainTemporaryAccommodationApplicationEntity -> TemporaryAccommodationApplication(
        id = jpa.id,
        person = personTransformer.transformModelToPersonApi(personInfo),
        createdByUserId = jpa.createdByUser.id,
        schemaVersion = jpa.schemaVersion.id,
        outdatedSchema = !jpa.schemaUpToDate,
        createdAt = jpa.createdAt.toInstant(),
        submittedAt = jpa.submittedAt?.toInstant(),
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
        type = "CAS3",
        offenceId = jpa.offenceId,
      )

      else -> throw RuntimeException("Unrecognised application type when transforming: ${jpa::class.qualifiedName}")
    }
  }

  fun transformDomainToApiSummary(
    domain: DomainApplicationSummary,
    personInfo: PersonInfoResult,
  ): ApiApplicationSummary = when (domain) {
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
        tier = domain.getTier(),
        isWithdrawn = domain.getIsWithdrawn(),
        releaseType = domain.getReleaseType()?.let { ReleaseTypeOption.valueOf(it) },
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

    else -> throw RuntimeException("Unrecognised application type when transforming: ${domain::class.qualifiedName}")
  }

  fun transformJpaToApi(jpa: OfflineApplicationEntity, personInfo: PersonInfoResult) = OfflineApplication(
    id = jpa.id,
    person = personTransformer.transformModelToPersonApi(personInfo),
    createdAt = jpa.createdAt.toInstant(),
    type = "Offline",
  )

  fun transformToWithdrawable(jpa: ApplicationEntity) = Withdrawable(
    id = jpa.id,
    type = WithdrawableType.application,
    dates = emptyList(),
  )

  private fun getStatus(entity: ApplicationEntity, latestAssessment: AssessmentEntity?): ApplicationStatus {
    return when {
      latestAssessment?.clarificationNotes?.any { it.response == null } == true -> ApplicationStatus.requestedFurtherInformation
      entity.submittedAt !== null -> ApplicationStatus.submitted
      else -> ApplicationStatus.inProgress
    }
  }

  private fun getStatusFromSummary(entity: DomainCas2ApplicationSummary): ApplicationStatus {
    return when {
      entity.getSubmittedAt() != null -> ApplicationStatus.submitted
      else -> ApplicationStatus.inProgress
    }
  }

  private fun getStatusFromSummary(entity: DomainTemporaryAccommodationApplicationSummary): ApplicationStatus {
    return when {
      entity.getLatestAssessmentHasClarificationNotesWithoutResponse() -> ApplicationStatus.requestedFurtherInformation
      entity.getSubmittedAt() !== null -> ApplicationStatus.submitted
      else -> ApplicationStatus.inProgress
    }
  }

  private fun getStatusFromSummary(entity: DomainApprovedPremisesApplicationSummary): ApiApprovedPremisesApplicationStatus =
    this.reversedStatusMap[ApprovedPremisesApplicationStatus.valueOf(entity.getStatus())]
      ?: throw RuntimeException("Application ${entity.getId()} has no status")

  fun transformJpaDecisionToApi(decision: AssessmentDecision?) = when (decision) {
    AssessmentDecision.ACCEPTED -> uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision.accepted
    AssessmentDecision.REJECTED -> uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AssessmentDecision.rejected
    null -> null
  }
}
