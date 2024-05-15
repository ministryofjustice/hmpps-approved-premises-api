package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.assertj.core.groups.Tuple
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1EmailNotifier

/**
 * This class is offered as an alternative to a Mockk version of Cas1EmailNotificationService,
 * providing simpler syntax. If used, the 'reset' function should be called before each test
 */
class MockCas1EmailNotificationService : Cas1EmailNotifier {
  val requestedEmails = mutableListOf<EmailRequest>()

  override fun sendEmail(recipientEmailAddress: String, templateId: String, personalisation: Map<String, *>) {
    requestedEmails.add(EmailRequest(recipientEmailAddress, templateId, personalisation))
  }

  override fun sendEmails(
    recipientEmailAddresses: Set<String>,
    templateId: String,
    personalisation: Map<String, *>,
  ) {
    recipientEmailAddresses.forEach { sendEmail(it, templateId, personalisation) }
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
