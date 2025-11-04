package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model

data class PersonDepartedDestination(

  val moveOnCategory: MoveOnCategory,

  val premises: DestinationPremises? = null,

  val destinationProvider: DestinationProvider? = null,
)
