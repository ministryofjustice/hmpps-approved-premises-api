package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotifier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate

@Service
class Cas1PlacementRequestEmailService(
  private val emailNotifier: EmailNotifier,
  private val notifyConfig: NotifyConfig,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.application-timeline}") private val applicationTimelineUrlTemplate: UrlTemplate,
  @Value("\${feature-flags.cas1-aps530-withdrawal-email-improvements}") private val aps530WithdrawalEmailImprovements: Boolean,
) {
  fun placementRequestWithdrawn(
    placementRequest: PlacementRequestEntity,
    withdrawingUser: UserEntity?,
  ) {
    val application = placementRequest.application

    val personalisation = mutableMapOf(
      "crn" to application.crn,
      "applicationUrl" to applicationUrlTemplate.resolve("id", application.id.toString()),
      "applicationTimelineUrl" to applicationTimelineUrlTemplate.resolve("applicationId", application.id.toString()),
      "applicationArea" to application.apArea?.name,
      "startDate" to placementRequest.expectedArrival.toString(),
      "endDate" to placementRequest.expectedDeparture().toString(),
      "additionalDatesSet" to "no",
    )

    if (withdrawingUser != null) {
      personalisation["withdrawnBy"] = withdrawingUser.name
    }

    if (placementRequest.isForApplicationsArrivalDate()) {
      val template = if (aps530WithdrawalEmailImprovements) {
        notifyConfig.templates.placementRequestWithdrawnV2
      } else {
        notifyConfig.templates.placementRequestWithdrawn
      }

      val applicant = application.createdByUser
      applicant.email?.let { applicantEmail ->
        emailNotifier.sendEmail(
          recipientEmailAddress = applicantEmail,
          /**
           * For information on why we send a request for placement email
           * instead of match request, see [PlacementRequestEntity.isForApplicationsArrivalDate]
           **/
          templateId = template,
          personalisation = personalisation,
        )
      }
    }

    if (!placementRequest.hasActiveBooking()) {
      val template = if (aps530WithdrawalEmailImprovements) {
        notifyConfig.templates.matchRequestWithdrawnV2
      } else {
        notifyConfig.templates.matchRequestWithdrawn
      }

      val area = application.apArea
      area?.emailAddress?.let { cruEmail ->
        emailNotifier.sendEmail(
          recipientEmailAddress = cruEmail,
          templateId = template,
          personalisation = personalisation,
        )
      }
    }
  }
}
