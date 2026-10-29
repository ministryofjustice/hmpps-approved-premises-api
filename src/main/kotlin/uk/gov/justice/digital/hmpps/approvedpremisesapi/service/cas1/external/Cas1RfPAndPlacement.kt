package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.external

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalPlacementDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.Cas1ExternalRequestForPlacementDto
import java.time.LocalDateTime

data class Cas1RfPAndPlacement(
  val requestForPlacement: Cas1ExternalRequestForPlacementDto,
  val placement: Cas1ExternalPlacementDto?,
  val dateApplied: LocalDateTime,
)
