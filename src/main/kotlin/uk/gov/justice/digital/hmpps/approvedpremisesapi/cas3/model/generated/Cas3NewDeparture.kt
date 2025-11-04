package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.util.UUID

/**
 *
 * @param dateTime
 * @param reasonId
 * @param moveOnCategoryId
 * @param notes
 */
data class Cas3NewDeparture(

  @get:JsonProperty("dateTime", required = true) val dateTime: Instant,

  @get:JsonProperty("reasonId", required = true) val reasonId: UUID,

  @get:JsonProperty("moveOnCategoryId", required = true) val moveOnCategoryId: UUID,

  @get:JsonProperty("notes") val notes: String? = null,
)
