package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import jakarta.persistence.EntityNotFoundException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prisoner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OffenderManagementUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import java.util.UUID

@Service
class Cas2EmailService(
  private val emailNotificationService: EmailNotificationService,
  private val notifyConfig: NotifyConfig,
  private val nomisUserRepository: NomisUserRepository,
  private val prisonsApiClient: PrisonsApiClient,
  private val statusUpdateRepository: Cas2StatusUpdateRepository,
  private val offenderManagementUnitRepository: OffenderManagementUnitRepository,
  @Value("\${url-templates.frontend.cas2.application-overview}") private val applicationUrlTemplate: String,
  @Value("\${notify.emailaddresses.nacro}") private val nacroEmail: String,
) {

  fun sendLocationChangedEmails(application: Cas2ApplicationEntity, oldPomUserId: UUID, nomsNumber: String, prisoner: Prisoner) {
    val oldPrisonCode = getOldPrisonCode(application, prisoner.prisonId) ?: error("Old prison code ${prisoner.prisonId} not found.")

    nomisUserRepository.findById(oldPomUserId).map { oldPom ->
      prisonsApiClient.getAgencyDetails(oldPrisonCode).map { agency ->

        val oldOmuEmail = offenderManagementUnitRepository.findByPrisonCode(oldPrisonCode)?.email ?: error("No OMU email found for old prison code $oldPrisonCode.")
        val newOmuEmail = offenderManagementUnitRepository.findByPrisonCode(prisoner.prisonId)?.email ?: error("No OMU email found for new prison code ${prisoner.prisonId}.")

        val statusUpdate = statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id) ?: throw EntityNotFoundException("StatusUpdate for ${application.id} not found")
        emailNotificationService.sendCas2Email(
          oldPom.email!!,
          notifyConfig.templates.cas2ToTransferringPomApplicationTransferredToAnotherPrison,
          mapOf(
            "nomsNumber" to nomsNumber,
            "receivingPrisonName" to prisoner.prisonName,
          ),
        )
        emailNotificationService.sendCas2Email(
          oldOmuEmail,
          notifyConfig.templates.cas2ToTransferringPomUnitApplicationTransferredToAnotherPrison,
          mapOf(
            "nomsNumber" to nomsNumber,
            "receivingPrisonName" to prisoner.prisonName,
          ),
        )
        emailNotificationService.sendCas2Email(
          newOmuEmail,
          notifyConfig.templates.cas2ToReceivingPomUnitApplicationTransferredToAnotherPrison,
          mapOf(
            "nomsNumber" to nomsNumber,
            "transferringPrisonName" to agency.description,
            "link" to getLink(application.id),
            "applicationStatus" to statusUpdate.label,
          ),
        )
        emailNotificationService.sendCas2Email(
          nacroEmail,
          notifyConfig.templates.cas2ToNacroApplicationTransferredToAnotherPrison,
          mapOf(
            "nomsNumber" to nomsNumber,
            "receivingPrisonName" to prisoner.prisonName,
            "transferringPrisonName" to agency.description,
            "link" to getLink(application.id),
          ),
        )
      }
    }.orElseThrow()
  }

  fun sendAllocationChangedEmails(newPom: NomisUserEntity, nomsNumber: String, application: Cas2ApplicationEntity, newPrisonCode: String) {
    val oldPrisonCode = getOldPrisonCode(application, newPrisonCode) ?: error("Old prison code not found.")

    prisonsApiClient.getAgencyDetails(oldPrisonCode).map { oldAgency ->
      prisonsApiClient.getAgencyDetails(newPrisonCode).map { newAgency ->
        val statusUpdate = statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id) ?: throw EntityNotFoundException("StatusUpdate for ${application.id} not found")

        emailNotificationService.sendCas2Email(
          newPom.email!!,
          notifyConfig.templates.cas2ToReceivingPomApplicationTransferredToAnotherPom,
          mapOf(
            "nomsNumber" to nomsNumber,
            "transferringPrisonName" to oldAgency.description,
            "link" to getLink(application.id),
            "applicationStatus" to statusUpdate.label,
          ),
        )
        emailNotificationService.sendCas2Email(
          nacroEmail,
          notifyConfig.templates.cas2ToNacroApplicationTransferredToAnotherPom,
          mapOf(
            "nomsNumber" to nomsNumber,
            "receivingPrisonName" to newAgency.description,
            "link" to getLink(application.id),
          ),
        )
      }
    }
  }

  private fun getLink(applicationId: UUID): String = applicationUrlTemplate.replace("#id", applicationId.toString())

  fun getOldPrisonCode(application: Cas2ApplicationEntity, newPrisonCode: String): String? = application.applicationAssignments.firstOrNull { it.prisonCode != newPrisonCode }?.prisonCode
}
