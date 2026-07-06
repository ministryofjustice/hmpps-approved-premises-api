package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.TemporaryAccommodationAssessmentStatus
import java.time.LocalDate
import java.util.UUID

data class Cas3SuitableApplication(
  val id: UUID,
  val applicationStatus: ApplicationStatus,
  val assessmentStatus: TemporaryAccommodationAssessmentStatus?,
  val bookingStatus: Cas3BookingStatus?,
  val premises: Cas3ExternalPremisesDto?,
  val uiUrl: String,
)

data class Cas3ExternalPremisesDto(
  val startDate: LocalDate?,
  val endDate: LocalDate?,
  val name: String?,
  val addressLine1: String,
  val addressLine2: String?,
  val town: String?,
  val postcode: String,
)
