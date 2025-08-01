package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed.ApprovedPremisesApplicationIdNoteIdCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import kotlin.collections.forEach

class SeedCas1SoftDeleteApplicationTimelineNotesTest : SeedTestBase() {

  @Test
  fun `should succeed for valid application id and timeline note id`() {
    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCreatedByUser(givenAUser().first)
    }

    val noteIds = applicationTimelineNoteEntityFactory.produceAndPersistMultiple(3) {
      withApplicationId(application.id)
    }.map { it.id }

    seed(
      SeedFileType.approvedPremisesDeleteApplicationTimelineNotes,
      contents = listOf(
        ApprovedPremisesApplicationIdNoteIdCsvRow(
          applicationId = application.id.toString(),
          timelineNoteId = noteIds[1].toString(),
        ),
        ApprovedPremisesApplicationIdNoteIdCsvRow(
          applicationId = application.id.toString(),
          timelineNoteId = noteIds[2].toString(),
        ),
      ).toCsv(),
    )

    assertThat(applicationTimelineNoteRepository.getReferenceById(noteIds[0]).deletedAt).isNull()
    assertThat(applicationTimelineNoteRepository.getReferenceById(noteIds[1]).deletedAt).isNotNull()
    assertThat(applicationTimelineNoteRepository.getReferenceById(noteIds[2]).deletedAt).isNotNull()
  }

  @Test
  fun `should log error if application timeline note id is not found`() {
    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCreatedByUser(givenAUser().first)
    }

    val noteId = applicationTimelineNoteEntityFactory.produceAndPersist {
      withApplicationId(application.id)
    }.id

    seed(
      SeedFileType.approvedPremisesDeleteApplicationTimelineNotes,
      contents = listOf(
        ApprovedPremisesApplicationIdNoteIdCsvRow(
          applicationId = application.id.toString(),
          timelineNoteId = "9d4ac9fa-cf8e-42dd-aa02-9ac10750dcc7",
        ),
        ApprovedPremisesApplicationIdNoteIdCsvRow(
          applicationId = application.id.toString(),
          timelineNoteId = noteId.toString(),
        ),
      ).toCsv(),
    )

    assertThat(logEntries)
      .anyMatch {
        it.level == "error" &&
          it.message == "Timeline note with id 9d4ac9fa-cf8e-42dd-aa02-9ac10750dcc7 not found."
      }
    assertThat(applicationTimelineNoteRepository.getReferenceById(noteId).deletedAt).isNotNull()
  }

  @Test
  fun `should log error if application does not have note with note id`() {
    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCreatedByUser(givenAUser().first)
    }

    val noteId = applicationTimelineNoteEntityFactory.produceAndPersist {
      withApplicationId(application.id)
    }.id

    val anotherApplication = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withCreatedByUser(givenAUser().first)
    }

    val noteIdForAnotherApplication = applicationTimelineNoteEntityFactory.produceAndPersist {
      withApplicationId(anotherApplication.id)
    }.id

    seed(
      SeedFileType.approvedPremisesDeleteApplicationTimelineNotes,
      contents = listOf(
        ApprovedPremisesApplicationIdNoteIdCsvRow(
          applicationId = application.id.toString(),
          timelineNoteId = noteIdForAnotherApplication.toString(),
        ),
        ApprovedPremisesApplicationIdNoteIdCsvRow(
          applicationId = application.id.toString(),
          timelineNoteId = noteId.toString(),
        ),
      ).toCsv(),
    )

    assertThat(logEntries)
      .anyMatch {
        it.level == "error" &&
          it.message == "Timeline note with id $noteIdForAnotherApplication does not belong to application with id ${application.id}."
      }
    assertThat(applicationTimelineNoteRepository.getReferenceById(noteId).deletedAt).isNotNull()
  }

  private fun List<ApprovedPremisesApplicationIdNoteIdCsvRow>.toCsv(): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "applicationId",
        "timelineNoteId",
      )
      .newRow()

    this.forEach {
      builder
        .withQuotedField(it.applicationId)
        .withQuotedField(it.timelineNoteId)
        .newRow()
    }

    return builder.build()
  }
}
