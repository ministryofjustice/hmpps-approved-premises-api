package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import io.sentry.Sentry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prisoner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import kotlin.jvm.optionals.getOrElse

@Service
class Cas2EmailService(
  private val emailNotificationService: EmailNotificationService,
  private val notifyConfig: NotifyConfig,
  private val nomisUserRepository: NomisUserRepository,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Suppress("TooGenericExceptionThrown")
  fun sendLocationChangedEmailToTransferringPom(
    application: Cas2ApplicationEntity,
    nomsNumber: String,
    prisoner: Prisoner,
  ) {
    val oldAllocatedPomUserId =
      application.applicationAssignments.first { it.allocatedPomUserId != null }.allocatedPomUserId!!
    val oldPom = nomisUserRepository.findById(oldAllocatedPomUserId).getOrElse {
      throw RuntimeException("No user for $oldAllocatedPomUserId found")
    }

    if (oldPom.email != null) {
      val recipientEmailAddress = oldPom.email!!
      val templateId = notifyConfig.templates.toTransferringPomApplicationTransferredToAnotherPrison
      val personalisation = mapOf(
        "nomsNumber" to nomsNumber,
        "prisonerName" to prisoner.name,
        "receivingPrisonName" to prisoner.prisonName,
      )

      emailNotificationService.sendCas2Email(recipientEmailAddress, templateId, personalisation)
    } else {
      val errorMessage =
        "Email not found for User $oldAllocatedPomUserId. Unable to send email for Location Transfer on Application ${application.id}"
      log.error(errorMessage)
      Sentry.captureMessage(errorMessage)
    }
  }
}
