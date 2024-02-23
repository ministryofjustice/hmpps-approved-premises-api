package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.assertj.core.groups.Tuple
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotifier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailRequest

/**
 * This class is offered as an alternative to a Mockk version of EmailNotificationService,
 * providing simpler syntax. If used, the 'reset' function should be called before each test
 */
class MockEmailNotificationService : EmailNotifier {
  val requestedEmails = mutableListOf<EmailRequest>()

  override fun sendEmail(recipientEmailAddress: String, templateId: String, personalisation: Map<String, *>) {
    requestedEmails.add(EmailRequest(recipientEmailAddress, templateId, personalisation))
  }

  fun assertNoEmailsRequested() {
    assertEmailRequestCount(0)
  }

  fun assertEmailRequestCount(expectedCount: Int) {
    assertThat(requestedEmails).hasSize(expectedCount)
  }

  fun assertEmailRequested(
    recipientEmailAddress: String,
    templateId: String,
    personalisationSubSet: Map<String, Any>,
  ) {
    assertThat(requestedEmails)
      .extracting<Tuple> { tuple(it.email, it.templateId) }
      .contains(tuple(recipientEmailAddress, templateId))

    val emailRequest = requestedEmails.first { it.email == recipientEmailAddress && it.templateId == templateId }
    assertThat(emailRequest.personalisation).containsAllEntriesOf(personalisationSubSet)
  }

  fun reset() {
    requestedEmails.clear()
  }
}
