package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import io.sentry.Sentry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PomAllocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prisoner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.NomisUserService
import java.util.UUID
import kotlin.jvm.optionals.getOrElse

@Service
class Cas2EmailService(
  private val emailNotificationService: EmailNotificationService,
  private val notifyConfig: NotifyConfig,
  private val nomisUserService: NomisUserService,
  private val prisonsApiClient: PrisonsApiClient,

  @Value("\${url-templates.frontend.cas2.application-overview}") private val applicationUrlTemplate: String,
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

    // TODO need to check elsewhere when cannot find user
    val oldPom = nomisUserService.getNomisUserByIdAndAddIfMissing(oldAllocatedPomUserId)
    val errorMessage =
      "Email not found for User $oldAllocatedPomUserId. Unable to send email for Location Transfer on Application ${application.id}"
    val personalisation = mapOf(
      "nomsNumber" to nomsNumber,
      "receivingPrisonName" to prisoner.prisonName,
    )
    sendLocationOrAllocationChangedEmail(
      oldPom.email,
      errorMessage,
      notifyConfig.templates.toTransferringPomApplicationTransferredToAnotherPrison,
      personalisation,
    )
  }

  fun sendLocationChangedEmailToTransferringPomUnit(
    applicationId: UUID,
    nomsNumber: String,
    prisoner: Prisoner,
  ) {
    // TODO get unit email from somewhere
    val email = "tbc"
    val errorMessage =
      "Email not found for transferring POM Unit. Unable to send email for Location Transfer on Application $applicationId"
    val personalisation = mapOf(
      "nomsNumber" to nomsNumber,
      "receivingPrisonName" to prisoner.prisonName,
    )
    sendLocationOrAllocationChangedEmail(
      email,
      errorMessage,
      notifyConfig.templates.toTransferringPomUnitApplicationTransferredToAnotherPrison,
      personalisation,
    )
  }

  fun sendLocationChangedEmailToNacro(
    application: Cas2ApplicationEntity,
    prisoner: Prisoner,
  ) {
    val oldPrisonCode =
      application.applicationAssignments.first { it.allocatedPomUserId != null }.prisonCode

    prisonsApiClient.getAgencyDetails(oldPrisonCode).map {
      // TODO get unit email from somewhere
      val email = "tbc"
      val errorMessage =
        "Email not found for Nacro. Unable to send email for Location Transfer on Application ${application.id}"
      val personalisation = mapOf(
        "receivingPrisonName" to prisoner.prisonName,
        "transferringPrisonName" to it.description,
        "link" to applicationUrlTemplate.replace("#id", application.id.toString()),
      )
      sendLocationOrAllocationChangedEmail(
        email,
        errorMessage,
        notifyConfig.templates.toNacroApplicationTransferredToAnotherPrison,
        personalisation,
      )
    }
  }

  private fun sendLocationOrAllocationChangedEmail(
    recipientEmailAddress: String?,
    errorMessage: String,
    templateId: String,
    personalisation: Map<String, String>,
  ) {
    if (recipientEmailAddress != null) {
      emailNotificationService.sendEmail(recipientEmailAddress, templateId, personalisation)
    } else {
      log.error(errorMessage)
      Sentry.captureMessage(errorMessage)
    }
  }

  fun sendLocationChangedEmailToReceivingPomUnit(
    applicationId: UUID,
    nomsNumber: String,
    prisoner: Prisoner,
  ) {
    val email = "tbc"
    val errorMessage =
      "Email not found for receiving ${prisoner.prisonName} POM Unit. Unable to send email for Location Transfer on Application $applicationId"
    val personalisation = mapOf(
      "nomsNumber" to nomsNumber,
      "receivingPrisonName" to prisoner.prisonName,
      "link" to applicationUrlTemplate.replace("#id", applicationId.toString()),
    )
    sendLocationOrAllocationChangedEmail(
      email,
      errorMessage,
      notifyConfig.templates.toReceivingPomUnitApplicationTransferredToAnotherPrison,
      personalisation,
    )
  }

  @Suppress("TooGenericExceptionThrown")
  fun sendAllocationChangedEmailToReceivingPom(
    application: Cas2ApplicationEntity,
    nomsNumber: String,
    pomAllocation: PomAllocation,
  ) {
    val newAllocatedPomUserId =
      application.applicationAssignments.first { it.allocatedPomUserId != null }.allocatedPomUserId!!

    // TODO need to check elsewhere when cannot find user
    val newPom = nomisUserService.getNomisUserByIdAndAddIfMissing(newAllocatedPomUserId)

    val oldPrisonCode =
      application.applicationAssignments.first { it.allocatedPomUserId == null && it.prisonCode !== pomAllocation.prison.code }.prisonCode

    val status = application.statusUpdates?.last()?.status()?.description
      ?: throw RuntimeException("No status found for application ${application.id}")

    prisonsApiClient.getAgencyDetails(oldPrisonCode).map { agency ->
      val errorMessage =
        "Email not found for User $newAllocatedPomUserId. Unable to send email for Location Transfer on Application ${application.id}"
      val personalisation = mapOf(
        "nomsNumber" to nomsNumber,
        "transferringPrisonName" to agency.description,
        "link" to applicationUrlTemplate.replace("#id", application.id.toString()),
        "applicationStaus" to status,
      )
      sendLocationOrAllocationChangedEmail(
        newPom.email,
        errorMessage,
        notifyConfig.templates.toReceivingPomApplicationTransferredToAnotherPrison,
        personalisation,
      )
    }
  }
}
