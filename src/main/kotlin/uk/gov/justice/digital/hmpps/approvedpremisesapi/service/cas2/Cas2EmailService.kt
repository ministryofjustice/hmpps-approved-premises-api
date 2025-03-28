package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prisoner
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
  private val statusUpdateRepository: Cas2StatusUpdateRepository,
  private val prisonRegisterClient: PrisonRegisterClient,
  @Value("\${url-templates.frontend.cas2.application-overview}") private val applicationUrlTemplate: String,
  @Value("\${notify.emailaddresses.nacro}") private val nacroEmail: String,
) {

  fun sendLocationChangedEmails(application: Cas2ApplicationEntity, oldPomUserId: UUID, prisoner: Prisoner) {
    val oldPrisonCode = getOldPrisonCode(application, prisoner.prisonId) ?: error("Old prison code not found.")
    nomisUserRepository.findById(oldPomUserId).map { oldPom ->
      val oldOmuEmail = getEmail(oldPrisonCode)
      val newOmuEmail = getEmail(prisoner.prisonId)
      val oldPrisonName = getPrisonName(oldPrisonCode)
      val statusUpdate = statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id) ?: error("StatusUpdate for ${application.id} not found")
      emailNotificationService.sendCas2Email(
        oldPom.email!!,
        notifyConfig.templates.cas2ToTransferringPomApplicationTransferredToAnotherPrison,
        mapOf(
          "nomsNumber" to application.nomsNumber,
          "receivingPrisonName" to prisoner.prisonName,
        ),
      )
      emailNotificationService.sendCas2Email(
        oldOmuEmail,
        notifyConfig.templates.cas2ToTransferringPomUnitApplicationTransferredToAnotherPrison,
        mapOf(
          "nomsNumber" to application.nomsNumber,
          "receivingPrisonName" to prisoner.prisonName,
        ),
      )
      emailNotificationService.sendCas2Email(
        newOmuEmail,
        notifyConfig.templates.cas2ToReceivingPomUnitApplicationTransferredToAnotherPrison,
        mapOf(
          "nomsNumber" to application.nomsNumber,
          "transferringPrisonName" to oldPrisonName,
          "link" to getLink(application.id),
          "applicationStatus" to statusUpdate.label,
        ),
      )
      emailNotificationService.sendCas2Email(
        nacroEmail,
        notifyConfig.templates.cas2ToNacroApplicationTransferredToAnotherPrison,
        mapOf(
          "nomsNumber" to application.nomsNumber,
          "receivingPrisonName" to prisoner.prisonName,
          "transferringPrisonName" to oldPrisonName,
          "link" to getLink(application.id),
        ),
      )
    }.orElseThrow()
  }

  fun sendAllocationChangedEmails(application: Cas2ApplicationEntity, newPom: NomisUserEntity, newPrisonCode: String) {
    val oldPrisonCode = getOldPrisonCode(application, newPrisonCode) ?: error("Old prison code not found.")
    val oldPrisonName = getPrisonName(oldPrisonCode)
    val newPrisonName = getPrisonName(newPrisonCode)
    val statusUpdate = statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id) ?: error("StatusUpdate for ${application.id} not found")

    emailNotificationService.sendCas2Email(
      newPom.email!!,
      notifyConfig.templates.cas2ToReceivingPomApplicationTransferredToAnotherPom,
      mapOf(
        "nomsNumber" to application.nomsNumber,
        "transferringPrisonName" to oldPrisonName,
        "link" to getLink(application.id),
        "applicationStatus" to statusUpdate.label,
      ),
    )
    emailNotificationService.sendCas2Email(
      nacroEmail,
      notifyConfig.templates.cas2ToNacroApplicationTransferredToAnotherPom,
      mapOf(
        "nomsNumber" to application.nomsNumber,
        "receivingPrisonName" to newPrisonName,
        "link" to getLink(application.id),
      ),
    )
  }

  private fun getLink(applicationId: UUID): String = applicationUrlTemplate.replace("#id", applicationId.toString())

  fun getOldPrisonCode(application: Cas2ApplicationEntity, newPrisonCode: String): String? = application.applicationAssignments.firstOrNull { it.prisonCode != newPrisonCode }?.prisonCode

  fun getEmail(prisonId: String) = when (val result = prisonRegisterClient.getOmuContactDetails(prisonId)) {
    is ClientResult.Success -> result.body.emailAddress ?: error("OMU email address is null for prison ID $prisonId.")
    is ClientResult.Failure -> error("No OMU contact details found for prison ID $prisonId.")
  }

  fun getPrisonName(prisonId: String) = when (val result = prisonRegisterClient.getPrison(prisonId)) {
    is ClientResult.Success -> result.body.prisonName
    is ClientResult.Failure -> error("No prison name for prison ID $prisonId.")
  }
}
