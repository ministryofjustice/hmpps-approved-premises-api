package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class NewReferralHistoryUserNote(

  @get:JsonProperty("message", required = true) val message: kotlin.String,
)
