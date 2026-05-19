package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class FullPersonSummary(

  @get:JsonProperty("name", required = true) val name: String,

  @get:JsonProperty("isRestricted", required = true) val isRestricted: Boolean,

  @get:JsonProperty("crn", required = true) override val crn: String,

  @get:JsonProperty("personType", required = true) override val personType: PersonSummaryDiscriminator,
) : PersonSummary
