package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ManagePomCasesClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PomAllocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InvalidDomainEventException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.NomisUserService
import java.util.UUID

@Service
class Cas2AllocationChangedService(
  private val managePomCasesClient: ManagePomCasesClient,
  private val applicationService: Cas2ApplicationService,
  private val applicationRepository: Cas2ApplicationRepository,
  private val nomisUserService: NomisUserService,
  private val emailService: Cas2EmailService,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @Transactional
  fun process(event: HmppsDomainEvent) {
    val nomsNumber = event.personReference.findNomsNumber()
    val detailUrl = event.detailUrl

    if (nomsNumber == null || detailUrl == null) {
      throw InvalidDomainEventException(event)
    }

    applicationService.findApplicationToAssign(nomsNumber)?.let { application ->
      log.info("Received Allocation changed event with application to assign:\n {}", event)

      when (val pomAllocation = getAllocationResponse(detailUrl)) {
        is PomAllocation -> {
          val allocatedUser = nomisUserService.getUserByStaffId(staffId = pomAllocation.manager.code)
          addApplicationAssignment(application, pomAllocation.prison.code, allocatedUser)
        }

        else -> {
          log.info("No POM allocated.")
        }
      }
    }
  }

  private fun sendEmails(oldPrisonCode: String, newPrisonCode: String, application: Cas2ApplicationEntity, allocatedUser: NomisUserEntity) {
    val hasMovedPrison = oldPrisonCode != newPrisonCode
    if (hasMovedPrison) {
      emailService.sendAllocationChangedEmails(application = application, newPom = allocatedUser, newPrisonCode = newPrisonCode)
    }
  }

  private fun addApplicationAssignment(application: Cas2ApplicationEntity, newPrisonCode: String, allocatedUser: NomisUserEntity) {
    if (isNewAllocation(application.currentPomUserId, allocatedUser.id)) {
      val oldPrisonCode = application.currentPrisonCode!!
      application.createApplicationAssignment(
        prisonCode = newPrisonCode,
        allocatedPomUser = allocatedUser,
      )
      applicationRepository.save(application)
      sendEmails(oldPrisonCode, newPrisonCode, application, allocatedUser)
    }
  }

  private fun isNewAllocation(currentStaffId: UUID?, staffIdToCheck: UUID): Boolean = currentStaffId != staffIdToCheck

  private fun getAllocationResponse(detailUrl: String) = when (val result = managePomCasesClient.getPomAllocation(detailUrl)) {
    is ClientResult.Success -> result.body
    is ClientResult.Failure.StatusCode -> when (result.status) {
      HttpStatus.NOT_FOUND -> log.info("No POM allocated")
      else -> result.throwException()
    }

    is ClientResult.Failure -> result.throwException()
  }
}
