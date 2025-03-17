package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpClientErrorException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ManagePomCasesClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PomAllocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PomDeallocated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PomNotAllocated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InvalidDomainEventException
import java.net.URI
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas2AllocationChangedService(
  private val managePomCasesClient: ManagePomCasesClient,
  private val applicationService: Cas2ApplicationService,
  private val applicationRepository: Cas2ApplicationRepository,
  private val nomisUserRepository: NomisUserRepository,
) {
  private val log = LoggerFactory.getLogger(this::class.java)
  private fun getAllocationResponse(detailUrl: String) = try {
    managePomCasesClient.getPomAllocation(URI.create(detailUrl))
  } catch (e: HttpClientErrorException) {
    when (e.message) {
      "404 Not allocated" -> PomDeallocated
      else -> PomNotAllocated
    }
  }

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

      val pomAllocation = getAllocationResponse(detailUrl)
      if (pomAllocation is PomAllocation) {
        // this should call the nomis-user-roles api - /users/staff/{staffId} to get the staffDetail and create a user.
        // need to check permissions/roles before implementing
        val user = nomisUserRepository.findByNomisStaffId(pomAllocation.manager.code)
          ?: throw RuntimeException("No NOMIS user details found")

        val newAssignment = Cas2ApplicationAssignmentEntity(
          id = UUID.randomUUID(),
          application = application,
          prisonCode = pomAllocation.prison.code,
          allocatedPomUserId = user.id,
          createdAt = OffsetDateTime.now(),
        )

        application.applicationAssignments.add(newAssignment)
        applicationRepository.save(application)
      } else {
        log.info("POM not allocated. No action taken: {}", pomAllocation)
      }
    }
  }
}
