package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param id
 * @param updatedAt
 * @param revisionType
 * @param updatedBy
 * @param startDate
 * @param endDate
 * @param reason
 * @param referenceNumber
 * @param notes
 */
data class Cas1OutOfServiceBedRevision(

  @get:JsonProperty("id", required = true) val id: java.util.UUID,

  @get:JsonProperty("updatedAt", required = true) val updatedAt: java.time.Instant,

  @get:JsonProperty("revisionType", required = true) val revisionType: kotlin.collections.List<Cas1OutOfServiceBedRevisionType>,

  @get:JsonProperty("updatedBy") val updatedBy: User? = null,

  @get:JsonProperty("startDate") val startDate: java.time.LocalDate? = null,

  @get:JsonProperty("endDate") val endDate: java.time.LocalDate? = null,

  @get:JsonProperty("reason") val reason: Cas1OutOfServiceBedReason? = null,

  @get:JsonProperty("referenceNumber") val referenceNumber: kotlin.String? = null,

  @get:JsonProperty("notes") val notes: kotlin.String? = null,
)
