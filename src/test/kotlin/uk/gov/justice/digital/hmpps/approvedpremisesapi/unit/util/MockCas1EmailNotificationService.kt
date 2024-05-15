package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.assertj.core.groups.Tuple
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1EmailNotifier

/**
 * This class is offered as an alternative to a Mockk version of Cas1EmailNotificationService,
 * providing simpler syntax. If used, the 'reset' function should be called before each test
 */
class MockCas1EmailNotificationService : Cas1EmailNotifier {
  private val requestedEmails = mutableListOf<Cas1EmailRequest>()

  override fun sendEmail(
    recipientEmailAddress: String,
    templateId: String,
    personalisation: Map<String, *>,
    application: ApprovedPremisesApplicationEntity,
  ) {
    requestedEmails.add(Cas1EmailRequest(recipientEmailAddress, templateId, personalisation, application))
  }

  override fun sendEmails(
    recipientEmailAddresses: Set<String>,
    templateId: String,
    personalisation: Map<String, *>,
    application: ApprovedPremisesApplicationEntity,
  ) {
    recipientEmailAddresses.forEach { sendEmail(it, templateId, personalisation, application) }
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
    application: ApprovedPremisesApplicationEntity,
  ) {
    assertThat(requestedEmails)
      .extracting<Tuple> { tuple(it.email, it.templateId) }
      .contains(tuple(recipientEmailAddress, templateId))

    val emailRequest = requestedEmails.first { it.email == recipientEmailAddress && it.templateId == templateId }
    assertThat(emailRequest.personalisation).containsAllEntriesOf(personalisationSubSet)
    assertThat(emailRequest.application).isEqualTo(application)
  }

  fun reset() {
    requestedEmails.clear()
  }
}

data class Cas1EmailRequest(
  val email: String,
  val templateId: String,
  val personalisation: Map<String, *>,
  val application: ApprovedPremisesApplicationEntity,
)
