package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationAssignmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.IgnorableMessageException
import java.net.URI
import java.util.UUID

@Service
class Cas2LocationChangedService(
  private val prisonerSearchClient: PrisonerSearchClient,
  private val applicationService: Cas2ApplicationService,
  private val applicationAssignmentRepository: Cas2ApplicationAssignmentRepository,
) {

  @SuppressWarnings("ThrowsCount")
  fun handleLocationChangedEvent(event: HmppsDomainEvent) {
    val nomsNumber = event.personReference.findNomsNumber() ?: throw IgnorableMessageException("No nomsNumber found")
    val detailUrl = event.detailUrl ?: throw IgnorableMessageException("No detail URL found")

    val application = applicationService.findMostRecentApplication(nomsNumber) ?: return

    val prisoner = prisonerSearchClient.getPrisoner(URI.create(detailUrl))
      ?: throw IgnorableMessageException("No prisoner found for detailUrl $detailUrl")
    updateApplicationsAssignment(
      Cas2ApplicationAssignmentEntity(
        id = UUID.randomUUID(),
        application = application,
        prisonCode = prisoner.prisonId,
        createdAt = event.occurredAt.toOffsetDateTime(),
        allocatedPomUserId = null,
      ),
    )
  }

  fun updateApplicationsAssignment(assignment: Cas2ApplicationAssignmentEntity) {
    applicationAssignmentRepository.findFirstByApplicationIdOrderByCreatedAtDesc(assignment.application.id)
      ?: throw IgnorableMessageException("No application assigment found for applicationId ${assignment.application.id}")
    applicationAssignmentRepository.save(assignment)
  }
}
