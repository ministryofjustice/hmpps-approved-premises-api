package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toUiFormat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toUiFormattedHourOfDay
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas1AssessmentEmailService(
  private val emailNotifier: Cas1EmailNotifier,
  private val notifyConfig: NotifyConfig,
  private val workingDayService: WorkingDayService,
  @Value("\${url-templates.frontend.assessment}") private val assessmentUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.application-timeline}") private val applicationTimelineUrlTemplate: UrlTemplate,
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
        templateId = notifyConfig.templates.assessmentAllocated,
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
        templateId = notifyConfig.templates.assessmentDeallocated,
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
        templateId = notifyConfig.templates.assessmentAccepted,
        personalisation = mapOf(
          "name" to application.createdByUser.name,
          "applicationUrl" to applicationUrlTemplate.resolve("id", application.id.toString()),
          "crn" to application.crn,
        ),
        application = application,
      )
    }
  }

  fun assessmentRejected(application: ApprovedPremisesApplicationEntity) {
    application.createdByUser.email?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = notifyConfig.templates.assessmentRejected,
        personalisation = mapOf(
          "name" to application.createdByUser.name,
          "applicationUrl" to applicationUrlTemplate.resolve("id", application.id.toString()),
          "crn" to application.crn,
        ),
        application = application,
      )
    }
  }

  fun appealedAssessmentAllocated(allocatedUser: UserEntity, assessmentId: UUID, application: ApprovedPremisesApplicationEntity) {
    allocatedUser.email?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = notifyConfig.templates.appealedAssessmentAllocated,
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
          templateId = notifyConfig.templates.assessmentWithdrawnV2,
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
