package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.cas2

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationStatusUpdatedEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2Status
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.ExternalUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.util.UUID

class Cas2ApplicationStatusUpdatedEventDetailsFactory : Factory<Cas2ApplicationStatusUpdatedEventDetails> {

  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var applicationUrl: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var personReference: Yielded<PersonReference> = { PersonReferenceFactory().produce() }
  private var newStatus: Yielded<Cas2Status> = { Cas2StatusFactory().produce() }
  private var updatedAt: Yielded<Instant> = { Instant.now().randomDateTimeBefore(7) }
  private var updatedBy: Yielded<ExternalUser> = { ExternalUserFactory().produce() }

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

  fun withStatus(newStatus: Cas2Status) = apply {
    this.newStatus = { newStatus }
  }

  fun withUpdatedBy(externalUser: ExternalUser) = apply {
    this.updatedBy = { externalUser }
  }

  override fun produce(): Cas2ApplicationStatusUpdatedEventDetails = Cas2ApplicationStatusUpdatedEventDetails(
    applicationId = this.applicationId(),
    applicationUrl = this.applicationUrl(),
    personReference = this.personReference(),
    newStatus = this.newStatus(),
    updatedAt = this.updatedAt(),
    updatedBy = this.updatedBy(),
  )
}
