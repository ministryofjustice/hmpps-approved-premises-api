package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 */
class UnknownPerson(

  override val crn: kotlin.String,

  override val type: PersonType,
) : Person
