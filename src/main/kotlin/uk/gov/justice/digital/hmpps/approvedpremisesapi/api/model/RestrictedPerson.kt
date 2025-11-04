package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

class RestrictedPerson(

  @get:JsonProperty("crn", required = true) override val crn: kotlin.String,

  @get:JsonProperty("type", required = true) override val type: PersonType,
) : Person
