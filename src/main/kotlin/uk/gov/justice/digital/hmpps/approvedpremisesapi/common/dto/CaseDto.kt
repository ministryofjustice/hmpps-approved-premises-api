package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.dto

import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.dto.TierDto
import java.time.OffsetDateTime

data class CaseDto(
  var crn: String,
  var nomsNumber: String?,
  var name: String?,
  var tier: TierDto?,
  val createdAt: OffsetDateTime,
  var lastUpdatedAt: OffsetDateTime,
  val gender: String? = null,
)
