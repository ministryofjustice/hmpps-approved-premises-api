package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param assessmentMetadata
 * @param supportingInformation
 */
data class Cas1OASysMetadata(

  val assessmentMetadata: Cas1OASysAssessmentMetadata,

  val supportingInformation: kotlin.collections.List<Cas1OASysSupportingInformationQuestionMetaData>,
)
