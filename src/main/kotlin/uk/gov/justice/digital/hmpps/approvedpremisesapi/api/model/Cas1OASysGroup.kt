package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1OASysGroup(

  @get:JsonProperty("group", required = true) val group: Cas1OASysGroupName,

  @get:JsonProperty("assessmentMetadata", required = true) val assessmentMetadata: Cas1OASysAssessmentMetadata,

  @get:JsonProperty("answers", required = true) val answers: kotlin.collections.List<OASysQuestion>,
)
