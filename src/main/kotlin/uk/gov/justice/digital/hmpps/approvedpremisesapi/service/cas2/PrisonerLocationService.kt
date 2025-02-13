package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent

@Service
class PrisonerLocationService {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun handleAllocationChangedEvent(event: HmppsDomainEvent) {
    log.info("Handle allocation changed event ${event.occurredAt}")
  }

  fun handleLocationChangedEvent(event: HmppsDomainEvent) {
    log.info("Handle location changed event at ${event.occurredAt}")
  }
}
