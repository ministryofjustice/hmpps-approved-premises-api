package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

@Deprecated("Use Cas1ApplicationSummary")
data class ApprovedPremisesApplicationSummary(

  @get:JsonProperty("createdByUserId", required = true) val createdByUserId: java.util.UUID,

  @get:JsonProperty("status", required = true) val status: ApprovedPremisesApplicationStatus,

  @get:JsonProperty("isWithdrawn", required = true) val isWithdrawn: Boolean,

  @get:JsonProperty("hasRequestsForPlacement", required = true) val hasRequestsForPlacement: Boolean,

  @get:JsonProperty("type", required = true) override val type: String,

  @get:JsonProperty("id", required = true) override val id: java.util.UUID,

  @get:JsonProperty("person", required = true) override val person: Person,

  @get:JsonProperty("createdAt", required = true) override val createdAt: java.time.Instant,

  @get:JsonProperty("isWomensApplication") val isWomensApplication: Boolean? = null,

  @get:JsonProperty("isEmergencyApplication") val isEmergencyApplication: Boolean? = null,

  @get:JsonProperty("arrivalDate") val arrivalDate: java.time.Instant? = null,

  @get:JsonProperty("risks") val risks: PersonRisks? = null,

  @get:JsonProperty("tier") val tier: String? = null,

  @get:JsonProperty("releaseType") val releaseType: ReleaseTypeOption? = null,

  @get:JsonProperty("submittedAt") override val submittedAt: java.time.Instant? = null,
) : ApplicationSummary
