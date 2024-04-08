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

  override fun sendEmail(recipientEmailAddress: String, templateId: String, personalisation: Map<String, *>, replyToEmailId: String?) {
    requestedEmails.add(EmailRequest(recipientEmailAddress, templateId, personalisation))
  }

  override fun sendEmails(
    recipientEmailAddresses: Set<String>,
    templateId: String,
    personalisation: Map<String, *>,
    replyToEmailId: String?,
  ) {
    recipientEmailAddresses.forEach { sendEmail(it, templateId, personalisation, replyToEmailId) }
  }

  override fun sendCas2Email(recipientEmailAddress: String, templateId: String, personalisation: Map<String, *>) {
    requestedEmails.add(EmailRequest(recipientEmailAddress, templateId, personalisation, replyToEmailId = "cbe00c2d-387b-4283-9b9c-13c8b7a61444"))
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
