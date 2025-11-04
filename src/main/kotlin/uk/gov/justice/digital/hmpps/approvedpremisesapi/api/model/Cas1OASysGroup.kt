package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Groups questions and answers from OAsys. Groups directly align with OAsys Sections other than 'needs', which collates questions from multiple sections
 * @param group
 * @param assessmentMetadata
 * @param answers
 */
data class Cas1OASysGroup(

  @get:JsonProperty("group", required = true) val group: Cas1OASysGroupName,

  @get:JsonProperty("assessmentMetadata", required = true) val assessmentMetadata: Cas1OASysAssessmentMetadata,

  @get:JsonProperty("answers", required = true) val answers: kotlin.collections.List<OASysQuestion>,
)
