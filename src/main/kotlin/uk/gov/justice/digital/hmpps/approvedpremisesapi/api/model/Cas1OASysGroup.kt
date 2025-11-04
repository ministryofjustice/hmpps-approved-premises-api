package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Cas1OASysGroup(

  val group: Cas1OASysGroupName,

  val assessmentMetadata: Cas1OASysAssessmentMetadata,

  val answers: kotlin.collections.List<OASysQuestion>,
)
