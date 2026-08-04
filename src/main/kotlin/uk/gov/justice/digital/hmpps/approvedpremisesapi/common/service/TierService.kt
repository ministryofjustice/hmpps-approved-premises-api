package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.common.dto.AvailableTierDto

@Service
class TierService {

  companion object {
    private val TIERS_V2 = listOf(
      "D0", "D1", "D2", "D3",
      "C0", "C1", "C2", "C3",
      "B0", "B1", "B2", "B3",
      "A0", "A1", "A2", "A3",
    ).map { AvailableTierDto(it) }

    private val TIERS_V3 = listOf(
      "A",
      "B",
      "C",
      "D",
      "E",
      "F",
      "G",
      "MISSING",
      "NOT_SUPERVISED",
    ).map { AvailableTierDto(it) }
  }

  fun getAvailableTiersV2(): List<AvailableTierDto> = TIERS_V2

  fun getAvailableTiersV3(): List<AvailableTierDto> = TIERS_V3
}
