package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService

@Service
class Cas1EmailNotificationService(
  private val emailNotificationService: EmailNotificationService,
) : Cas1EmailNotifier {

  override fun sendEmail(
    recipientEmailAddress: String,
    templateId: String,
    personalisation: Map<String, *>,
    application: ApprovedPremisesApplicationEntity,
  ) {
    emailNotificationService.sendEmail(
      recipientEmailAddress,
      templateId,
      personalisation,
    )
  }

  override fun sendEmails(
    recipientEmailAddresses: Set<String>,
    templateId: String,
    personalisation: Map<String, *>,
    application: ApprovedPremisesApplicationEntity,
  ) {
    emailNotificationService.sendEmails(
      recipientEmailAddresses,
      templateId,
      personalisation,
    )
  }
}

interface Cas1EmailNotifier {
  fun sendEmail(
    recipientEmailAddress: String,
    templateId: String,
    personalisation: Map<String, *>,
    application: ApprovedPremisesApplicationEntity,
  )

  fun sendEmails(
    recipientEmailAddresses: Set<String>,
    templateId: String,
    personalisation: Map<String, *>,
    application: ApprovedPremisesApplicationEntity,
  )
}
