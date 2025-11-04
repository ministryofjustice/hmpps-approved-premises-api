package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param moveOnCategory
 * @param premises
 * @param destinationProvider
 */
data class PersonDepartedDestination(

  val moveOnCategory: MoveOnCategory,

  val premises: DestinationPremises? = null,

  val destinationProvider: DestinationProvider? = null,
)
