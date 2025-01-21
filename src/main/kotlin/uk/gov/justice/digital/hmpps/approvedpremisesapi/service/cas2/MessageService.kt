package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MessageService {
  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun handleMessage(message: Message) {
    log.info("received event: {}", message)

    print(message)
  }
}
