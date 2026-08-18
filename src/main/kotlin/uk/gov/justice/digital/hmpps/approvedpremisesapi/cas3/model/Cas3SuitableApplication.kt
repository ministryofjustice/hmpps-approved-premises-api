package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.TemporaryAccommodationAssessmentStatus
import java.time.LocalDate
import java.util.UUID

data class Cas3SuitableApplication(
  val id: UUID,
  val applicationStatus: ApplicationStatus,
  val applicationSubmittedDate: LocalDate?,
  val applicationSubmittedBy: Cas3StaffDto,
  val applicationRejectedReason: String?,
  val assessmentStatus: TemporaryAccommodationAssessmentStatus?,
  val bookingStatus: Cas3BookingStatus?,
  val bookingProvisionalOfferSentDate: LocalDate?,
  val previousBookings: List<Cas3ExternalPreviousBookingDto>?,
  @Schema(description = "This is the most recent booking for the application, could arguably be named 'latestBooking' or 'mostRecentBooking' but 'premises' is the name used in SAS.")
  val premises: Cas3ExternalLatestBookingDto?,
  val uiUrl: String,
)

data class Cas3ExternalLatestBookingDto(
  val startDate: LocalDate?,
  val endDate: LocalDate?,
  val name: String?,
  val addressLine1: String,
  val addressLine2: String?,
  val town: String?,
  val postcode: String,
)

data class Cas3ExternalPreviousBookingDto(
  val bookingStatus: Cas3BookingStatus?,
  val cancellation: Cas3ExternalPreviousBookingCancellationDto?,
)

data class Cas3ExternalPreviousBookingCancellationDto(
  val cancellationDate: LocalDate?,
  val cancellationReason: String?,
)
