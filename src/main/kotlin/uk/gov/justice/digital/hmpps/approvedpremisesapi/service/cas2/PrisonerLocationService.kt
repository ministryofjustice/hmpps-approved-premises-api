package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.Prisoner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2PrisonerLocationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2PrisonerLocationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.IgnorableMessageException
import java.net.URI
import java.util.*

@Service
class PrisonerLocationService(
  private val prisonerSearchClient: PrisonerSearchClient,
  private val applicationRepository: Cas2ApplicationRepository,
  private val prisonerLocationRepository: Cas2PrisonerLocationRepository,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun handleAllocationChangedEvent(event: HmppsDomainEvent) {
    log.info("Handle allocation changed event ${event.occurredAt}")
  }

  fun handleLocationChangedEvent(event: HmppsDomainEvent) {
    val nomsNumber = event.personReference.findNomsNumber() ?: throw IgnorableMessageException("No nomsNumber found")
    val detailUrl = event.detailUrl ?: throw IgnorableMessageException("No detail URL found")

    val applications = applicationRepository.findAllSubmittedApplicationByNomsNumber(nomsNumber)

    if (applications.isEmpty()) {
      return
    }

    val prisoner = prisonerSearchClient.getPrisoner(URI.create(detailUrl))
      ?: throw IgnorableMessageException("No prisoner found for detailUrl $detailUrl")

    applications.forEach {
      updatePrisonerLocation(it, prisoner, event)
    }
  }

  fun updatePrisonerLocation(application: Cas2ApplicationEntity, prisoner: Prisoner, event: HmppsDomainEvent) {
    val oldPrisonerLocation = prisonerLocationRepository.findPrisonerLocation(application.id)
      ?: throw IgnorableMessageException("No null prisoner location found for applicationId ${application.id}")
    prisonerLocationRepository.save(oldPrisonerLocation.copy(endDate = event.occurredAt.toOffsetDateTime()))
    prisonerLocationRepository.save(
      Cas2PrisonerLocationEntity(
        id = UUID.randomUUID(),
        application = application,
        prisonCode = prisoner.prisonId,
        staffId = null,
        occurredAt = event.occurredAt.toOffsetDateTime(),
        endDate = null,
      ),
    )
  }
}
