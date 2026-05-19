package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationDeliveryUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.User

/**
 *
 * @param roles
 */
data class TemporaryAccommodationUser(

  @get:JsonProperty("roles", required = true) val roles: List<TemporaryAccommodationUserRole>,

  @get:JsonProperty("service", required = true) override val service: String,

  @get:JsonProperty("id", required = true) override val id: java.util.UUID,

  @get:JsonProperty("name", required = true) override val name: String,

  @get:JsonProperty("deliusUsername", required = true) override val deliusUsername: String,

  @get:JsonProperty("region", required = true) override val region: ProbationRegion,

  @get:JsonProperty("email") override val email: String? = null,

  @get:JsonProperty("telephoneNumber") override val telephoneNumber: String? = null,

  @get:JsonProperty("isActive") override val isActive: Boolean? = null,

  @get:JsonProperty("probationDeliveryUnit") override val probationDeliveryUnit: ProbationDeliveryUnit? = null,
) : User
