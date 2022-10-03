package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi

import java.time.LocalDateTime

data class CaseNotesPage(
  val totalElements: Int,
  val totalPages: Int,
  val number: Int,
  val content: List<CaseNote>
)

data class CaseNote(
  val caseNoteId: Long,
  val bookingId: Long,
  val type: String,
  val typeDescription: String?,
  val subType: String,
  val subTypeDescription: String?,
  val source: String?,
  val creationDateTime: LocalDateTime,
  val occurrenceDateTime: LocalDateTime,
  val staffId: Long,
  val authorName: String,
  val text: String,
  val originalNoteText: String,
  val agencyId: String?,
  val amendments: List<CaseNoteAmendment>
)

data class CaseNoteAmendment(
  val creationDateTime: LocalDateTime,
  val authorName: String,
  val additionalNoteText: String
)
