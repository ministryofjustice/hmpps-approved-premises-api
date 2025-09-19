package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity

sealed interface User

// TODO besscerule - renamed to prevent confusion with Cas2UserEntity
sealed interface UnifiedUser : User {
  val name: String
  val email: String?
}
