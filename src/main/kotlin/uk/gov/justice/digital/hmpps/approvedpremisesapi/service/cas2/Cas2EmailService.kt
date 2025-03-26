package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
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
  private val statusUpdateRepository: Cas2StatusUpdateRepository,
  private val offenderManagementUnitRepository: OffenderManagementUnitRepository,
  @Value("\${url-templates.frontend.cas2.application-overview}") private val applicationUrlTemplate: String,
  @Value("\${notify.emailaddresses.nacro}") private val nacroEmail: String,
) {

  fun sendLocationChangedEmails(application: Cas2ApplicationEntity, oldPomUserId: UUID, newPrisonCode: String) {
    val oldPrisonCode = getOldPrisonCode(application, newPrisonCode) ?: error("Old prison code not found.")
    nomisUserRepository.findById(oldPomUserId).map { oldPom ->
      val oldOmu = offenderManagementUnitRepository.findByPrisonCode(oldPrisonCode) ?: error("No OMU found for old prison code $oldPrisonCode.")
      val newOmu = offenderManagementUnitRepository.findByPrisonCode(newPrisonCode) ?: error("No OMU found for new prison code $newPrisonCode.")
      val statusUpdate = statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id) ?: error("StatusUpdate for ${application.id} not found")
      emailNotificationService.sendCas2Email(
        oldPom.email!!,
        notifyConfig.templates.cas2ToTransferringPomApplicationTransferredToAnotherPrison,
        mapOf(
          "nomsNumber" to application.nomsNumber,
          "receivingPrisonName" to newOmu.prisonName,
        ),
      )
      emailNotificationService.sendCas2Email(
        oldOmu.email,
        notifyConfig.templates.cas2ToTransferringPomUnitApplicationTransferredToAnotherPrison,
        mapOf(
          "nomsNumber" to application.nomsNumber,
          "receivingPrisonName" to newOmu.prisonName,
        ),
      )
      emailNotificationService.sendCas2Email(
        newOmu.email,
        notifyConfig.templates.cas2ToReceivingPomUnitApplicationTransferredToAnotherPrison,
        mapOf(
          "nomsNumber" to application.nomsNumber,
          "transferringPrisonName" to oldOmu.prisonName,
          "link" to getLink(application.id),
          "applicationStatus" to statusUpdate.label,
        ),
      )
      emailNotificationService.sendCas2Email(
        nacroEmail,
        notifyConfig.templates.cas2ToNacroApplicationTransferredToAnotherPrison,
        mapOf(
          "nomsNumber" to application.nomsNumber,
          "receivingPrisonName" to newOmu.prisonName,
          "transferringPrisonName" to oldOmu.prisonName,
          "link" to getLink(application.id),
        ),
      )
    }.orElseThrow()
  }

  fun sendAllocationChangedEmails(application: Cas2ApplicationEntity, newPom: NomisUserEntity, newPrisonCode: String) {
    val oldPrisonCode = getOldPrisonCode(application, newPrisonCode) ?: error("Old prison code not found.")
    val oldOmu = offenderManagementUnitRepository.findByPrisonCode(oldPrisonCode) ?: error("No OMU found for old prison code $oldPrisonCode.")
    val newOmu = offenderManagementUnitRepository.findByPrisonCode(newPrisonCode) ?: error("No OMU found for new prison code $newPrisonCode.")
    val statusUpdate = statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(application.id) ?: error("StatusUpdate for ${application.id} not found")

    emailNotificationService.sendCas2Email(
      newPom.email!!,
      notifyConfig.templates.cas2ToReceivingPomApplicationTransferredToAnotherPom,
      mapOf(
        "nomsNumber" to application.nomsNumber,
        "transferringPrisonName" to oldOmu.prisonName,
        "link" to getLink(application.id),
        "applicationStatus" to statusUpdate.label,
      ),
    )
    emailNotificationService.sendCas2Email(
      nacroEmail,
      notifyConfig.templates.cas2ToNacroApplicationTransferredToAnotherPom,
      mapOf(
        "nomsNumber" to application.nomsNumber,
        "receivingPrisonName" to newOmu.prisonName,
        "link" to getLink(application.id),
      ),
    )
  }

  private fun getLink(applicationId: UUID): String = applicationUrlTemplate.replace("#id", applicationId.toString())

  fun getOldPrisonCode(application: Cas2ApplicationEntity, newPrisonCode: String): String? = application.applicationAssignments.firstOrNull { it.prisonCode != newPrisonCode }?.prisonCode
}
