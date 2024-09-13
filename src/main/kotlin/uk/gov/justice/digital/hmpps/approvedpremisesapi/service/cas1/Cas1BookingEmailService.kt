package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Constants.DAYS_IN_WEEK
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilInclusive
import java.time.LocalDate
import java.util.UUID

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

  fun spaceBookingMade(
    spaceBooking: Cas1SpaceBookingEntity,
  ) = bookingMade(
    spaceBooking.toBookingInfo(),
  )

  fun bookingMade(
    application: ApprovedPremisesApplicationEntity,
    booking: BookingEntity,
    placementApplication: PlacementApplicationEntity?,
  ) = bookingMade(
    booking.toBookingInfo(application, placementApplication),
  )

  private fun bookingMade(bookingInfo: BookingInfo) {
    val application = bookingInfo.application
    val placementApplication = bookingInfo.placementApplication

    val applicationSubmittedByUser = application.createdByUser

    val emailPersonalisation = buildCommonPersonalisation(bookingInfo)

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

    if (bookingInfo.premises.emailAddress != null) {
      emailNotifier.sendEmail(
        recipientEmailAddress = bookingInfo.premises.emailAddress!!,
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
  ) = bookingWithdrawn(
    booking.toBookingInfo(application, placementApplication),
    withdrawalTriggeredBy,
  )

  fun spaceBookingWithdrawn(
    spaceBooking: Cas1SpaceBookingEntity,
    withdrawalTriggeredBy: WithdrawalTriggeredBy,
  ) = bookingWithdrawn(
    spaceBooking.toBookingInfo(),
    withdrawalTriggeredBy,
  )

  private fun bookingWithdrawn(
    bookingInfo: BookingInfo,
    withdrawalTriggeredBy: WithdrawalTriggeredBy,
  ) {
    val application = bookingInfo.application

    val allPersonalisation =
      buildCommonPersonalisation(bookingInfo).toMutableMap()

    allPersonalisation += "region" to bookingInfo.premises.probationRegion.name
    allPersonalisation["withdrawnBy"] = withdrawalTriggeredBy.getName()

    val interestedParties =
      (
        application.interestedPartiesEmailAddresses() +
          setOfNotNull(bookingInfo.placementApplication?.createdByUser?.email) +
          setOfNotNull(bookingInfo.premises.emailAddress) +
          setOfNotNull(application.apArea?.emailAddress)
        ).toSet()

    emailNotifier.sendEmails(
      recipientEmailAddresses = interestedParties,
      templateId = notifyConfig.templates.bookingWithdrawnV2,
      personalisation = allPersonalisation,
      application = application,
    )
  }

  private fun Cas1SpaceBookingEntity.toBookingInfo() =
    BookingInfo(
      bookingId = id,
      arrivalDate = canonicalArrivalDate,
      departureDate = canonicalDepartureDate,
      premises = premises,
      application = application,
      placementApplication = placementRequest.placementApplication,
    )

  private fun BookingEntity.toBookingInfo(
    application: ApprovedPremisesApplicationEntity,
    placementApplication: PlacementApplicationEntity?,
  ) = BookingInfo(
    bookingId = id,
    arrivalDate = arrivalDate,
    departureDate = departureDate,
    premises = premises as ApprovedPremisesEntity,
    application = application,
    placementApplication = placementApplication,
  )

  private data class BookingInfo(
    val bookingId: UUID,
    val arrivalDate: LocalDate,
    val departureDate: LocalDate,
    val premises: ApprovedPremisesEntity,
    val application: ApprovedPremisesApplicationEntity,
    val placementApplication: PlacementApplicationEntity?,
  )

  private fun buildCommonPersonalisation(bookingInfo: BookingInfo): Map<String, Any> {
    val application = bookingInfo.application

    val lengthOfStayDays = bookingInfo.arrivalDate.getDaysUntilInclusive(bookingInfo.departureDate).size
    val lengthOfStayWeeks = lengthOfStayDays.toDouble() / DAYS_IN_WEEK
    val lengthOfStayWeeksWholeNumber = (lengthOfStayDays.toDouble() % DAYS_IN_WEEK) == 0.0

    return mapOf(
      "apName" to bookingInfo.premises.name,
      "applicationUrl" to applicationUrlTemplate.resolve("id", application.id.toString()),
      "applicationTimelineUrl" to applicationTimelineUrlTemplate.resolve("applicationId", application.id.toString()),
      "bookingUrl" to bookingUrlTemplate.resolve(
        mapOf(
          "premisesId" to bookingInfo.premises.id.toString(),
          "bookingId" to bookingInfo.bookingId.toString(),
        ),
      ),
      "crn" to application.crn,
      "startDate" to bookingInfo.arrivalDate.toString(),
      "endDate" to bookingInfo.departureDate.toString(),
      "lengthStay" to if (lengthOfStayWeeksWholeNumber) lengthOfStayWeeks.toInt() else lengthOfStayDays,
      "lengthStayUnit" to if (lengthOfStayWeeksWholeNumber) "weeks" else "days",
    )
  }
}
