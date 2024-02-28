package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotifier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.util.UUID

@Service
class Cas1AssessmentEmailService(
  private val emailNotificationService: EmailNotifier,
  private val notifyConfig: NotifyConfig,
  @Value("\${url-templates.frontend.assessment}") private val assessmentUrlTemplate: UrlTemplate,
) {

  fun assessmentAllocated(allocatedUser: UserEntity, assessmentId: UUID, crn: String) {
    allocatedUser.email?.let { email ->
      emailNotificationService.sendEmail(
        recipientEmailAddress = email,
        templateId = notifyConfig.templates.assessmentAllocated,
        personalisation = mapOf(
          "name" to allocatedUser.name,
          "assessmentUrl" to assessmentUrlTemplate.resolve("id", assessmentId.toString()),
          "crn" to crn,
        ),
      )
    }
  }

  fun assessmentDeallocated(deallocatedUserEntity: UserEntity, assessmentId: UUID, crn: String) {
    deallocatedUserEntity.email?.let { email ->
      emailNotificationService.sendEmail(
        recipientEmailAddress = email,
        templateId = notifyConfig.templates.assessmentDeallocated,
        personalisation = mapOf(
          "name" to deallocatedUserEntity.name,
          "assessmentUrl" to assessmentUrlTemplate.resolve("id", assessmentId.toString()),
          "crn" to crn,
        ),
      )
    }
  }
}
