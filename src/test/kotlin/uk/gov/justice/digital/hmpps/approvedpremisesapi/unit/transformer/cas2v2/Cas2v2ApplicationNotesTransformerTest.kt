package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas2v2

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ExternalUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2v2.Cas2v2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2v2.Cas2v2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2.Cas2v2ApplicationNotesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.OffsetDateTime
import java.util.UUID

class Cas2v2ApplicationNotesTransformerTest {
  private val user = NomisUserEntityFactory().produce()
  private val submittedApplication = Cas2v2ApplicationEntityFactory()
    .withCreatedByUser(user)
    .withSubmittedAt(OffsetDateTime.now())
    .produce()

  private val cas2v2ApplicationNotesTransformer = Cas2v2ApplicationNotesTransformer()

  @Nested
  inner class WithExternalUser {
    @Test
    fun `transforms JPA Cas2v2ApplicationNote db entity to API representation`() {
      val externalUser = ExternalUserEntityFactory().produce()
      val createdAt = OffsetDateTime.now().randomDateTimeBefore(1)
      val jpaEntity = Cas2v2ApplicationNoteEntity(
        id = UUID.randomUUID(),
        createdByUser = externalUser,
        application = submittedApplication,
        body = "new note",
        createdAt = createdAt,
        assessment = Cas2v2AssessmentEntityFactory().produce(),
      )

      val expectedRepresentation = Cas2v2ApplicationNote(
        id = jpaEntity.id,
        createdAt = createdAt.toInstant(),
        email = jpaEntity.getUser().email!!,
        name = jpaEntity.getUser().name,
        body = jpaEntity.body,
      )

      val transformation = cas2v2ApplicationNotesTransformer.transformJpaToApi(jpaEntity)

      Assertions.assertThat(transformation).isEqualTo(expectedRepresentation)
    }
  }

  @Nested
  inner class WithNomisUser {
    @Test
    fun `transforms JPA Cas2v2ApplicationNote db entity to API representation`() {
      val nomisUser = NomisUserEntityFactory().produce()
      val createdAt = OffsetDateTime.now().randomDateTimeBefore(1)
      val jpaEntity = Cas2v2ApplicationNoteEntity(
        id = UUID.randomUUID(),
        createdByUser = nomisUser,
        application = submittedApplication,
        body = "new note",
        createdAt = createdAt,
        assessment = Cas2v2AssessmentEntityFactory().produce(),
      )

      val expectedRepresentation = Cas2v2ApplicationNote(
        id = jpaEntity.id,
        createdAt = createdAt.toInstant(),
        email = jpaEntity.getUser().email!!,
        name = jpaEntity.getUser().name,
        body = jpaEntity.body,
      )

      val transformation = cas2v2ApplicationNotesTransformer.transformJpaToApi(jpaEntity)

      Assertions.assertThat(transformation).isEqualTo(expectedRepresentation)
    }
  }
}
