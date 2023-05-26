package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community

import java.time.LocalDate
import java.time.LocalDateTime

data class Conviction(
  val convictionId: Long,
  val index: String,
  val active: Boolean,
  val inBreach: Boolean,
  val failureToComplyCount: Long,
  val breachEnd: LocalDate?,
  val awaitingPsr: Boolean,
  val convictionDate: LocalDate?,
  val referralDate: LocalDate,
  val offences: List<Offence>?,
)

data class Offence(
  val offenceId: String,
  val mainOffence: Boolean,
  val detail: OffenceDetail,
  val offenceDate: LocalDateTime,
  val offenceCount: Long,
  val tics: Long?,
  val verdict: String?,
  val offenderId: Long,
  val createdDatetime: LocalDateTime,
  val lastUpdatedDatetime: LocalDateTime,
)

data class OffenceDetail(
  val code: String,
  val description: String,
  val abbreviation: String?,
  val mainCategoryCode: String,
  val mainCategoryDescription: String,
  val mainCategoryAbbreviation: String?,
  val ogrsOffenceCategory: String,
  val subCategoryCode: String,
  val subCategoryDescription: String,
  val form20Code: String?,
  val subCategoryAbbreviation: String?,
  val cjitCode: String?,
)
