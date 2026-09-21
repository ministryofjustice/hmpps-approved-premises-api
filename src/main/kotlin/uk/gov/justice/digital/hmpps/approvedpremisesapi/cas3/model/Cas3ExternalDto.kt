package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.TemporaryAccommodationAssessmentStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.requireXor
import java.time.LocalDate
import java.util.UUID

data class Cas3ExternalCurrentApplicationDto(
  val id: UUID,
  val applicationStatus: ApplicationStatus,
  val uiUrl: String,
  @Schema(description = "A submitted application will be defined when the applicationStatus has any value other than 'inProgress'")
  val submittedApplication: Cas3ExternalSubmittedApplicationDto?,
  @Schema(deprecated = true)
  val applicationSubmittedDate: LocalDate?,
  @Schema(deprecated = true)
  val applicationSubmittedBy: Cas3StaffDto,
  @Schema(deprecated = true)
  val applicationRejectedReason: String?,
  @Schema(deprecated = true)
  val assessmentStatus: TemporaryAccommodationAssessmentStatus?,
  @Schema(deprecated = true)
  val bookingStatus: Cas3BookingStatus?,
  @Schema(deprecated = true)
  val bookingProvisionalOfferSentDate: LocalDate?,
  val previousBookings: List<Cas3ExternalPreviousBookingDto>?,
  @Schema(deprecated = true, description = "This is the most recent booking for the application, could arguably be named 'latestBooking' or 'mostRecentBooking' but 'premises' is the name used in SAS.")
  val premises: Cas3ExternalLatestBookingPremisesDto?,
) {
  init {
    requireXor(
      applicationStatus == ApplicationStatus.inProgress,
      submittedApplication != null,
    ) {
      "A submitted application is required for any status other than 'inProgress'"
    }
  }
}

data class Cas3ExternalSubmittedApplicationDto(
  val submittedDate: LocalDate,
  val submittedBy: Cas3StaffDto,
  val assessmentStatus: TemporaryAccommodationAssessmentStatus?,
  @Schema(description = "Will only be defined if the assessmentStatus is 'rejected'")
  val assessmentRejectionReason: String?,
  val latestBooking: Cas3ExternalLatestBookingDto?,
) {
  init {
    requireXor(
      assessmentStatus == TemporaryAccommodationAssessmentStatus.rejected,
      assessmentRejectionReason == null,
    ) {
      "Assessment rejection reason can only be provided if status is `rejected`"
    }
  }
}

data class Cas3ExternalLatestBookingDto(
  val status: Cas3BookingStatus?,
  val provisionalOfferSentDate: LocalDate?,
  val premises: Cas3ExternalLatestBookingPremisesDto,
)

data class Cas3ExternalLatestBookingPremisesDto(
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
  val cancellationDate: LocalDate,
  val cancellationReason: String,
)
