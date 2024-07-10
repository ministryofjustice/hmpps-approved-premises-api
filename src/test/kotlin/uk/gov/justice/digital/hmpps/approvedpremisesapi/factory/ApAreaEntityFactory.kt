package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class ApAreaEntityFactory : Factory<ApAreaEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }
  private var identifier: Yielded<String> = { randomStringUpperCase(5) }
  private var emailAddress: Yielded<String?> = { randomStringUpperCase(10) }
  private var notifyReplyToEmailId: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withIdentifier(identifier: String) = apply {
    this.identifier = { identifier }
  }

  fun withEmailAddress(emailAddress: String?) = apply {
    this.emailAddress = { emailAddress }
  }

  fun withNotifyReplyToEmailId(notifyReplyToEmailId: String) = apply {
    this.notifyReplyToEmailId = { notifyReplyToEmailId }
  }

  override fun produce(): ApAreaEntity = ApAreaEntity(
    id = this.id(),
    name = this.name(),
    identifier = this.identifier(),
    probationRegions = mutableListOf(),
    emailAddress = this.emailAddress(),
    notifyReplyToEmailId = this.notifyReplyToEmailId(),
  )
}
