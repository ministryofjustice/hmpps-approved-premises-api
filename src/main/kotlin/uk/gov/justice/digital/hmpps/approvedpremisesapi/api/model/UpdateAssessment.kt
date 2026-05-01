package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class UpdateAssessment(

  val `data`: kotlin.collections.Map<kotlin.String, kotlin.Any>,

  val releaseDate: java.time.LocalDate? = null,

  val accommodationRequiredFromDate: java.time.LocalDate? = null,
)
