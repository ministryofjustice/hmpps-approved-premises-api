package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.transformer

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.ApplicationNotesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.OffsetDateTime
import java.util.UUID

class ApplicationNoteTransformerTest {
  private val user = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS).produce()
  private val submittedApplication = Cas2ApplicationEntityFactory()
    .withCreatedByUser(user)
    .withSubmittedAt(OffsetDateTime.now())
    .produce()

  private val applicationNotesTransformer = ApplicationNotesTransformer()

  @Nested
  inner class WithExternalUser {
    @Test
    fun `transforms JPA Cas2ApplicationNote db entity to API representation`() {
      val externalUser = Cas2UserEntityFactory().withUserType(Cas2UserType.EXTERNAL).produce()
      val createdAt = OffsetDateTime.now().randomDateTimeBefore(1)
      val jpaEntity = Cas2ApplicationNoteEntity(
        id = UUID.randomUUID(),
        createdByCas2User = externalUser,
        application = submittedApplication,
        body = "new note",
        createdAt = createdAt,
        assessment = Cas2AssessmentEntityFactory().produce(),
      )

      val expectedRepresentation = Cas2ApplicationNote(
        id = jpaEntity.id,
        createdAt = createdAt.toInstant(),
        email = jpaEntity.createdByCas2User.email!!,
        name = jpaEntity.createdByCas2User.name,
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
      val nomisUser = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS).produce()
      val createdAt = OffsetDateTime.now().randomDateTimeBefore(1)
      val jpaEntity = Cas2ApplicationNoteEntity(
        id = UUID.randomUUID(),
        createdByCas2User = nomisUser,
        application = submittedApplication,
        body = "new note",
        createdAt = createdAt,
        assessment = Cas2AssessmentEntityFactory().produce(),
      )

      val expectedRepresentation = Cas2ApplicationNote(
        id = jpaEntity.id,
        createdAt = createdAt.toInstant(),
        email = jpaEntity.createdByCas2User.email!!,
        name = jpaEntity.createdByCas2User.name,
        body = jpaEntity.body,
      )

      val transformation = applicationNotesTransformer.transformJpaToApi(jpaEntity)

      Assertions.assertThat(transformation).isEqualTo(expectedRepresentation)
    }
  }
}
