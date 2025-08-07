package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.manageusers

import java.time.LocalDateTime
import java.util.UUID

data class ExternalUserDetails(
  val username: String,
  val userId: UUID,
  val firstName: String,
  val lastName: String,
  val email: String,
  val authSource: String,
  val enabled: Boolean,
  val locked: Boolean,
  val verified: Boolean,
  val lastLoggedIn: LocalDateTime?,
  val inactiveReason: String?,
)
