package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * A member of probation or AP staff
 * @param staffCode
 * @param forenames
 * @param surname
 * @param username
 */
data class StaffMember(

  @Schema(example = "N54A999", required = true, description = "")
  val staffCode: kotlin.String,

  @Schema(example = "John", required = true, description = "")
  val forenames: kotlin.String,

  @Schema(example = "Smith", required = true, description = "")
  val surname: kotlin.String,

  @Schema(example = "JohnSmithNPS", description = "")
  val username: kotlin.String? = null,
)
