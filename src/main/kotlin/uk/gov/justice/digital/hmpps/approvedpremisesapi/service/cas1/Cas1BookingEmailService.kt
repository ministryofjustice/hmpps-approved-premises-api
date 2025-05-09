package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas1NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate

object Constants {
  const val DAYS_IN_WEEK = 7
}

@Service
class Cas1BookingEmailService(
  private val emailNotifier: Cas1EmailNotifier,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.application-timeline}") private val applicationTimelineUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.booking}") private val bookingUrlTemplate: UrlTemplate,
) {

  fun spaceBookingMade(
    spaceBooking: Cas1SpaceBookingEntity,
    application: ApprovedPremisesApplicationEntity,
  ) = bookingMade(
    spaceBooking.toEmailBookingInfo(application),
  )

  fun bookingMade(
    application: ApprovedPremisesApplicationEntity,
    booking: BookingEntity,
    placementApplication: PlacementApplicationEntity?,
  ) = bookingMade(
    booking.toEmailBookingInfo(application, placementApplication),
  )

  private fun bookingMade(emailBookingInfo: EmailBookingInfo) {
    val application = emailBookingInfo.application
    val placementApplication = emailBookingInfo.placementApplication

    val emailPersonalisation = buildCommonPersonalisation(emailBookingInfo)

    val recipientEmailAddresses = (
      application.interestedPartiesEmailAddresses() +
        setOfNotNull(placementApplication?.createdByUser?.email)
      ).toSet()

    emailNotifier.sendEmails(
      recipientEmailAddresses = recipientEmailAddresses,
      templateId = Cas1NotifyTemplates.BOOKING_MADE,
      personalisation = emailPersonalisation,
      application = application,
    )

    if (emailBookingInfo.premises.emailAddress != null) {
      emailNotifier.sendEmail(
        recipientEmailAddress = emailBookingInfo.premises.emailAddress!!,
        templateId = Cas1NotifyTemplates.BOOKING_MADE_FOR_PREMISES,
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
  ) = bookingWithdrawn(
    booking.toEmailBookingInfo(application, placementApplication),
    withdrawalTriggeredBy,
  )

  fun spaceBookingWithdrawn(
    spaceBooking: Cas1SpaceBookingEntity,
    application: ApprovedPremisesApplicationEntity,
    withdrawalTriggeredBy: WithdrawalTriggeredBy,
  ) = bookingWithdrawn(
    spaceBooking.toEmailBookingInfo(application),
    withdrawalTriggeredBy,
  )

  private fun bookingWithdrawn(
    emailBookingInfo: EmailBookingInfo,
    withdrawalTriggeredBy: WithdrawalTriggeredBy,
  ) {
    val application = emailBookingInfo.application

    val allPersonalisation =
      buildCommonPersonalisation(emailBookingInfo).toMutableMap()

    allPersonalisation += "region" to emailBookingInfo.premises.probationRegion.name
    allPersonalisation["withdrawnBy"] = withdrawalTriggeredBy.getName()

    val interestedParties =
      (
        application.interestedPartiesEmailAddresses() +
          setOfNotNull(emailBookingInfo.placementApplication?.createdByUser?.email) +
          setOfNotNull(emailBookingInfo.premises.emailAddress) +
          setOfNotNull(application.cruManagementArea?.emailAddress)
        ).toSet()

    emailNotifier.sendEmails(
      recipientEmailAddresses = interestedParties,
      templateId = Cas1NotifyTemplates.BOOKING_WITHDRAWN_V2,
      personalisation = allPersonalisation,
      application = application,
    )
  }

  fun spaceBookingAmended(
    spaceBooking: Cas1SpaceBookingEntity,
    application: ApprovedPremisesApplicationEntity,
    updateType: Cas1SpaceBookingService.UpdateType,
  ) = bookingAmended(
    spaceBooking.toEmailBookingInfo(application),
    shortened = updateType == Cas1SpaceBookingService.UpdateType.SHORTENING,
  )

  fun bookingAmended(
    application: ApprovedPremisesApplicationEntity,
    booking: BookingEntity,
    placementApplication: PlacementApplicationEntity?,
  ) = bookingAmended(
    booking.toEmailBookingInfo(application, placementApplication),
    shortened = false,
  )

  private fun bookingAmended(
    emailBookingInfo: EmailBookingInfo,
    shortened: Boolean,
  ) {
    val application = emailBookingInfo.application
    val emailPersonalisation = buildCommonPersonalisation(emailBookingInfo)

    val interestedParties = buildSet {
      addAll(application.interestedPartiesEmailAddresses())
      emailBookingInfo.premises.emailAddress?.let { add(it) }
      emailBookingInfo.placementApplication?.createdByUser?.email?.let { add(it) }
      if (shortened) {
        application.cruManagementArea?.emailAddress?.let { add(it) }
      }
    }

    emailNotifier.sendEmails(
      recipientEmailAddresses = interestedParties,
      templateId = Cas1NotifyTemplates.BOOKING_AMENDED,
      personalisation = emailPersonalisation,
      application = application,
    )
  }

  private fun buildCommonPersonalisation(emailBookingInfo: EmailBookingInfo): Map<String, Any> {
    val application = emailBookingInfo.application

    val values = emailBookingInfo.personalisationValues()

    return mapOf(
      "apName" to values.premisesName,
      "applicationUrl" to applicationUrlTemplate.resolve("id", application.id.toString()),
      "applicationTimelineUrl" to applicationTimelineUrlTemplate.resolve("applicationId", application.id.toString()),
      "bookingUrl" to bookingUrlTemplate.resolve(
        mapOf(
          "premisesId" to emailBookingInfo.premises.id.toString(),
          "bookingId" to emailBookingInfo.bookingId.toString(),
        ),
      ),
      "crn" to values.crn,
      "startDate" to values.startDate,
      "endDate" to values.endDate,
      "lengthStay" to values.lengthOfStay,
      "lengthStayUnit" to values.lengthOfStayUnit,
    )
  }
}
