package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

sealed interface User

sealed interface Cas2User : User {
  val name: String
  val email: String?
}
