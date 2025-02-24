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
    val nomsNumber = event.personReference.findNomsNumber() ?: throw Exception("No nomsNumber found")
    val detailUrl = event.detailUrl ?: throw Exception("No detail URL found")

    val applications = applicationRepository.findAllSubmittedApplicationByNomsNumber(nomsNumber)

    if (applications.isEmpty()) {
      log.info("No submitted applications found for NomsNumber $nomsNumber")
      return
    }

    val prisoner = prisonerSearchClient.getPrisoner(URI.create(detailUrl))
      ?: throw Exception("No prisoner found for detailUrl $detailUrl")

    applicationRepository.findAllSubmittedApplicationByNomsNumber(nomsNumber).forEach {
      updatePrisonerLocation(it, prisoner, event)
    }
  }

  fun updatePrisonerLocation(application: Cas2ApplicationEntity, prisoner: Prisoner, event: HmppsDomainEvent) {
    val oldPrisonerLocation = prisonerLocationRepository.findPrisonerLocation(application.id)
    if (oldPrisonerLocation != null) {
      val oldPrisonerLocationUpdated = oldPrisonerLocation.copy(occurredAt = event.occurredAt.toOffsetDateTime())
      prisonerLocationRepository.save(oldPrisonerLocationUpdated)
    }
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
