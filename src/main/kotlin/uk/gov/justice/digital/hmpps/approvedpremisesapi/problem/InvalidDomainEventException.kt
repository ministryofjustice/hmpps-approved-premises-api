package uk.gov.justice.digital.hmpps.approvedpremisesapi.problem

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.HmppsDomainEvent

class InvalidDomainEventException(
  val event: HmppsDomainEvent,
) : RuntimeException("Invalid message received: $event")
