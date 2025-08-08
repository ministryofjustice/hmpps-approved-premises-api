package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Constants.DAYS_IN_WEEK
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilInclusive
import java.time.LocalDate
import java.util.UUID

fun ApprovedPremisesApplicationEntity.interestedPartiesEmailAddresses(): Set<String> = setOfNotNull(
  createdByUser.email,
  if (caseManagerIsNotApplicant == true) {
    caseManagerUserDetails?.email
  } else {
    null
  },
)

fun PlacementApplicationEntity.interestedPartiesEmailAddresses(): Set<String> = application.interestedPartiesEmailAddresses() +
  setOfNotNull(createdByUser.email)

fun PlacementRequestEntity.requestingUsersEmailAddresses() = application.interestedPartiesEmailAddresses() +
  (placementApplication?.interestedPartiesEmailAddresses() ?: emptySet())

fun Cas1SpaceBookingEntity.toEmailBookingInfo(
  application: ApprovedPremisesApplicationEntity,
) = EmailBookingInfo(
  bookingId = id,
  arrivalDate = canonicalArrivalDate,
  departureDate = canonicalDepartureDate,
  premises = premises,
  application = application,
  placementApplication = placementRequest?.placementApplication,
)

data class EmailBookingInfo(
  val bookingId: UUID,
  val arrivalDate: LocalDate,
  val departureDate: LocalDate,
  val premises: ApprovedPremisesEntity,
  val application: ApprovedPremisesApplicationEntity,
  val placementApplication: PlacementApplicationEntity?,
) {
  fun personalisationValues(): BookingEmailPersonalisationValues {
    val lengthOfStayDays = arrivalDate.getDaysUntilInclusive(departureDate).size
    val lengthOfStayWeeks = lengthOfStayDays.toDouble() / DAYS_IN_WEEK
    val lengthOfStayWeeksWholeNumber = (lengthOfStayDays.toDouble() % DAYS_IN_WEEK) == 0.0

    return BookingEmailPersonalisationValues(
      crn = application.crn,
      startDate = arrivalDate.toString(),
      endDate = departureDate.toString(),
      premisesName = premises.name,
      lengthOfStay = if (lengthOfStayWeeksWholeNumber) lengthOfStayWeeks.toInt() else lengthOfStayDays,
      lengthOfStayUnit = if (lengthOfStayWeeksWholeNumber) "weeks" else "days",
    )
  }
}

data class BookingEmailPersonalisationValues(
  val crn: String,
  val startDate: String,
  val endDate: String,
  val premisesName: String,
  val lengthOfStay: Int,
  val lengthOfStayUnit: String,
)
