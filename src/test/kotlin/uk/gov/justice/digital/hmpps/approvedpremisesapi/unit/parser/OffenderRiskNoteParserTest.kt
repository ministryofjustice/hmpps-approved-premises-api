package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.parser

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RegistrationFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.parser.OffenderRiskNoteParser
import java.time.LocalDate

class OffenderRiskNoteParserTest {
  private val parser = OffenderRiskNoteParser()

  private val splitParam = "---------------------------------------------------------" + System.lineSeparator()

  @Test
  fun `parseRiskNotes returns empty list for null riskNotes`() {
    val caseDetail = CaseDetailFactory()
      .withRegistrations(
        listOf(
          RegistrationFactory()
            .withRiskNotes(null)
            .produce(),
        ),
      )
      .produce()

    val result = parser.withParsedRiskNotes(caseDetail)

    assertThat(result.registrations[0].riskNotesDetail).isEmpty()
  }

  @Test
  fun `parseRiskNotes returns empty list for blank riskNotes`() {
    val caseDetail = CaseDetailFactory()
      .withRegistrations(
        listOf(
          RegistrationFactory()
            .withRiskNotes("   ")
            .produce(),
        ),
      )
      .produce()

    val result = parser.withParsedRiskNotes(caseDetail)

    assertThat(result.registrations[0].riskNotesDetail).isEmpty()
  }

  @Test
  fun `parseRiskNotes returns empty list for 'null' riskNotes`() {
    val caseDetail = CaseDetailFactory()
      .withRegistrations(
        listOf(
          RegistrationFactory()
            .withRiskNotes("null")
            .produce(),
        ),
      )
      .produce()

    val result = parser.withParsedRiskNotes(caseDetail)

    assertThat(result.registrations[0].riskNotesDetail).isEmpty()
  }

  @Test
  fun `parseRiskNotes parses a single note with a header correctly`() {
    val riskNotes = "Comment added by John Doe on 11/03/2026 at 14:19${System.lineSeparator()}This is a risk note"

    val caseDetail = CaseDetailFactory()
      .withRegistrations(
        listOf(
          RegistrationFactory()
            .withRiskNotes(riskNotes)
            .produce(),
        ),
      )
      .produce()

    val result = parser.withParsedRiskNotes(caseDetail)

    assertThat(result.registrations[0].riskNotesDetail).hasSize(1)
    assertThat(result.registrations[0].riskNotesDetail!![0].note).isEqualTo("This is a risk note")
    assertThat(result.registrations[0].riskNotesDetail!![0].date).isEqualTo(LocalDate.of(2026, 3, 11))
  }

  @Test
  fun `parseRiskNotes parses a single note without a header correctly`() {
    val riskNotes = "This is a risk note without a header"

    val caseDetail = CaseDetailFactory()
      .withRegistrations(
        listOf(
          RegistrationFactory()
            .withRiskNotes(riskNotes)
            .produce(),
        ),
      )
      .produce()

    val result = parser.withParsedRiskNotes(caseDetail)

    assertThat(result.registrations[0].riskNotesDetail).hasSize(1)
    assertThat(result.registrations[0].riskNotesDetail!![0].note).isEqualTo("This is a risk note without a header")
    assertThat(result.registrations[0].riskNotesDetail!![0].date).isNull()
  }

  @Test
  fun `parseRiskNotes parses multiple notes and reverses their order`() {
    val note1 = "Comment added by User A on 01/01/2026 at 10:00${System.lineSeparator()}First note"
    val note2 = "Comment added by User B on 02/01/2026 at 11:00${System.lineSeparator()}Second note"
    val note3 = "Third note without header"

    val riskNotes = listOf(note1, note2, note3).joinToString(separator = splitParam)

    val caseDetail = CaseDetailFactory()
      .withRegistrations(
        listOf(
          RegistrationFactory()
            .withRiskNotes(riskNotes)
            .produce(),
        ),
      )
      .produce()

    val result = parser.withParsedRiskNotes(caseDetail)

    val parsedNotes = result.registrations[0].riskNotesDetail!!
    assertThat(parsedNotes).hasSize(3)

    assertThat(parsedNotes[0].note).isEqualTo("Third note without header")
    assertThat(parsedNotes[0].date).isNull()

    assertThat(parsedNotes[1].note).isEqualTo("Second note")
    assertThat(parsedNotes[1].date).isEqualTo(LocalDate.of(2026, 1, 2))

    assertThat(parsedNotes[2].note).isEqualTo("First note")
    assertThat(parsedNotes[2].date).isEqualTo(LocalDate.of(2026, 1, 1))
  }

  @Test
  fun `parseRiskNotes ignores blank notes in multiple notes`() {
    val note1 = "Comment added by User A on 01/01/2026 at 10:00${System.lineSeparator()}First note"
    val note2 = "   "
    val note3 = "null"
    val note4 = "Comment added by User B on 02/01/2026 at 11:00${System.lineSeparator()}Second note"

    val riskNotes = listOf(note1, note2, note3, note4).joinToString(separator = splitParam)

    val caseDetail = CaseDetailFactory()
      .withRegistrations(
        listOf(
          RegistrationFactory()
            .withRiskNotes(riskNotes)
            .produce(),
        ),
      )
      .produce()

    val result = parser.withParsedRiskNotes(caseDetail)

    val parsedNotes = result.registrations[0].riskNotesDetail!!
    assertThat(parsedNotes).hasSize(2)
    assertThat(parsedNotes[0].note).isEqualTo("Second note")
    assertThat(parsedNotes[1].note).isEqualTo("First note")
  }
}
