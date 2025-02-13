package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent

@Service
class AllocationChangedService {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun handleEvent(event: HmppsDomainEvent) {
    log.info("Handle allocation changed message ${event.occurredAt}")
  }
}
