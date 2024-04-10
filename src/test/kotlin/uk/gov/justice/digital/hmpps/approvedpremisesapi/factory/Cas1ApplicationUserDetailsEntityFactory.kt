package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.util.UUID

class Cas1ApplicationUserDetailsEntityFactory : Factory<Cas1ApplicationUserDetailsEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }
  private var emailAddress: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }
  private var telephoneNumber: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }

  fun withEmailAddress(emailAddress: String) = apply {
    this.emailAddress = { emailAddress }
  }

  override fun produce(): Cas1ApplicationUserDetailsEntity = Cas1ApplicationUserDetailsEntity(
    this.id(),
    this.name(),
    this.emailAddress(),
    this.telephoneNumber(),
  )
}
