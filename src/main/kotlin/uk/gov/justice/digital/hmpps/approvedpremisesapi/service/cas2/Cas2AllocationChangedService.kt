package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import io.sentry.Sentry
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpClientErrorException
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ManagePomCasesClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PomAllocation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PomDeallocated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PomNotAllocated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.IgnorableMessageException
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

  private fun getAllocationResponse(detailUrl: String) = try {
    managePomCasesClient.getPomAllocation(URI.create(detailUrl))
  } catch (e: HttpClientErrorException) {
    when (e.message) {
      "404 Not allocated" -> PomDeallocated
      else -> PomNotAllocated
    }
  }

  @Transactional
  fun saveApplicationAssignment(pomAllocation: PomAllocation, application: Cas2ApplicationEntity) {
    val user = nomisUserRepository.findByNomisStaffId(pomAllocation.manager.code)
      ?: throw IgnorableMessageException("No user found for staffCode ${pomAllocation.manager.code}")

    application.applicationAssignments.add(
      Cas2ApplicationAssignmentEntity(
        id = UUID.randomUUID(),
        application = application,
        prisonCode = pomAllocation.prison.code,
        allocatedPomUserId = user.id,
        createdAt = OffsetDateTime.now(),
      ),
    )
    applicationRepository.save(application)
  }

  @Transactional
  @SuppressWarnings("ThrowsCount")
  fun handleAllocationChangedEvent(event: HmppsDomainEvent) = try {
    val nomsNumber = event.personReference.findNomsNumber() ?: throw IgnorableMessageException("No nomsNumber found")
    val detailUrl = event.detailUrl ?: throw IgnorableMessageException("No detail URL found")

    applicationService.findMostRecentApplication(nomsNumber)?.apply {
      when (val pomAllocation = getAllocationResponse(detailUrl)) {
        is PomAllocation -> saveApplicationAssignment(pomAllocation, this)
        is PomDeallocated -> Sentry.captureMessage("Pom deallocated for $nomsNumber, no action required")
        else -> Sentry.captureMessage("Pom not allocated for $nomsNumber, no action required")
      }
    }
  } catch (ime: IgnorableMessageException) {
    Sentry.captureException(ime)
  }
}
