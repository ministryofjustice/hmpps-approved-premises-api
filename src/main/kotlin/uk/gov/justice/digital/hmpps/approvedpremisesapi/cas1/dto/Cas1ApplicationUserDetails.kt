package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto

data class Cas1ApplicationUserDetails(

  val name: String,

  val email: String? = null,

  val telephoneNumber: String? = null,
)
