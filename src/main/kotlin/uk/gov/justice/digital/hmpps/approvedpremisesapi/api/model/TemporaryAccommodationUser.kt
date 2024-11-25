package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 *
 * @param roles
 */
data class TemporaryAccommodationUser(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("roles", required = true) val roles: kotlin.collections.List<TemporaryAccommodationUserRole>,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("service", required = true) override val service: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("id", required = true) override val id: java.util.UUID,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("name", required = true) override val name: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("deliusUsername", required = true) override val deliusUsername: kotlin.String,

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("region", required = true) override val region: ProbationRegion,

  @Schema(example = "null", description = "")
  @get:JsonProperty("email") override val email: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("telephoneNumber") override val telephoneNumber: kotlin.String? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("isActive") override val isActive: kotlin.Boolean? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("probationDeliveryUnit") override val probationDeliveryUnit: ProbationDeliveryUnit? = null,
) : User {
}
