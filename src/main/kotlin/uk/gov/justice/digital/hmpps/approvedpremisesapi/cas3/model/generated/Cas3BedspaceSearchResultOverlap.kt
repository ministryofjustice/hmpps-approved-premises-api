package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import java.util.UUID

/**
 *
 * @param name
 * @param crn
 * @param personType
 * @param days
 * @param bookingId
 * @param roomId
 * @param isSexualRisk
 * @param sex
 * @param assessmentId
 */
data class Cas3BedspaceSearchResultOverlap(

  @get:JsonProperty("name", required = true) val name: String,

  @get:JsonProperty("crn", required = true) val crn: String,

  @get:JsonProperty("personType", required = true) val personType: PersonType,

  @get:JsonProperty("days", required = true) val days: Int,

  @get:JsonProperty("bookingId", required = true) val bookingId: UUID,

  @get:JsonProperty("roomId", required = true) val roomId: UUID,

  @get:JsonProperty("isSexualRisk", required = true) val isSexualRisk: Boolean,

  @get:JsonProperty("sex") val sex: String? = null,

  @get:JsonProperty("assessmentId") val assessmentId: UUID? = null,
)
