package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ManagePomCasesClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PomAllocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InvalidDomainEventException
import java.util.UUID

@Service
class Cas2AllocationChangedService(
  private val managePomCasesClient: ManagePomCasesClient,
  private val applicationService: Cas2ApplicationService,
  private val applicationRepository: Cas2ApplicationRepository,
  private val nomisUserRepository: NomisUserRepository,
  private val emailService: Cas2EmailService,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @Suppress("TooGenericExceptionThrown")
  @Transactional
  fun process(event: HmppsDomainEvent) {
    val nomsNumber = event.personReference.findNomsNumber()
    val detailUrl = event.detailUrl

    if (nomsNumber == null || detailUrl == null) {
      throw InvalidDomainEventException(event)
    }

    applicationService.findMostRecentApplication(nomsNumber)?.let { application ->
      log.info("Received Allocation changed event:\n{}", event)

      when (val pomAllocation = getAllocationResponse(detailUrl)) {
        is PomAllocation -> {
          // this should call the nomis-user-roles api - /users/staff/{staffId} to get the staffDetail and create a user.
          // need to check permissions/roles before implementing
          val allocatedUser = nomisUserRepository.findByNomisStaffId(pomAllocation.manager.code)
            ?: throw RuntimeException("No NOMIS user details found")

          if (isNewAllocation(application.mostRecentPomUserId, allocatedUser.id)) {
            application.createApplicationAssignment(
              prisonCode = pomAllocation.prison.code,
              allocatedPomUserId = allocatedUser.id,
            )
            applicationRepository.save(application)

            emailService.sendAllocationChangedEmails(application, allocatedUser, pomAllocation.prison.code)
          }
        }

        else -> {
          log.info("No POM allocated.")
        }
      }
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
