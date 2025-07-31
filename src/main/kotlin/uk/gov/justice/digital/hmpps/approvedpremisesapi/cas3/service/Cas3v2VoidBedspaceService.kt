package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspacesRepository
import java.util.UUID

@Service
class Cas3v2VoidBedspaceService(
  private val cas3VoidBedspacesRepository: Cas3VoidBedspacesRepository,
) {
  fun findVoidBedspaces(premisesId: UUID): List<Cas3VoidBedspaceEntity> = cas3VoidBedspacesRepository.findActiveVoidBedspacesByPremisesId(premisesId)
}
