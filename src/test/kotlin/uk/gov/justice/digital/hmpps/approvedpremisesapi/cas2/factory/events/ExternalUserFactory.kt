package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.events.ExternalUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase

class ExternalUserFactory : Factory<ExternalUser> {
  private var name: Yielded<String> = { randomStringUpperCase(6) }
  private var username: Yielded<String> = { randomStringUpperCase(8) }
  private var email: Yielded<String> = { "roger@nacro.external.example.com" }
  private var origin: Yielded<String> = { "NACRO" }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withUsername(username: String) = apply {
    this.username = { username }
  }

  override fun produce(): ExternalUser = ExternalUser(
    name = this.name(),
    username = this.username(),
    email = this.email(),
    origin = this.origin(),
  )
}
