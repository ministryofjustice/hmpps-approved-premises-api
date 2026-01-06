package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import java.util.UUID

data class Cas3SuitableApplication(
  val id: UUID,
  val applicationStatus: ApplicationStatus,
  val bookingStatus: Cas3BookingStatus?,
)
