package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2PrisonerLocationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2PrisonerLocationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2.Cas2OffenderEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.HmppsDomainEvent
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas2PrisonerLocationService(
  private val prisonerLocationRepository: Cas2PrisonerLocationRepository,
) {
  private val log = LoggerFactory.getLogger(this::class.java)
  fun handleAllocationChangedEvent(event: HmppsDomainEvent) {
    log.info("Handle allocation changed event ${event.occurredAt}")
  }

  fun handleLocationChangedEvent(event: HmppsDomainEvent) {
    log.info("Handle location changed event at ${event.occurredAt}")
  }

  @Transactional
  fun createPrisonerLocation(
    prisonCode: String,
    pomUserId: UUID,
    occurredAt: OffsetDateTime,
    offender: Cas2OffenderEntity,
  ): Cas2PrisonerLocationEntity = prisonerLocationRepository.save(
    Cas2PrisonerLocationEntity(
      id = UUID.randomUUID(),
      prisonCode = prisonCode,
      allocatedPomUserId = pomUserId,
      createdAt = occurredAt,
      offender = offender,
    ),
  )
}
