package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AppealDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validatedCasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.AssessmentService
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas1AppealService(
  private val appealRepository: AppealRepository,
  private val assessmentService: AssessmentService,
  private val cas1AppealEmailService: Cas1AppealEmailService,
  private val cas1AppealDomainEventService: Cas1AppealDomainEventService,
) {
  fun getAppeal(appealId: UUID, application: ApplicationEntity): AuthorisableActionResult<AppealEntity> {
    val appeal = appealRepository.findByIdOrNull(appealId)
      ?: return AuthorisableActionResult.NotFound("Appeal", appealId.toString())

    if (appeal.application.id != application.id) return AuthorisableActionResult.NotFound("Appeal", appealId.toString())

    return AuthorisableActionResult.Success(appeal)
  }

  @Transactional
  fun createAppeal(
    appealDate: LocalDate,
    appealDetail: String,
    decision: AppealDecision,
    decisionDetail: String,
    application: ApprovedPremisesApplicationEntity,
    assessment: AssessmentEntity,
    createdBy: UserEntity,
  ): CasResult<AppealEntity> {
    if (!createdBy.hasRole(UserRole.CAS1_APPEALS_MANAGER)) return CasResult.Unauthorised()

    return validatedCasResult {
      if (application.status != ApprovedPremisesApplicationStatus.REJECTED) {
        return generalError("Appeals can only be created for rejected applications")
      }

      if (appealDate.isAfter(LocalDate.now())) {
        "$.appealDate" hasValidationError "mustNotBeFuture"
      }

      if (appealDetail.isBlank()) {
        "$.appealDetail" hasValidationError "empty"
      }

      if (decisionDetail.isBlank()) {
        "$.decisionDetail" hasValidationError "empty"
      }

      if (validationErrors.any()) {
        return@validatedCasResult fieldValidationError
      }

      val appeal = appealRepository.save(
        AppealEntity(
          id = UUID.randomUUID(),
          appealDate = appealDate,
          appealDetail = appealDetail,
          decision = decision.value,
          decisionDetail = decisionDetail,
          createdAt = OffsetDateTime.now(),
          application = application,
          assessment = assessment,
          createdBy = createdBy,
        ),
      )

      cas1AppealDomainEventService.appealRecordCreated(appeal)

      when (decision) {
        AppealDecision.accepted -> {
          assessmentService.createApprovedPremisesAssessment(application as ApprovedPremisesApplicationEntity, createdFromAppeal = true)
          cas1AppealEmailService.appealSuccess(application, appeal)
        }
        AppealDecision.rejected -> {
          cas1AppealEmailService.appealFailed(application as ApprovedPremisesApplicationEntity)
        }
      }

      success(appeal)
    }
  }
}
