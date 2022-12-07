package uk.gov.justice.digital.hmpps.approvedpremisesapi.domain.events

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity

@Service
class ApplicationEventGenerator {

  fun generate(eventName: String, application: ApplicationEntity): String {
    return "Generating an $eventName domain event for application ${application.id}"
  }
}
