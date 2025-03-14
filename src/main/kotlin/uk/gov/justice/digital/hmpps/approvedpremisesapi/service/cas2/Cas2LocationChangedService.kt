package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import io.sentry.Sentry
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.IgnorableMessageException
import java.net.URI
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas2LocationChangedService(
  private val prisonerSearchClient: PrisonerSearchClient,
  private val applicationService: Cas2ApplicationService,
  private val applicationRepository: Cas2ApplicationRepository,
) {

  @Transactional
  @SuppressWarnings("ThrowsCount")
  fun handleLocationChangedEvent(event: HmppsDomainEvent) = try {
    val nomsNumber = event.personReference.findNomsNumber() ?: throw IgnorableMessageException("No nomsNumber found")
    val detailUrl = event.detailUrl ?: throw IgnorableMessageException("No detail URL found")

    applicationService.findMostRecentApplication(nomsNumber)?.apply {
      val prisoner = prisonerSearchClient.getPrisoner(URI.create(detailUrl))
        ?: throw IgnorableMessageException("No prisoner found for detailUrl $detailUrl")

      this.applicationAssignments.add(
        Cas2ApplicationAssignmentEntity(
          id = UUID.randomUUID(),
          application = this,
          prisonCode = prisoner.prisonId,
          createdAt = OffsetDateTime.now(),
          allocatedPomUserId = null,
        ),
      )
      applicationRepository.save(this)
    }
  } catch (ime: IgnorableMessageException) {
    Sentry.captureException(ime)
  }
}
