package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate

@Service
class Cas1PlacementRequestEmailService(
  private val emailNotifier: Cas1EmailNotifier,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.application-timeline}") private val applicationTimelineUrlTemplate: UrlTemplate,
) {

  /**
   * This is only sent if the placement request relates to the 'initial'
   * request for placement. Because we now create a placement application
   * 'automatic' for these requests, we should delegate sending this
   * email to the `PlacementApplicationService' (which _does_ send this
   * email for other types of placement applications on submission). Note
   * that for those other types the PLACEMENT_REQUEST_SUBMITTED_V2 template
   * is used.
   */
  fun placementRequestSubmitted(
    application: ApprovedPremisesApplicationEntity,
  ) {
    application.createdByUser.email?.let { email ->
      emailNotifier.sendEmail(
        recipientEmailAddress = email,
        templateId = Cas1NotifyTemplates.PLACEMENT_REQUEST_SUBMITTED,
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
        templateId = Cas1NotifyTemplates.PLACEMENT_REQUEST_WITHDRAWN_V2,
        personalisation = personalisation,
        application = application,
      )
    }

    if (!placementRequest.hasActiveBooking()) {
      val area = application.cruManagementArea
      area?.emailAddress?.let { cruEmail ->
        emailNotifier.sendEmail(
          recipientEmailAddress = cruEmail,
          templateId = Cas1NotifyTemplates.MATCH_REQUEST_WITHDRAWN_V2,
          personalisation = personalisation,
          application = application,
        )
      }
    }
  }
}
