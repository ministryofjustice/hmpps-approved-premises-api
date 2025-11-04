package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Cas1NewSpaceBookingCancellation(

  val occurredAt: java.time.LocalDate,

  val reasonId: java.util.UUID,

  val reasonNotes: kotlin.String? = null,
)
