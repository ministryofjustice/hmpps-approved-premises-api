package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent

@Service("LocationChangedService")
class LocationChangedService {

  val log = LoggerFactory.getLogger(this::class.java)

  fun handleEvent(event: HmppsDomainEvent) {
    log.info("Handle location changed message at ${event.occurredAt}")
  }
}
