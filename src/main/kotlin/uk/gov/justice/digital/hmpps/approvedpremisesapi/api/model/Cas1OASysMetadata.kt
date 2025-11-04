package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Cas1OASysMetadata(

  @get:JsonProperty("assessmentMetadata", required = true) val assessmentMetadata: Cas1OASysAssessmentMetadata,

  @get:JsonProperty("supportingInformation", required = true) val supportingInformation: kotlin.collections.List<Cas1OASysSupportingInformationQuestionMetaData>,
)
