package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.manageusers.ExternalUserDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomEmailAddress
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.LocalDateTime
import java.util.UUID

class ExternalUserDetailsFactory : Factory<ExternalUserDetails> {
  private var username: Yielded<String> = { randomStringUpperCase(8) }
  private var userId: Yielded<UUID> = { UUID.randomUUID() }
  private var firstName: Yielded<String> = { randomStringUpperCase(8) }
  private var lastName: Yielded<String> = { randomStringUpperCase(8) }
  private var email: Yielded<String> = { randomEmailAddress() }
  private var authSource: Yielded<String> = { "auth" }
  private var enabled: Yielded<Boolean> = { true }
  private var locked: Yielded<Boolean> = { false }
  private var verified: Yielded<Boolean> = { true }
  private var lastLoggedIn: Yielded<LocalDateTime?> = { null }
  private var inactiveReason: Yielded<String?> = { null }

  fun withUsername(username: String) = apply {
    this.username = { username }
  }

  fun withFirstName(firstName: String) = apply {
    this.firstName = { firstName }
  }

  fun withLastName(lastName: String) = apply {
    this.lastName = { lastName }
  }

  fun withEmail(email: String) = apply {
    this.email = { email }
  }

  override fun produce(): ExternalUserDetails = ExternalUserDetails(
    username = this.username(),
    userId = this.userId(),
    firstName = this.firstName(),
    lastName = this.lastName(),
    email = this.email(),
    authSource = this.authSource(),
    enabled = this.enabled(),
    locked = this.locked(),
    verified = this.verified(),
    lastLoggedIn = this.lastLoggedIn(),
    inactiveReason = this.inactiveReason(),
  )
}
