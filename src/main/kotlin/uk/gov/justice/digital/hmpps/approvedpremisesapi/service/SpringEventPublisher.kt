package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

/**
 * A wrap around spring's [ApplicationEventPublisher] that is simpler to mock
 * as it uses kotlin's [Any] type
 */
@Service
class SpringEventPublisher(
  val applicationEventPublisher: ApplicationEventPublisher,
) {

  fun publishEvent(event: Any) {
    applicationEventPublisher.publishEvent(event)
  }
}
