package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsRepository
import java.util.UUID

@Service
class LostBedService(
  private val lostBedsRepository: LostBedsRepository,
) {
  fun getActiveLostBedsForPremisesId(premisesId: UUID) = lostBedsRepository.findAllActiveForPremisesId(premisesId)
}
