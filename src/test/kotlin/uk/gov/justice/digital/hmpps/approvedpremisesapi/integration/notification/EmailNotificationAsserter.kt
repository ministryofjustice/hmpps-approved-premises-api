package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.notification

import org.assertj.core.api.Assertions.assertThat
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SendEmailRequestedEvent

@Component
class EmailNotificationAsserter {

  private val requestedEmails = mutableListOf<EmailRequest>()

  @EventListener
  fun emailRequested(emailRequested: SendEmailRequestedEvent) {
    requestedEmails.add(emailRequested.request)
  }

  fun assertEmailRequested(toEmailAddress: String, templateId: String) {
    val anyMatch = requestedEmails.any { toEmailAddress == it.email && templateId == it.templateId }

    assertThat(anyMatch)
      .withFailMessage {
        "Could not find email request. Provided email requests are $requestedEmails"
      }.isTrue
  }
}
