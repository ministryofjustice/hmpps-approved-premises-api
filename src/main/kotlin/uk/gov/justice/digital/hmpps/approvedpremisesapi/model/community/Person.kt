package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community

data class Person(
  val crn: String,
  val name: String,
  val isActive: Boolean
)
