package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Cas1OASysMetadata(

  val assessmentMetadata: Cas1OASysAssessmentMetadata,

  val supportingInformation: kotlin.collections.List<Cas1OASysSupportingInformationQuestionMetaData>,
)
