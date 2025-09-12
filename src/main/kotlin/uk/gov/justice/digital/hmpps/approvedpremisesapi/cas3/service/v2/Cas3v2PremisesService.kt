package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service.v2

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import java.time.LocalDate
import java.util.UUID

@Service
class Cas3v2PremisesService(
  private val cas3PremisesRepository: Cas3PremisesRepository,
  private val cas3BedspacesRepository: Cas3BedspacesRepository,
  private val cas3v2DomainEventService: Cas3v2DomainEventService,
) {
  fun getPremises(premisesId: UUID): Cas3PremisesEntity? = cas3PremisesRepository.findByIdOrNull(premisesId)
  fun findBedspace(premisesId: UUID, bedspaceId: UUID): Cas3BedspacesEntity? = cas3BedspacesRepository.findCas3Bedspace(premisesId = premisesId, bedspaceId = bedspaceId)

  fun unarchivePremisesAndSaveDomainEvent(premises: Cas3PremisesEntity, restartDate: LocalDate) {
    val currentStartDate = premises.startDate
    val currentEndDate = premises.endDate
    premises.startDate = restartDate
    premises.endDate = null
    premises.status = Cas3PremisesStatus.online
    cas3PremisesRepository.save(premises)
    cas3v2DomainEventService.savePremisesUnarchiveEvent(
      premises,
      currentStartDate,
      newStartDate = restartDate,
      currentEndDate,
    )
  }
}
