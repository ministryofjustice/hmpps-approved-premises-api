package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.service

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ManagePomCasesClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PomAllocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.problem.InvalidDomainEventException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent

@Service
class Cas2HdcAllocationChangedService(
  private val managePomCasesClient: ManagePomCasesClient,
  private val applicationService: Cas2HdcApplicationService,
  private val applicationRepository: Cas2ApplicationRepository,
  private val cas2HdcUserService: Cas2HdcUserService,
  private val emailService: Cas2HdcEmailService,
  private val cas2HdcLocationChangedService: Cas2HdcLocationChangedService,
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
          val allocatedUser = cas2HdcUserService.getUserByStaffId(staffId = pomAllocation.manager.code, application.serviceOrigin)
          val isSamePOM = allocatedUser.id == application.currentPomUserId
          if (isSamePOM) {
            log.info("POM has not changed for $nomsNumber.")
            return
          }

          if (application.isLocationChange(pomAllocation.prison.code)) {
            cas2HdcLocationChangedService.createLocationChangeAssignmentAndSendEmails(
              application,
              pomAllocation.prison.code,
            )
          }

          addApplicationAssignmentAndSendEmails(application, pomAllocation.prison.code, allocatedUser)
        }

        else -> {
          log.info("No POM allocated.")
        }
      }
    }
  }

  private fun addApplicationAssignmentAndSendEmails(
    application: Cas2ApplicationEntity,
    pomAllocationPrisonCode: String,
    pomAllocatedToOffender: Cas2UserEntity,
  ) {
    // We don't send emails for same prison allocations, so only send emails on the first allocation.
    val isFirstPomAllocationAtPrison = application.currentPomUserId == null

    application.createApplicationAssignment(
      prisonCode = pomAllocationPrisonCode,
      allocatedPomUser = pomAllocatedToOffender,
    )
    applicationRepository.save(application)

    if (isFirstPomAllocationAtPrison) {
      emailService.sendAllocationChangedEmails(
        application = application,
        emailAddress = pomAllocatedToOffender.email!!, // BAIL-WIP
        newPrisonCode = pomAllocationPrisonCode,
      )
    }
  }

  private fun getAllocationResponse(detailUrl: String) = when (val result = managePomCasesClient.getPomAllocation(detailUrl)) {
    is ClientResult.Success -> result.body
    is ClientResult.Failure.StatusCode -> when (result.status) {
      HttpStatus.NOT_FOUND -> log.info("No POM allocated")
      else -> result.throwException()
    }

    is ClientResult.Failure -> result.throwException()
  }
}
