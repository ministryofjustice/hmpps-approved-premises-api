package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas2v2

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2v2.model.Cas2v2ApplicationStatusUpdatedEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2v2.model.Cas2v2Status
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2v2.model.Cas2v2User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2v2.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas2v2.Cas2v2PersonReferenceFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.util.UUID

class Cas2v2ApplicationStatusUpdatedEventDetailsFactory : Factory<Cas2v2ApplicationStatusUpdatedEventDetails> {

  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var applicationUrl: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var personReference: Yielded<PersonReference> = { Cas2v2PersonReferenceFactory().produce() }
  private var newStatus: Yielded<Cas2v2Status> = { Cas2v2StatusFactory().produce() }
  private var updatedAt: Yielded<Instant> = { Instant.now().randomDateTimeBefore(7) }
  private var updatedBy: Yielded<Cas2v2User> = { Cas2v2UserFactory().produce() }

  fun withApplicationId(applicationId: UUID) = apply {
    this.applicationId = { applicationId }
  }

  fun withApplicationUrl(applicationUrl: String) = apply {
    this.applicationUrl = { applicationUrl }
  }

  fun withPersonReference(personReference: PersonReference) = apply {
    this.personReference = { personReference }
  }

  fun withUpdatedAt(updatedAt: Instant) = apply {
    this.updatedAt = { updatedAt }
  }

  fun withStatus(newStatus: Cas2v2Status) = apply {
    this.newStatus = { newStatus }
  }

  fun withUpdatedBy(user: Cas2v2User) = apply {
    this.updatedBy = { user }
  }

  override fun produce(): Cas2v2ApplicationStatusUpdatedEventDetails = Cas2v2ApplicationStatusUpdatedEventDetails(
    applicationId = this.applicationId(),
    applicationUrl = this.applicationUrl(),
    personReference = this.personReference(),
    newStatus = this.newStatus(),
    updatedAt = this.updatedAt(),
    updatedBy = this.updatedBy(),
  )
}
