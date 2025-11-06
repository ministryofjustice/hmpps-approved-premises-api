package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Cas1SpaceBookingSummary(

  val id: java.util.UUID,

  val person: PersonSummary,

  val premises: NamedId,

  val canonicalArrivalDate: java.time.LocalDate,

  val canonicalDepartureDate: java.time.LocalDate,

  val expectedArrivalDate: java.time.LocalDate,

  val expectedDepartureDate: java.time.LocalDate,

  val characteristics: List<Cas1SpaceCharacteristic> = arrayListOf(),

  val isCancelled: Boolean,

  val openChangeRequestTypes: List<Cas1ChangeRequestType>,

  val actualArrivalDate: java.time.LocalDate? = null,

  val actualDepartureDate: java.time.LocalDate? = null,

  val isNonArrival: Boolean? = null,

  val tier: String? = null,

  val keyWorkerAllocation: Cas1KeyWorkerAllocation? = null,

  val deliusEventNumber: String? = null,

  @Deprecated(message = "")
  val plannedTransferRequested: kotlin.Boolean? = null,

  @Deprecated(message = "")
  val appealRequested: kotlin.Boolean? = null,

  val createdAt: java.time.Instant? = null,

  val transferReason: TransferReason? = null,

  val additionalInformation: String? = null,
)
