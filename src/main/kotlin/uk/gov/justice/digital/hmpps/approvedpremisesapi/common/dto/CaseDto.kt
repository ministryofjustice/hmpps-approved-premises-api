package uk.gov.justice.digital.hmpps.approvedpremisesapi.common.dto

import java.time.OffsetDateTime

data class CaseDto(
  var crn: String,
  var nomsNumber: String?,
  var name: String,
  var tier: String?,
  val createdAt: OffsetDateTime,
  var lastUpdatedAt: OffsetDateTime,
)
