package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

/**
 *
 * @param id
 * @param name
 */
data class Cas3BookingPremisesSummary(

  @get:JsonProperty("id", required = true) val id: UUID,

  @get:JsonProperty("name", required = true) val name: String,
)
