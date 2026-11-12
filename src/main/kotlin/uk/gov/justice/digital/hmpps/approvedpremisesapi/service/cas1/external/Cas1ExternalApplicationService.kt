package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1RequestForPlacementOutcomeDto
import java.util.UUID

@SuppressWarnings("TooGenericExceptionThrown")
@Service
class Cas1ExternalApplicationService(
  private val requestForPlacementOutcomeService: Cas1RequestForPlacementOutcomeService,
) {

  fun getPlacementPairsNewestFirst(applicationId: UUID): List<Cas1RequestForPlacementOutcomeDto> = requestForPlacementOutcomeService
    .getPlacementPairs(applicationId)
    .sortedByDescending { it.dateApplied }
}
