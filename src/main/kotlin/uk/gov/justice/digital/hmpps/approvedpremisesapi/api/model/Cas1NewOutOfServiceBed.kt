package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Cas1NewOutOfServiceBed(

  val startDate: java.time.LocalDate,

  val endDate: java.time.LocalDate,

  val reason: java.util.UUID,

  val bedId: java.util.UUID,

  val referenceNumber: kotlin.String? = null,

  val notes: kotlin.String? = null,
)
