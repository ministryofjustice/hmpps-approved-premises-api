package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

@Deprecated("Use Cas1ApplicationSummary")
data class ApprovedPremisesApplicationSummary(

  val createdByUserId: java.util.UUID,

  val status: ApprovedPremisesApplicationStatus,

  val isWithdrawn: kotlin.Boolean,

  val hasRequestsForPlacement: kotlin.Boolean,

  override val type: kotlin.String,

  override val id: java.util.UUID,

  override val person: Person,

  override val createdAt: java.time.Instant,

  val isWomensApplication: kotlin.Boolean? = null,

  val isEmergencyApplication: kotlin.Boolean? = null,

  val arrivalDate: java.time.Instant? = null,

  val risks: PersonRisks? = null,

  val tier: kotlin.String? = null,

  val releaseType: ReleaseTypeOption? = null,

  override val submittedAt: java.time.Instant? = null,
) : ApplicationSummary
