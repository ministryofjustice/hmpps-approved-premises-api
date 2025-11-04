package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 */
class UnknownPersonSummary(

  override val crn: kotlin.String,

  override val personType: PersonSummaryDiscriminator,
) : PersonSummary
