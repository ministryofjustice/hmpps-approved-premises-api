package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1UpdateAssessment(

  @get:JsonProperty("data", required = true) val `data`: Map<String, Any>,
)
