package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas2v2

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2v2.model.Cas2v2User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class Cas2v2UserFactory : Factory<Cas2v2User> {
  private var name: Yielded<String> = { randomStringUpperCase(6) }
  private var username: Yielded<String> = { randomStringUpperCase(8) }
  private var email: Yielded<String> = { "roger@nacro.external.example.com" }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withUsername(username: String) = apply {
    this.username = { username }
  }

  override fun produce(): Cas2v2User = Cas2v2User(
    id = UUID.randomUUID(),
    name = this.name(),
    username = this.username(),
    email = this.email(),
    authSource = "auth",
    isActive = true,
  )
}
