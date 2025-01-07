package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3LostBedsRepository
import java.util.UUID

@Service
class Cas3LostBedService(
  private val lostBedsRepository: Cas3LostBedsRepository,
) {
  fun getActiveLostBedsForPremisesId(premisesId: UUID) = lostBedsRepository.findAllActiveForPremisesId(premisesId)
}
