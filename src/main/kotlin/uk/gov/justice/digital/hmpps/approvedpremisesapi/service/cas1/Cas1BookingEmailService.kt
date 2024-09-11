package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Constants.DAYS_IN_WEEK
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilInclusive

object Constants {
  const val DAYS_IN_WEEK = 7
}

@Service
class Cas1BookingEmailService(
  private val emailNotifier: Cas1EmailNotifier,
  private val notifyConfig: NotifyConfig,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.application-timeline}") private val applicationTimelineUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.booking}") private val bookingUrlTemplate: UrlTemplate,
) {

  fun bookingMade(
    application: ApprovedPremisesApplicationEntity,
    booking: BookingEntity,
    placementApplication: PlacementApplicationEntity?,
  ) {
    val applicationSubmittedByUser = application.createdByUser

    val emailPersonalisation = buildCommonPersonalisation(
      application,
      booking,
    )

    val applicants = setOfNotNull(
      applicationSubmittedByUser.email,
      placementApplication?.createdByUser?.email,
    )

    emailNotifier.sendEmails(
      recipientEmailAddresses = applicants,
      templateId = notifyConfig.templates.bookingMade,
      personalisation = emailPersonalisation,
      application = application,
    )

    if (booking.premises.emailAddress != null) {
      emailNotifier.sendEmail(
        recipientEmailAddress = booking.premises.emailAddress!!,
        templateId = notifyConfig.templates.bookingMadePremises,
        personalisation = emailPersonalisation,
        application = application,
      )
    }
  }

  fun bookingWithdrawn(
    application: ApprovedPremisesApplicationEntity,
    booking: BookingEntity,
    placementApplication: PlacementApplicationEntity?,
    withdrawalTriggeredBy: WithdrawalTriggeredBy,
  ) {
    val allPersonalisation =
      buildCommonPersonalisation(application, booking).toMutableMap()

    allPersonalisation += "region" to booking.premises.probationRegion.name
    allPersonalisation["withdrawnBy"] = withdrawalTriggeredBy.getName()

    val interestedParties =
      (
        application.interestedPartiesEmailAddresses() +
          setOfNotNull(placementApplication?.createdByUser?.email) +
          setOfNotNull(booking.premises.emailAddress) +
          setOfNotNull(application.apArea?.emailAddress)
        ).toSet()

    emailNotifier.sendEmails(
      recipientEmailAddresses = interestedParties,
      templateId = notifyConfig.templates.bookingWithdrawnV2,
      personalisation = allPersonalisation,
      application = application,
    )
  }

  fun buildCommonPersonalisation(application: ApplicationEntity, booking: BookingEntity): Map<String, Any> {
    val lengthOfStayDays = booking.arrivalDate.getDaysUntilInclusive(booking.departureDate).size
    val lengthOfStayWeeks = lengthOfStayDays.toDouble() / DAYS_IN_WEEK
    val lengthOfStayWeeksWholeNumber = (lengthOfStayDays.toDouble() % DAYS_IN_WEEK) == 0.0

    return mapOf(
      "apName" to booking.premises.name,
      "applicationUrl" to applicationUrlTemplate.resolve("id", application.id.toString()),
      "applicationTimelineUrl" to applicationTimelineUrlTemplate.resolve("applicationId", application.id.toString()),
      "bookingUrl" to bookingUrlTemplate.resolve(
        mapOf(
          "premisesId" to booking.premises.id.toString(),
          "bookingId" to booking.id.toString(),
        ),
      ),
      "crn" to application.crn,
      "startDate" to booking.arrivalDate.toString(),
      "endDate" to booking.departureDate.toString(),
      "lengthStay" to if (lengthOfStayWeeksWholeNumber) lengthOfStayWeeks.toInt() else lengthOfStayDays,
      "lengthStayUnit" to if (lengthOfStayWeeksWholeNumber) "weeks" else "days",
    )
  }
}
