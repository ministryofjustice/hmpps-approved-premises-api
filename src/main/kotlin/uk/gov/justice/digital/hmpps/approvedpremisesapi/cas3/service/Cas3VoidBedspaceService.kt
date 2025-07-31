package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspacesRepository
import java.util.UUID

@Service
class Cas3VoidBedspaceService(
  private val voidBedspacesRepository: Cas3VoidBedspacesRepository,
) {
  fun getActiveVoidBedspacesForPremisesId(premisesId: UUID) = voidBedspacesRepository.findAllActiveForPremisesId(premisesId)
}
