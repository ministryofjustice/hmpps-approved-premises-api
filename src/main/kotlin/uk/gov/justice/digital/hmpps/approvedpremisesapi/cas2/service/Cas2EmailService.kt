package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.Cas2NotifyTemplates
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OffenderManagementUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import java.util.UUID

@Service
class Cas2EmailService(
  private val emailNotificationService: EmailNotificationService,
  private val cas2UserRepository: Cas2UserRepository,
  private val statusUpdateRepository: Cas2StatusUpdateRepository,
  private val offenderManagementUnitRepository: OffenderManagementUnitRepository,
  @Value("\${url-templates.frontend.cas2.application-overview}") private val applicationUrlTemplate: String,
  @Value("\${url-templates.frontend.cas2.submitted-application-overview}") private val submittedApplicationUrlTemplate: String,
  @Value("\${notify.emailaddresses.nacro}") private val nacroEmail: String,
) {
  fun sendLocationChangedEmails(
    application: Cas2ApplicationEntity,
    prisonCode: String,
    transferringFromPomId: UUID?,
  ) {
    val oldPrisonCode = getOldPrisonCode(application, prisonCode) ?: error("Old prison code not found.")

    val oldOmu = offenderManagementUnitRepository.findByPrisonCode(oldPrisonCode)
      ?: error("No OMU found for old prison code $oldPrisonCode.")
    val newOmu = offenderManagementUnitRepository.findByPrisonCode(prisonCode)
      ?: error("No OMU found for new prison code $prisonCode.")

    // only send an email to previous POM if the offender actually has one
    transferringFromPomId?.let {
      val transferringFromPom = cas2UserRepository.findByIdAndServiceOrigin(it, application.serviceOrigin) ?: error("No Cas2 User found for id $it.")
      emailNotificationService.sendCas2Email(
        transferringFromPom.email!!,
        Cas2NotifyTemplates.cas2ToTransferringPomApplicationTransferredToAnotherPrison,
        mapOf(
          "nomsNumber" to application.nomsNumber,
          "receivingPrisonName" to newOmu.prisonName,
        ),
      )
    }
    emailNotificationService.sendCas2Email(
      oldOmu.email,
      Cas2NotifyTemplates.cas2ToTransferringPomUnitApplicationTransferredToAnotherPrison,
      mapOf(
        "nomsNumber" to application.nomsNumber,
        "receivingPrisonName" to newOmu.prisonName,
      ),
    )

    emailNotificationService.sendCas2Email(
      newOmu.email,
      Cas2NotifyTemplates.cas2ToReceivingPomUnitApplicationTransferredToAnotherPrison,
      mapOf(
        "nomsNumber" to application.nomsNumber,
        "transferringPrisonName" to oldOmu.prisonName,
        "link" to getLink(application.id),
        "applicationStatus" to getApplicationStatusOrDefault(application.id),
      ),
    )
    emailNotificationService.sendCas2Email(
      nacroEmail,
      Cas2NotifyTemplates.cas2ToNacroApplicationTransferredToAnotherPrison,
      mapOf(
        "nomsNumber" to application.nomsNumber,
        "receivingPrisonName" to newOmu.prisonName,
        "transferringPrisonName" to oldOmu.prisonName,
        "link" to getAssessorLink(application.id),
      ),
    )
  }

  fun sendAllocationChangedEmails(application: Cas2ApplicationEntity, emailAddress: String, newPrisonCode: String) {
    val oldPrisonCode = getOldPrisonCode(application, newPrisonCode) ?: error("Old prison code not found.")
    val oldOmu = offenderManagementUnitRepository.findByPrisonCode(oldPrisonCode) ?: error("No OMU found for old prison code $oldPrisonCode.")
    val newOmu = offenderManagementUnitRepository.findByPrisonCode(newPrisonCode) ?: error("No OMU found for new prison code $newPrisonCode.")

    emailNotificationService.sendCas2Email(
      emailAddress,
      Cas2NotifyTemplates.cas2ToReceivingPomApplicationTransferredToAnotherPom,
      mapOf(
        "nomsNumber" to application.nomsNumber,
        "transferringPrisonName" to oldOmu.prisonName,
        "link" to getLink(application.id),
        "applicationStatus" to getApplicationStatusOrDefault(application.id),
      ),
    )
    emailNotificationService.sendCas2Email(
      nacroEmail,
      Cas2NotifyTemplates.cas2ToNacroApplicationTransferredToAnotherPom,
      mapOf(
        "nomsNumber" to application.nomsNumber,
        "receivingPrisonName" to newOmu.prisonName,
        "link" to getAssessorLink(application.id),
      ),
    )
  }

  fun getReferrerEmail(application: Cas2ApplicationEntity): String? {
    // currently with cas2 bail, there will not be an application assignment as there is no POM nor prison.
    // in this instance we should fall back to use the created by user (a delius user)
    application.currentAssignment?.let {
      return it.allocatedPomUser?.email
        ?: offenderManagementUnitRepository.findByPrisonCode(it.prisonCode)!!.email
    }

    return application.createdByUser.email
  }

  fun getApplicationStatusOrDefault(applicationId: UUID): String = statusUpdateRepository.findFirstByApplicationIdOrderByCreatedAtDesc(applicationId)?.label ?: "Received"
  private fun getLink(applicationId: UUID): String = applicationUrlTemplate.replace("#id", applicationId.toString())
  private fun getAssessorLink(applicationId: UUID): String = submittedApplicationUrlTemplate.replace("#applicationId", applicationId.toString())
  fun getOldPrisonCode(application: Cas2ApplicationEntity, newPrisonCode: String): String? = application.applicationAssignments.firstOrNull { it.prisonCode != newPrisonCode }?.prisonCode
}
