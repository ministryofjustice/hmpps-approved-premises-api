package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import io.sentry.Sentry
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prisoner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import java.util.UUID

@Service
class Cas2EmailService(
  private val emailNotificationService: EmailNotificationService,
  private val notifyConfig: NotifyConfig,
  private val nomisUserRepository: NomisUserRepository,
  private val prisonsApiClient: PrisonsApiClient,
  private val statusUpdateRepository: Cas2StatusUpdateRepository,
  @Value("\${url-templates.frontend.cas2.application-overview}") private val applicationUrlTemplate: String,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun sendLocationChangedEmails(applicationId: UUID, oldPomUserId: UUID, oldPrisonCode: String, nomsNumber: String, prisoner: Prisoner) {
    nomisUserRepository.findById(oldPomUserId).map { oldPom ->
      prisonsApiClient.getAgencyDetails(oldPrisonCode).map { agency ->
        val statusUpdate = statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(applicationId) ?: throw EntityNotFoundException("StatusUpdate for $applicationId not found")
        sendLocationOrAllocationChangedEmail(
          oldPom.email,
          notifyConfig.templates.toTransferringPomApplicationTransferredToAnotherPrison,
          mapOf(
            "nomsNumber" to nomsNumber,
            "receivingPrisonName" to prisoner.prisonName,
          ),
        )
        sendLocationOrAllocationChangedEmail(
          "tbc",
          notifyConfig.templates.toTransferringPomUnitApplicationTransferredToAnotherPrison,
          mapOf(
            "nomsNumber" to nomsNumber,
            "receivingPrisonName" to prisoner.prisonName,
          ),
        )
        sendLocationOrAllocationChangedEmail(
          "tbc",
          notifyConfig.templates.toReceivingPomUnitApplicationTransferredToAnotherPrison,
          mapOf(
            "nomsNumber" to nomsNumber,
            "transferringPrisonName" to agency.description,
            "link" to getLink(applicationId),
            "applicationStatus" to statusUpdate.label,
          ),
        )
        sendLocationOrAllocationChangedEmail(
          "tbc",
          notifyConfig.templates.toNacroApplicationTransferredToAnotherPrison,
          mapOf(
            "nomsNumber" to nomsNumber,
            "receivingPrisonName" to prisoner.prisonName,
            "transferringPrisonName" to agency.description,
            "link" to getLink(applicationId),
          ),
        )
      }
    }.orElseThrow()
  }

  fun sendAllocationChangedEmails(newPom: NomisUserEntity, nomsNumber: String, application: Cas2ApplicationEntity, newPrisonCode: String) {
    val oldPrisonCode = application.applicationAssignments.first { it.prisonCode != newPrisonCode }.prisonCode

    prisonsApiClient.getAgencyDetails(oldPrisonCode).map { oldAgency ->
      prisonsApiClient.getAgencyDetails(newPrisonCode).map { newAgency ->
        val statusUpdate = statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id) ?: throw EntityNotFoundException("StatusUpdate for ${application.id} not found")

        sendLocationOrAllocationChangedEmail(
          newPom.email,
          notifyConfig.templates.toReceivingPomApplicationTransferredToAnotherPom,
          mapOf(
            "nomsNumber" to nomsNumber,
            "transferringPrisonName" to oldAgency.description,
            "link" to getLink(application.id),
            "applicationStatus" to statusUpdate.label,
          ),
        )
        sendLocationOrAllocationChangedEmail(
          "tbc",
          notifyConfig.templates.toNacroApplicationTransferredToAnotherPom,
          mapOf(
            "nomsNumber" to nomsNumber,
            "receivingPrisonName" to newAgency.description,
            "link" to getLink(application.id),
          ),
        )
      }
    }
  }

  private fun sendLocationOrAllocationChangedEmail(
    recipientEmailAddress: String?,
    templateId: String,
    personalisation: Map<String, String>,
  ) {
    if (recipientEmailAddress != null) {
      emailNotificationService.sendEmail(recipientEmailAddress, templateId, personalisation)
      log.info("Email $templateId ready to send to $recipientEmailAddress for NOMS Number ${personalisation["nomsNumber"]}")
    } else {
      val errorMessage = "Email $templateId not sent for NOMS Number ${personalisation["nomsNumber"]}"
      log.error(errorMessage)
      Sentry.captureMessage(errorMessage)
    }
  }

  private fun getLink(applicationId: UUID): String = applicationUrlTemplate.replace("#id", applicationId.toString())
}
