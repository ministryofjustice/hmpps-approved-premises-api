package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationDeliveryUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

/**
 * 
 * @param roles 
 */
data class TemporaryAccommodationUser(

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("roles", required = true) val roles: List<TemporaryAccommodationUserRole>,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("service", required = true) override val service: String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("id", required = true) override val id: UUID,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("name", required = true) override val name: String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("deliusUsername", required = true) override val deliusUsername: String,

    @Schema(example = "null", required = true, description = "")
    @get:JsonProperty("region", required = true) override val region: ProbationRegion,

    @Schema(example = "null", description = "")
    @get:JsonProperty("email") override val email: String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("telephoneNumber") override val telephoneNumber: String? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("isActive") override val isActive: Boolean? = null,

    @Schema(example = "null", description = "")
    @get:JsonProperty("probationDeliveryUnit") override val probationDeliveryUnit: ProbationDeliveryUnit? = null
    ) : User{

}

