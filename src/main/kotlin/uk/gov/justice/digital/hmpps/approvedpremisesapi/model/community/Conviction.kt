package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community

import java.time.LocalDateTime

data class Conviction(
  val convictionId: Long,
  val index: String,
  val active: Boolean,
  val offences: List<Offence>?,
)

data class Offence(
  val offenceId: String,
  val detail: OffenceDetail,
  val offenceDate: LocalDateTime?,
)

data class OffenceDetail(
  val mainCategoryDescription: String,
  val subCategoryDescription: String,
)
