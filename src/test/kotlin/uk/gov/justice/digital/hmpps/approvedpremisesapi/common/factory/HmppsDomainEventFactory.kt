package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.AdditionalInformation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.PersonReference
import java.time.ZonedDateTime

class HmppsDomainEventFactory : Factory<HmppsDomainEvent> {
  private var eventType: Yielded<String> = { "" }
  private var version: Yielded<Int> = { 0 }
  private var detailUrl: Yielded<String> = { "" }
  private var occurredAt: Yielded<ZonedDateTime> = { ZonedDateTime.now() }
  private var description: Yielded<String?> = { null }
  private var additionalInformation: Yielded<AdditionalInformation> = { AdditionalInformation() }
  private var personReference: Yielded<PersonReference> = { PersonReference() }

  fun withEventType(eventType: String) = apply {
    this.eventType = { eventType }
  }

  fun withPersonReference(personReference: PersonReference) = apply {
    this.personReference = { personReference }
  }

  override fun produce() = HmppsDomainEvent(
    eventType = eventType.invoke(),
    version = version.invoke(),
    detailUrl = detailUrl.invoke(),
    occurredAt = occurredAt.invoke(),
    description = description.invoke(),
    additionalInformation = additionalInformation.invoke(),
    personReference = personReference.invoke(),
  )
}
