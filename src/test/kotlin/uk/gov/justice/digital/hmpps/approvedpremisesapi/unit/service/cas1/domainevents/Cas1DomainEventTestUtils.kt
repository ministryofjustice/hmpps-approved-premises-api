package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.domainevents

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.Cas1DomainEventEnvelopeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.GetCas1DomainEvent
import java.util.UUID

fun <T : Cas1DomainEventPayload> buildDomainEvent(
  data: T,
  schemaVersion: Int? = null,
): GetCas1DomainEvent<Cas1DomainEventEnvelope<T>> {
  val id = UUID.randomUUID()
  return GetCas1DomainEvent(
    id = id,
    data = Cas1DomainEventEnvelopeFactory<T>().withDetails(data).produce(),
    schemaVersion = schemaVersion,
  )
}
