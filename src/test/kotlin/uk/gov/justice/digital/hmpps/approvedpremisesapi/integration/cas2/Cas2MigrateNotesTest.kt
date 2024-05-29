package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas2

import com.ninjasquad.springmockk.SpykBean
import io.mockk.clearMocks
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.MigrationJobTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a CAS2 Assessor`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a CAS2 POM User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import java.time.OffsetDateTime
import java.util.UUID

class Cas2MigrateNotesTest : MigrationJobTestBase() {

  @SpykBean
  lateinit var realNotesRepository: Cas2ApplicationNoteRepository

  @AfterEach
  fun afterEach() {
    // SpringMockK does not correctly clear mocks for @SpyKBeans that are also a @Repository, causing mocked behaviour
    // in one test to show up in another (see https://github.com/Ninja-Squad/springmockk/issues/85)
    // Manually clearing after each test seems to fix this.
    clearMocks(realNotesRepository)
  }

  @Test
  fun `Should add assessments for CAS2 notes that don't have one and returns 202 response`() {
    `Given a CAS2 POM User` { userEntity, _ ->
      `Given a CAS2 Assessor` { assessor, _ ->
        val applicationSchema = cas2ApplicationJsonSchemaEntityFactory.produceAndPersist {
          withAddedAt(OffsetDateTime.now())
          withId(UUID.randomUUID())
        }

        val submittedApplication1 = createApplicationEntity(applicationSchema, userEntity, OffsetDateTime.now().minusDays(1))

        val assessment1 = cas2AssessmentEntityFactory.produceAndPersist {
          withApplication(submittedApplication1)
        }

        // create the notes (without assessment relationships) so there are two pages
        val assessment1NoteIds = mutableListOf<UUID>()
        repeat(12) {
          assessment1NoteIds.add(
            cas2NoteEntityFactory.produceAndPersist {
              withApplication(submittedApplication1)
              withCreatedByUser(userEntity)
            }.id,
          )
        }

        repeat(2) {
          assessment1NoteIds.add(
            cas2NoteEntityFactory.produceAndPersist {
              withApplication(submittedApplication1)
              withAssessment(assessment1)
              withCreatedByUser(userEntity)
            }.id,
          )
        }

        val submittedApplication2 = createApplicationEntity(applicationSchema, userEntity, OffsetDateTime.now().minusDays(1))

        val assessment2 = cas2AssessmentEntityFactory.produceAndPersist {
          withApplication(submittedApplication2)
        }

        val assessment2NoteIds = mutableListOf<UUID>()
        repeat(11) {
          assessment2NoteIds.add(
            cas2NoteEntityFactory.produceAndPersist {
              withApplication(submittedApplication2)
              withCreatedByUser(userEntity)
            }.id,
          )
        }

        repeat(3) {
          assessment2NoteIds.add(
            cas2NoteEntityFactory.produceAndPersist {
              withApplication(submittedApplication2)
              withAssessment(assessment2)
              withCreatedByUser(userEntity)
            }.id,
          )
        }

        migrationJobService.runMigrationJob(MigrationJobType.cas2NotesWithAssessments, 10)

        assessment1NoteIds.forEach {
          val updatedNote = realNotesRepository.findById(it)
          Assertions.assertThat(updatedNote.get().assessment!!.id).isEqualTo(assessment1.id)
        }

        assessment2NoteIds.forEach {
          val updatedNote = realNotesRepository.findById(it)
          Assertions.assertThat(updatedNote.get().assessment!!.id).isEqualTo(assessment2.id)
        }
      }
    }
  }

  private fun createApplicationEntity(applicationSchema: Cas2ApplicationJsonSchemaEntity, userEntity: NomisUserEntity, submittedAt: OffsetDateTime?) =
    cas2ApplicationEntityFactory.produceAndPersist {
      withId(UUID.randomUUID())
      withApplicationSchema(applicationSchema)
      withCreatedByUser(userEntity)
      withData("{}")
      withSubmittedAt(submittedAt)
    }
}
