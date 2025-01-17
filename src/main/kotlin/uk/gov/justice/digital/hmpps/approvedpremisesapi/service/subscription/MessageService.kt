package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.subscription

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MessageService {
  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun handleMessage(hmppsEvent: HmppsEvent) {
    log.info("received event: {}", hmppsEvent)

    print(hmppsEvent)
  }
}
