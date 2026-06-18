package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysQuestion

data class Cas1OASysGroup(

  @get:JsonProperty("group", required = true) val group: Cas1OASysGroupName,

  @get:JsonProperty("assessmentMetadata", required = true) val assessmentMetadata: Cas1OASysAssessmentMetadata,

  @get:JsonProperty("answers", required = true) val answers: List<OASysQuestion>,
)
