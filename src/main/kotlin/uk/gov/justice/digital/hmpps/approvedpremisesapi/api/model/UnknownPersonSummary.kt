package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 */
class UnknownPersonSummary(

  @get:JsonProperty("crn", required = true) override val crn: kotlin.String,

  @get:JsonProperty("personType", required = true) override val personType: PersonSummaryDiscriminator,
) : PersonSummary
