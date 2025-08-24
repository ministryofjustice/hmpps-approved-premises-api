package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity

import java.util.UUID

sealed interface User

sealed interface UnifiedUser : User {
  val id: UUID
  val name: String
  val email: String?
}
