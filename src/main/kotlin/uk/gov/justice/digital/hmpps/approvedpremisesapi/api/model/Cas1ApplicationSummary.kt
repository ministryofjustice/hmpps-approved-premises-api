package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Cas1ApplicationSummary(

  val id: java.util.UUID,

  val person: Person,

  val createdAt: java.time.Instant,

  val createdByUserId: java.util.UUID,

  val createdByUserName: String?,

  val status: ApprovedPremisesApplicationStatus,

  val isWithdrawn: Boolean,

  val hasRequestsForPlacement: Boolean,

  val submittedAt: java.time.Instant? = null,

  val isWomensApplication: Boolean? = null,

  val isPipeApplication: Boolean? = null,

  val isEmergencyApplication: Boolean? = null,

  val isEsapApplication: Boolean? = null,

  val arrivalDate: java.time.LocalDate? = null,

  val risks: PersonRisks? = null,

  val tier: String? = null,

  val releaseType: ReleaseTypeOption? = null,
)
