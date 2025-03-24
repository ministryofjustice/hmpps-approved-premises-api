package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import io.sentry.Sentry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prisoner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import java.util.UUID

@Service
class Cas2EmailService(
  private val emailNotificationService: EmailNotificationService,
  private val notifyConfig: NotifyConfig,
  private val nomisUserRepository: NomisUserRepository,
  private val prisonsApiClient: PrisonsApiClient,
  @Value("\${url-templates.frontend.cas2.application-overview}") private val applicationUrlTemplate: String,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  private fun sendLocationOrAllocationChangedEmail(
    recipientEmailAddress: String?,
    templateId: String,
    personalisation: Map<String, String>,
  ) {
    if (recipientEmailAddress != null) {
      emailNotificationService.sendEmail(recipientEmailAddress, templateId, personalisation)
      log.info("Email $templateId ready to send to ${recipientEmailAddress} for NOMS Number ${personalisation["nomsNumber"]}")
    } else {
      val errorMessage = "Email $templateId not sent for NOMS Number ${personalisation["nomsNumber"]}"
      log.error(errorMessage)
      Sentry.captureMessage(errorMessage)
    }
  }

  fun sendLocationChangedEmailToTransferringPom(
    application: Cas2ApplicationEntity,
    nomsNumber: String,
    prisoner: Prisoner,
  ) {
    // TODO need to check elsewhere when cannot find user
    nomisUserRepository.findById(application.mostRecentPomUserId).map { oldPom ->
      val personalisation = mapOf(
        "nomsNumber" to nomsNumber,
        "receivingPrisonName" to prisoner.prisonName,
      )
      sendLocationOrAllocationChangedEmail(
        oldPom.email,
        notifyConfig.templates.toTransferringPomApplicationTransferredToAnotherPrison,
        personalisation,
      )
    }
  }

  fun sendLocationChangedEmailToTransferringPomUnit(
    nomsNumber: String,
    prisoner: Prisoner,
  ) {
    // TODO get unit email from somewhere
    val email = "tbc"
    val personalisation = mapOf(
      "nomsNumber" to nomsNumber,
      "receivingPrisonName" to prisoner.prisonName,
    )
    sendLocationOrAllocationChangedEmail(
      email,
      notifyConfig.templates.toTransferringPomUnitApplicationTransferredToAnotherPrison,
      personalisation,
    )
  }

  fun sendLocationChangedEmailToReceivingPomUnit(
    application: Cas2ApplicationEntity,
    nomsNumber: String,
  ) {
    val email = "tbc"

    val oldPrisonCode = getPreviousPrisonCode(application)

    prisonsApiClient.getAgencyDetails(oldPrisonCode).map {
      val personalisation = mapOf(
        "nomsNumber" to nomsNumber,
        "transferringPrisonName" to it.description,
        "link" to getLink(application.id),
        "applicationStatus" to getApplicationStatus(application),
      )
      sendLocationOrAllocationChangedEmail(
        email,
        notifyConfig.templates.toReceivingPomUnitApplicationTransferredToAnotherPrison,
        personalisation,
      )
    }
  }

  fun sendLocationChangedEmailToNacro(
    application: Cas2ApplicationEntity,
    nomsNumber: String,
    prisoner: Prisoner,
  ) {
    val oldPrisonCode = getPreviousPrisonCode(application)

    prisonsApiClient.getAgencyDetails(oldPrisonCode).map {
      // TODO get unit email from somewhere
      val email = "tbc"
      val personalisation = mapOf(
        "nomsNumber" to nomsNumber,
        "receivingPrisonName" to prisoner.prisonName,
        "transferringPrisonName" to it.description,
        "link" to getLink(application.id),
      )
      sendLocationOrAllocationChangedEmail(
        email,
        notifyConfig.templates.toNacroApplicationTransferredToAnotherPrison,
        personalisation,
      )
    }
  }

  fun sendAllocationChangedEmailToReceivingPom(
    application: Cas2ApplicationEntity,
    nomsNumber: String,
  ) {
    // TODO need to check elsewhere when cannot find user
    nomisUserRepository.findById(application.currentPomUserId!!).map { newPom ->

      val oldPrisonCode = getPreviousPrisonCode(application)

      prisonsApiClient.getAgencyDetails(oldPrisonCode).map { agency ->
        val personalisation = mapOf(
          "nomsNumber" to nomsNumber,
          "transferringPrisonName" to agency.description,
          "link" to getLink(application.id),
          "applicationStatus" to getApplicationStatus(application),
        )
        sendLocationOrAllocationChangedEmail(
          newPom.email,
          notifyConfig.templates.toReceivingPomApplicationTransferredToAnotherPom,
          personalisation,
        )
      }
    }
  }

  fun sendAllocationChangedEmailToNacro(
    application: Cas2ApplicationEntity,
    nomsNumber: String,
  ) {
    val email = "tbc"
    prisonsApiClient.getAgencyDetails(application.currentPrisonCode).map { agency ->
      val personalisation = mapOf(
        "nomsNumber" to nomsNumber,
        "receivingPrisonName" to agency.description,
        "link" to getLink(application.id),
      )
      sendLocationOrAllocationChangedEmail(
        email,
        notifyConfig.templates.toNacroApplicationTransferredToAnotherPom,
        personalisation,
      )
    }
  }

//  private fun getApplicationStatus(application: Cas2ApplicationEntity): String =
//    application.statusUpdates?.last()?.status()?.description
//      ?: throw RuntimeException("No status found for application ${application.id}")

  @Suppress("UnusedParameter", "FunctionOnlyReturningConstant")
  private fun getApplicationStatus(application: Cas2ApplicationEntity): String = "PLACEHOLDER"

  private fun getPreviousPrisonCode(application: Cas2ApplicationEntity): String = application.applicationAssignments.first { it.prisonCode != application.currentPrisonCode }.prisonCode

  private fun getLink(applicationId: UUID): String = applicationUrlTemplate.replace("#id", applicationId.toString())
}
