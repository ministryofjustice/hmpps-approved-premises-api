package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1AssessmentRejectionReasonDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService.Companion.FEATURE_FLAG_ISR_CAS1_EMAIL_CHANGES
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toUiFormat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toUiFormattedHourOfDay
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class Cas1AssessmentEmailService(
  private val emailNotifier: Cas1EmailNotifier,
  private val workingDayService: WorkingDayService,
  private val featureFlagService: FeatureFlagService,
  @Value("\${url-templates.frontend.assessment}") private val assessmentUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.application-timeline}") private val applicationTimelineUrlTemplate: UrlTemplate,
  @Value("\${services.cas2v2-ui.base-url}") private val cas2Url: String,
) {

  fun assessmentAllocated(
    allocatedUser: UserEntity,
    assessmentId: UUID,
    application: ApprovedPremisesApplicationEntity,
    deadline: OffsetDateTime?,
    isEmergency: Boolean,
  ) {
    allocatedUser.email?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = Cas1NotifyTemplates.ASSESSMENT_ALLOCATED,
        personalisation = mapOf(
          "name" to allocatedUser.name,
          "assessmentUrl" to assessmentUrlTemplate.resolve("id", assessmentId.toString()),
          "crn" to application.crn,
          "deadlineCopy" to deadlineCopy(deadline, isEmergency),
        ),
        application = application,
      )
    }
  }

  fun assessmentDeallocated(
    deallocatedUserEntity: UserEntity,
    assessmentId: UUID,
    application: ApprovedPremisesApplicationEntity,
  ) {
    deallocatedUserEntity.email?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = Cas1NotifyTemplates.ASSESSMENT_DEALLOCATED,
        personalisation = mapOf(
          "name" to deallocatedUserEntity.name,
          "assessmentUrl" to assessmentUrlTemplate.resolve("id", assessmentId.toString()),
          "crn" to application.crn,
        ),
        application = application,
      )
    }
  }

  fun assessmentAccepted(application: ApprovedPremisesApplicationEntity) {
    application.createdByUser.email?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = Cas1NotifyTemplates.ASSESSMENT_ACCEPTED,
        personalisation = mapOf(
          "name" to application.createdByUser.name,
          "applicationUrl" to applicationUrlTemplate.resolve("id", application.id.toString()),
          "crn" to application.crn,
        ),
        application = application,
      )
    }
  }

  fun assessmentRejected(
    application: ApprovedPremisesApplicationEntity,
    rejectionReason: Cas1AssessmentRejectionReasonDto? = null,
  ) {
    application.createdByUser.email?.let { email ->
      val timeApplicationReceived = application.submittedAt?.format(DateTimeFormatter.ofPattern("HH:mm"))
      val dateApplicationReceived = application.submittedAt?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = getAssessmentRejectionTemplate(rejectionReason),
        personalisation = mapOf(
          "name" to application.createdByUser.name,
          "applicationUrl" to applicationUrlTemplate.resolve("id", application.id.toString()),
          "crn" to application.crn,
          "timeApplicationReceived" to timeApplicationReceived,
          "dateApplicationReceived" to dateApplicationReceived,
          "cas2Url" to "$cas2Url?referred_by=cas1_app_rejected_email",
        ),
        application = application,
      )
    }
  }

  private fun getAssessmentRejectionTemplate(rejectionReason: Cas1AssessmentRejectionReasonDto?): String {
    val useAlternativeAccommodationTemplate =
      isAlternativeAccommodationRejectionReason(rejectionReason) &&
        featureFlagService.getBooleanFlag(FEATURE_FLAG_ISR_CAS1_EMAIL_CHANGES)

    return if (useAlternativeAccommodationTemplate) {
      Cas1NotifyTemplates.ASSESSMENT_REJECTED_ALTERNATIVE_ACCOMMODATION
    } else {
      Cas1NotifyTemplates.ASSESSMENT_REJECTED
    }
  }

  private fun isAlternativeAccommodationRejectionReason(rejectionReason: Cas1AssessmentRejectionReasonDto?) = rejectionReason in setOf(
    Cas1AssessmentRejectionReasonDto.accommodationNeedOnly,
    Cas1AssessmentRejectionReasonDto.notNecessaryOrProportionate,
    Cas1AssessmentRejectionReasonDto.riskCanBeManagedOtherWay,
  )

  fun appealedAssessmentAllocated(allocatedUser: UserEntity, assessmentId: UUID, application: ApprovedPremisesApplicationEntity) {
    allocatedUser.email?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = Cas1NotifyTemplates.APPEALED_ASSESSMENT_ALLOCATED,
        personalisation = mapOf(
          "name" to allocatedUser.name,
          "assessmentUrl" to assessmentUrlTemplate.resolve("id", assessmentId.toString()),
          "crn" to application.crn,
        ),
        application = application,
      )
    }
  }

  fun assessmentWithdrawn(
    assessment: ApprovedPremisesAssessmentEntity,
    application: ApprovedPremisesApplicationEntity,
    isAssessmentPending: Boolean,
    withdrawingUser: UserEntity,
  ) {
    if (isAssessmentPending) {
      assessment.allocatedToUser?.email?.let { email ->
        emailNotifier.sendEmail(
          recipientEmailAddress = email,
          templateId = Cas1NotifyTemplates.ASSESSMENT_WITHDRAWN_V2,
          personalisation = mapOf(
            "applicationUrl" to applicationUrlTemplate.resolve("id", assessment.application.id.toString()),
            "applicationTimelineUrl" to applicationTimelineUrlTemplate.resolve("applicationId", assessment.application.id.toString()),
            "crn" to application.crn,
            "withdrawnBy" to withdrawingUser.name,
          ),
          application = application,
        )
      }
    }
  }

  /*
  This is partially duplicating logic from TaskDeadlineService. This service
  should be told the type of deadline, not try and figure it out given the
  current date
   */
  private fun deadlineCopy(deadline: OffsetDateTime?, isEmergency: Boolean): String {
    if (deadline == null) {
      return DEFAULT_DEADLINE_COPY
    }

    if (isEmergency) {
      val deadlineDate = deadline.toLocalDate()
      return if (deadlineDate.isEqual(LocalDate.now())) {
        SAME_DAY_EMERGENCY_DEADLINE_COPY
      } else {
        NEXT_WORKING_DAY_EMERGENCY_DEADLINE_COPY.format(deadline.toUiFormattedHourOfDay(), deadlineDate.toUiFormat())
      }
    }

    return STANDARD_DEADLINE_COPY.format(
      workingDayService.getCompleteWorkingDaysFromNowUntil(deadline.toLocalDate()).toString(),
    )
  }

  companion object {
    val DEFAULT_DEADLINE_COPY = """
        You have 10 working days to complete the assessment, including any requests for further information. If the arrival date is within 28 days the assessment will need to be completed sooner. 
    """.trimIndent()
    val SAME_DAY_EMERGENCY_DEADLINE_COPY = """
        As this assessment is an emergency assessment, you have 2 hours to complete the assessment, including any requests for further information.
    """.trimIndent()
    val NEXT_WORKING_DAY_EMERGENCY_DEADLINE_COPY = """
      As this assessment is an emergency assessment, you have until %1${'$'}s on %2${'$'}s to complete the assessment, including any requests for further information.
    """.trimIndent()
    val STANDARD_DEADLINE_COPY = """
      You have %1${'$'}s working days to complete the assessment, including any requests for further information.
    """.trimIndent()
  }
}
