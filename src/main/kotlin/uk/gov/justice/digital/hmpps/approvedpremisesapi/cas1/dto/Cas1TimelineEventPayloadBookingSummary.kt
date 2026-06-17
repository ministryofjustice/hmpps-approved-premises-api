package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NamedId
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TransferReason

data class Cas1TimelineEventPayloadBookingSummary(

  val bookingId: java.util.UUID,

  val premises: NamedId,

  val arrivalDate: java.time.LocalDate,

  val departureDate: java.time.LocalDate,

  val transferReason: TransferReason? = null,

  val additionalInformation: String? = null,
)
