package uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisonsapi

import java.time.LocalDateTime

data class CaseNotesPage(
  val hasCaseNotes: Boolean,
  val metadata: PageMetaData,
  val content: List<CaseNote>,
)

data class CaseNote(
  val caseNoteId: String,
  val offenderIdentifier: String,
  val type: String,
  val typeDescription: String?,
  val subType: String,
  val subTypeDescription: String?,
  val source: String?,
  val creationDateTime: LocalDateTime,
  val occurrenceDateTime: LocalDateTime,
  val authorUserId: String,
  val authorName: String,
  val text: String,
  val locationId: String?,
  val eventId: Int,
  val sensitive: Boolean,
  val amendments: List<CaseNoteAmendment>,
)

data class CaseNoteAmendment(
  val creationDateTime: LocalDateTime,
  val authorName: String,
  val additionalNoteText: String,
)

data class CaseNotesRequest(
  val includeSensitive: Boolean,
  val typeSubTypes: Set<TypeSubTypeRequest> = emptySet(),
  val occurredFrom: LocalDateTime? = null,
  val occurredTo: LocalDateTime? = null,
  val page: Int,
  val size: Int,
  val sort: String,
)

data class TypeSubTypeRequest(val type: String, val subTypes: Set<String> = setOf())

data class PageMetaData(
  val page: Int,
  val size: Int,
  val totalElements: Int,
)
