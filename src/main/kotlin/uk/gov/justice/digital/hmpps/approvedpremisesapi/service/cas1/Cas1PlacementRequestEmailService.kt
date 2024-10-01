package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate

@Service
class Cas1PlacementRequestEmailService(
  private val emailNotifier: Cas1EmailNotifier,
  private val notifyConfig: NotifyConfig,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.application-timeline}") private val applicationTimelineUrlTemplate: UrlTemplate,
) {

  fun placementRequestSubmitted(
    application: ApprovedPremisesApplicationEntity,
  ) {
    application.createdByUser.email?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = notifyConfig.templates.placementRequestSubmitted,
        personalisation = mapOf(
          "crn" to application.crn,
        ),
        application = application,
      )
    }
  }

  fun placementRequestWithdrawn(
    placementRequest: PlacementRequestEntity,
    withdrawalTriggeredBy: WithdrawalTriggeredBy,
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
      "withdrawnBy" to withdrawalTriggeredBy.getName(),
    )

    if (placementRequest.isForApplicationsArrivalDate()) {
      /**
       * For information on why we send a request for placement email
       * instead of match request, see [PlacementRequestEntity.isForApplicationsArrivalDate]
       **/
      emailNotifier.sendEmails(
        recipientEmailAddresses = application.interestedPartiesEmailAddresses(),
        templateId = notifyConfig.templates.placementRequestWithdrawnV2,
        personalisation = personalisation,
        application = application,
      )
    }

    if (!placementRequest.hasActiveBooking()) {
      val area = application.cruManagementArea
      area?.emailAddress?.let { cruEmail ->
        emailNotifier.sendEmail(
          recipientEmailAddress = cruEmail,
          templateId = notifyConfig.templates.matchRequestWithdrawnV2,
          personalisation = personalisation,
          application = application,
        )
      }
    }
  }
}
