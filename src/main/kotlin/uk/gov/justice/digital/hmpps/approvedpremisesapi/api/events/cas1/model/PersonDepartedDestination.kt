package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.DestinationPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.DestinationProvider
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.MoveOnCategory

/**
 *
 * @param moveOnCategory
 * @param premises
 * @param destinationProvider
 */
data class PersonDepartedDestination(

  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("moveOnCategory", required = true) val moveOnCategory: MoveOnCategory,

  @Schema(example = "null", description = "")
  @get:JsonProperty("premises") val premises: DestinationPremises? = null,

  @Schema(example = "null", description = "")
  @get:JsonProperty("destinationProvider") val destinationProvider: DestinationProvider? = null,
)
