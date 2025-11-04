package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param moveOnCategory
 * @param premises
 * @param destinationProvider
 */
data class PersonDepartedDestination(

  @get:JsonProperty("moveOnCategory", required = true) val moveOnCategory: MoveOnCategory,

  @get:JsonProperty("premises") val premises: DestinationPremises? = null,

  @get:JsonProperty("destinationProvider") val destinationProvider: DestinationProvider? = null,
)
