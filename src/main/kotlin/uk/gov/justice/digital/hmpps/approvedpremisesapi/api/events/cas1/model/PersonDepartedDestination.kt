package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

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
