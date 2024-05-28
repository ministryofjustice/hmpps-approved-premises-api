package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas2

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ExternalUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.ApplicationNotesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.OffsetDateTime
import java.util.UUID

class ApplicationNoteTransformerTest {
  private val user = NomisUserEntityFactory().produce()
  private val submittedApplication = Cas2ApplicationEntityFactory()
    .withCreatedByUser(user)
    .withSubmittedAt(OffsetDateTime.now())
    .produce()

  private val applicationNotesTransformer = ApplicationNotesTransformer()

  @Nested
  inner class WithExternalUser {
    @Test
    fun `transforms JPA Cas2ApplicationNote db entity to API representation`() {
      val externalUser = ExternalUserEntityFactory().produce()
      val createdAt = OffsetDateTime.now().randomDateTimeBefore(1)
      val jpaEntity = Cas2ApplicationNoteEntity(
        id = UUID.randomUUID(),
        createdByUser = externalUser,
        application = submittedApplication,
        body = "new note",
        createdAt = createdAt,
        assessment = Cas2AssessmentEntityFactory().produce(),
      )

      val expectedRepresentation = Cas2ApplicationNote(
        id = jpaEntity.id,
        createdAt = createdAt.toInstant(),
        email = jpaEntity.getUser().email!!,
        name = jpaEntity.getUser().name,
        body = jpaEntity.body,
      )

      val transformation = applicationNotesTransformer.transformJpaToApi(jpaEntity)

      Assertions.assertThat(transformation).isEqualTo(expectedRepresentation)
    }
  }

  @Nested
  inner class WithNomisUser {
    @Test
    fun `transforms JPA Cas2ApplicationNote db entity to API representation`() {
      val nomisUser = NomisUserEntityFactory().produce()
      val createdAt = OffsetDateTime.now().randomDateTimeBefore(1)
      val jpaEntity = Cas2ApplicationNoteEntity(
        id = UUID.randomUUID(),
        createdByUser = nomisUser,
        application = submittedApplication,
        body = "new note",
        createdAt = createdAt,
        assessment = Cas2AssessmentEntityFactory().produce(),
      )

      val expectedRepresentation = Cas2ApplicationNote(
        id = jpaEntity.id,
        createdAt = createdAt.toInstant(),
        email = jpaEntity.getUser().email!!,
        name = jpaEntity.getUser().name,
        body = jpaEntity.body,
      )

      val transformation = applicationNotesTransformer.transformJpaToApi(jpaEntity)

      Assertions.assertThat(transformation).isEqualTo(expectedRepresentation)
    }
  }
}
