package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.dto.AvailableTierDto

@Service
class TierService {

  fun getAvailableTiers(): List<AvailableTierDto> = listOf(
    "D0", "D1", "D2", "D3",
    "C0", "C1", "C2", "C3",
    "B0", "B1", "B2", "B3",
    "A0", "A1", "A2", "A3",
  ).map { AvailableTierDto(it) }
}
