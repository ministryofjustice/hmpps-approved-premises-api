package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Cas1SpaceBooking(
  val id: java.util.UUID,
  val applicationId: java.util.UUID,
  val person: Person,
  val premises: NamedId,
  val apArea: NamedId,
  val expectedArrivalDate: java.time.LocalDate,
  val expectedDepartureDate: java.time.LocalDate,
  val canonicalArrivalDate: java.time.LocalDate,
  val canonicalDepartureDate: java.time.LocalDate,
  val createdAt: java.time.Instant,
  val otherBookingsInPremisesForCrn: List<Cas1SpaceBookingDates>,
  val characteristics: List<Cas1SpaceCharacteristic>,
  val allowedActions: List<Cas1SpaceBookingAction>,
  val openChangeRequests: List<Cas1ChangeRequestSummary>,
  val assessmentId: java.util.UUID? = null,
  val tier: String? = null,
  val bookedBy: User? = null,
  @Deprecated(message = "Use placementRequestId")
  val requestForPlacementId: java.util.UUID? = null,
  val placementRequestId: java.util.UUID? = null,
  val actualArrivalDate: java.time.LocalDate? = null,
  @Deprecated(message = "Use actualArrivalDate")
  val actualArrivalDateOnly: java.time.LocalDate? = null,
  val actualArrivalTime: String? = null,
  val actualDepartureDate: java.time.LocalDate? = null,
  @Deprecated(message = "Use actualDepartureDate")
  val actualDepartureDateOnly: java.time.LocalDate? = null,
  val actualDepartureTime: String? = null,
  val departure: Cas1SpaceBookingDeparture? = null,
  val keyWorkerAllocation: Cas1KeyWorkerAllocation? = null,
  val cancellation: Cas1SpaceBookingCancellation? = null,
  val nonArrival: Cas1SpaceBookingNonArrival? = null,
  val deliusEventNumber: String? = null,
  val additionalInformation: String? = null,
)
