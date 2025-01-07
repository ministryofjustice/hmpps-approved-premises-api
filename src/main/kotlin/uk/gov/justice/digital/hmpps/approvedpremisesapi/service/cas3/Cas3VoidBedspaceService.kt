package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspacesRepository
import java.util.UUID

@Service
class Cas3VoidBedspaceService(
  private val voidBedspacesRepository: Cas3VoidBedspacesRepository,
) {
  fun getActiveVoidBedspacesForPremisesId(premisesId: UUID) = voidBedspacesRepository.findAllActiveForPremisesId(premisesId)
}
