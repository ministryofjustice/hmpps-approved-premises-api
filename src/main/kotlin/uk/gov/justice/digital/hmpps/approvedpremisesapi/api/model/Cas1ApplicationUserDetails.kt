package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

data class Cas1ApplicationUserDetails(

  val name: String,

  val email: String? = null,

  val telephoneNumber: String? = null,
)
