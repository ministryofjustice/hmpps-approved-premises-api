package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.asserter

import org.assertj.core.api.Assertions.assertThat
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.test.context.event.annotation.BeforeTestMethod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService.Companion.resolveTemplateName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SendEmailRequestedEvent

@Component
class EmailNotificationAsserter {

  var log: Logger = LoggerFactory.getLogger(this::class.java)

  private val requestedEmails = mutableListOf<EmailRequest>()

  @EventListener
  fun consumeEmailRequestedEvent(emailRequested: SendEmailRequestedEvent) {
    log.info("Email requested ${emailRequested.request}")
    requestedEmails.add(emailRequested.request)
  }

  @BeforeTestMethod
  fun resetEmailList() {
    requestedEmails.clear()
  }

  fun assertNoEmailsRequested() {
    assertEmailsRequestedCount(0)
  }

  fun assertEmailRequested(
    toEmailAddress: String,
    templateId: String,
    personalisation: Map<String, Any> = emptyMap(),
    replyToEmailId: String? = null,
  ): EmailRequest {
    val match = requestedEmails.firstOrNull {
      toEmailAddress == it.email &&
        templateId == it.templateId &&
        it.personalisation.entries.containsAll(personalisation.entries) &&
        (replyToEmailId == null || it.replyToEmailId == replyToEmailId)
    }

    val templateName = resolveTemplateName(templateId)

    assertThat(match)
      .withFailMessage {
        "Could not find email request for template $templateName ($templateId) to $toEmailAddress with personalisation $personalisation. Provided email requests are ${formatRequestedEmails()}"
      }
      .isNotNull

    return match!!
  }

  fun formatRequestedEmails() = "\n" + requestedEmails.map { "\n $it" }

  fun assertEmailsRequestedCount(expectedCount: Int) {
    assertThat(requestedEmails.size).withFailMessage("Emails are $requestedEmails").isEqualTo(expectedCount)
  }
}
