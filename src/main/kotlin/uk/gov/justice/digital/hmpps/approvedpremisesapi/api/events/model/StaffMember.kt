package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * A member of probation or AP staff
 * @param staffCode
 * @param staffIdentifier
 * @param forenames
 * @param surname
 * @param username
 */
data class StaffMember(

  @Schema(example = "N54A999", required = true, description = "")
  @get:JsonProperty("staffCode", required = true) val staffCode: kotlin.String,

  @Schema(example = "1501234567", required = true, description = "")
  @get:JsonProperty("staffIdentifier", required = true) val staffIdentifier: kotlin.Long,

  @Schema(example = "John", required = true, description = "")
  @get:JsonProperty("forenames", required = true) val forenames: kotlin.String,

  @Schema(example = "Smith", required = true, description = "")
  @get:JsonProperty("surname", required = true) val surname: kotlin.String,

  @Schema(example = "JohnSmithNPS", description = "")
  @get:JsonProperty("username") val username: kotlin.String? = null,
)
