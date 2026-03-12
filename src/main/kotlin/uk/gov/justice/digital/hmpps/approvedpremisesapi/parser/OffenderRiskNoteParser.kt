package uk.gov.justice.digital.hmpps.approvedpremisesapi.parser

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.CaseDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.deliuscontext.NoteDetail
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class OffenderRiskNoteParser {
  companion object {
    private val NOTE_HEADER_REGEX = Regex(
      "^Comment added by (.+?) on (\\d{2}/\\d{2}/\\d{4}) at \\d{2}:\\d{2}${System.lineSeparator()}",

    )

    private val splitParam = "---------------------------------------------------------" + System.lineSeparator()

    private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  }

  fun withParsedRiskNotes(caseDetail: CaseDetail): CaseDetail = caseDetail.copy(
    registrations = caseDetail.registrations.map { registration ->
      registration.copy(
        riskNotesDetail = parseRiskNotes(registration.riskNotes),
        riskNotes = null, // Hide the raw note field (ignored during JSON serialization) when passing to the UI.
      )
    },
  )

  private fun parseRiskNotes(notes: String?): List<NoteDetail> {
    return buildList {
      notes
        ?.takeIf { it.isNotBlank() }
        ?.split(splitParam)
        ?.asReversed()
        ?.forEach { rawNote ->

          val match = NOTE_HEADER_REGEX.find(rawNote)
          val header = match?.value

          val riskNote = (header?.let { rawNote.removePrefix(it) } ?: rawNote).trimEnd()
          if (riskNote.isBlank() || riskNote == "null") return@forEach

          val createdDate = match?.groupValues?.getOrNull(2)
            ?.let { LocalDate.parse(it, DATE_FORMAT) }

          add(
            NoteDetail(
              date = createdDate,
              note = riskNote,
            ),
          )
        }
    }
  }
}
