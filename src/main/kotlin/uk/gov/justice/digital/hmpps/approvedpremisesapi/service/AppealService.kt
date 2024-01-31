package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AppealDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class AppealService(
  private val appealRepository: AppealRepository,
) {
  fun createAppeal(
    appealDate: LocalDate,
    appealDetail: String,
    reviewer: String,
    decision: AppealDecision,
    decisionDetail: String,
    application: ApplicationEntity,
    createdBy: UserEntity,
  ): AuthorisableActionResult<ValidatableActionResult<AppealEntity>> {
    if (!createdBy.hasRole(UserRole.CAS1_APPEALS_MANAGER)) return AuthorisableActionResult.Unauthorised()

    return AuthorisableActionResult.Success(
      validated {
        if (appealDate.isAfter(LocalDate.now())) {
          "$.appealDate" hasValidationError "mustNotBeFuture"
        }

        if (appealDetail.isBlank()) {
          "$.appealDetail" hasValidationError "empty"
        }

        if (reviewer.isBlank()) {
          "$.reviewer" hasValidationError "empty"
        }

        if (decisionDetail.isBlank()) {
          "$.decisionDetail" hasValidationError "empty"
        }

        if (validationErrors.any()) {
          return@validated fieldValidationError
        }

        val appeal = appealRepository.save(
          AppealEntity(
            id = UUID.randomUUID(),
            appealDate = appealDate,
            appealDetail = appealDetail,
            reviewer = reviewer,
            decision = decision.value,
            decisionDetail = decisionDetail,
            createdAt = OffsetDateTime.now(),
            application = application,
            assessment = null,
            createdBy = createdBy,
          ),
        )

        success(appeal)
      },
    )
  }
}
