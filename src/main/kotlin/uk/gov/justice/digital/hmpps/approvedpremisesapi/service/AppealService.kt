package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AssessmentAppealed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AssessmentAppealedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AppealDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AppealDecision as DomainEventApiAppealDecision

@Service
class AppealService(
  private val appealRepository: AppealRepository,
  private val assessmentService: AssessmentService,
  private val domainEventService: DomainEventService,
  private val communityApiClient: CommunityApiClient,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.application-appeal}") private val applicationAppealUrlTemplate: UrlTemplate,
) {
  fun getAppeal(appealId: UUID, application: ApplicationEntity): AuthorisableActionResult<AppealEntity> {
    val appeal = appealRepository.findByIdOrNull(appealId)
      ?: return AuthorisableActionResult.NotFound("Appeal", appealId.toString())

    if (appeal.application.id != application.id) return AuthorisableActionResult.NotFound("Appeal", appealId.toString())

    return AuthorisableActionResult.Success(appeal)
  }

  fun createAppeal(
    appealDate: LocalDate,
    appealDetail: String,
    reviewer: String,
    decision: AppealDecision,
    decisionDetail: String,
    application: ApplicationEntity,
    assessment: AssessmentEntity,
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
            assessment = assessment,
            createdBy = createdBy,
          ),
        )

        saveEvent(appeal)

        if (decision == AppealDecision.accepted) {
          assessmentService.createApprovedPremisesAssessment(application as ApprovedPremisesApplicationEntity, createdFromAppeal = true)
        }

        success(appeal)
      },
    )
  }

  private fun saveEvent(appeal: AppealEntity) {
    val id = UUID.randomUUID()
    val timestamp = appeal.createdAt.toInstant()

    val staffDetails = when (val result = communityApiClient.getStaffUserDetails(appeal.createdBy.deliusUsername)) {
      is ClientResult.Success -> result.body
      is ClientResult.Failure -> result.throwException()
    }

    domainEventService.saveAssessmentAppealedEvent(
      DomainEvent(
        id = id,
        applicationId = appeal.application.id,
        assessmentId = null,
        bookingId = null,
        crn = appeal.application.crn,
        occurredAt = timestamp,
        data = AssessmentAppealedEnvelope(
          id = id,
          timestamp = timestamp,
          eventType = "approved-premises.assessment.appealed",
          eventDetails = AssessmentAppealed(
            applicationId = appeal.application.id,
            applicationUrl = applicationUrlTemplate.resolve("id", appeal.application.id.toString()),
            appealId = appeal.id,
            appealUrl = applicationAppealUrlTemplate.resolve(
              mapOf("applicationId" to appeal.application.id.toString(), "appealId" to appeal.id.toString()),
            ),
            personReference = PersonReference(
              crn = appeal.application.crn,
              noms = appeal.application.nomsNumber!!,
            ),
            deliusEventNumber = (appeal.application as ApprovedPremisesApplicationEntity).eventNumber,
            createdAt = timestamp,
            createdBy = StaffMember(
              staffCode = staffDetails.staffCode,
              staffIdentifier = staffDetails.staffIdentifier,
              forenames = staffDetails.staff.forenames,
              surname = staffDetails.staff.surname,
              username = staffDetails.username,
            ),
            reviewer = appeal.reviewer,
            appealDetail = appeal.appealDetail,
            decision = parseDecision(appeal.decision),
            decisionDetail = appeal.decisionDetail,
          ),
        ),
      ),
    )
  }

  private fun parseDecision(value: String): DomainEventApiAppealDecision =
    DomainEventApiAppealDecision.entries.firstOrNull { it.value == value }
      ?: throw IllegalArgumentException("Unknown appeal decision type '$value'")
}
