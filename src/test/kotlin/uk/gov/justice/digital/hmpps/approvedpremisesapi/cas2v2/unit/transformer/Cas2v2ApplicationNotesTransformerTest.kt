package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.unit.transformer

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer.Cas2v2ApplicationNotesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import java.time.OffsetDateTime
import java.util.UUID

class Cas2v2ApplicationNotesTransformerTest {
  private val user = Cas2UserEntityFactory()
    .withServiceOrigin(Cas2ServiceOrigin.BAIL)
    .produce()
  private val submittedApplication = Cas2ApplicationEntityFactory()
    .withCreatedByUser(user)
    .withSubmittedAt(OffsetDateTime.now())
    .produce()

  private val cas2v2ApplicationNotesTransformer = Cas2v2ApplicationNotesTransformer()

  @Nested
  inner class WithExternalUser {
    @Test
    fun `transforms JPA Cas2v2ApplicationNote db entity to API representation`() {
      val externalUser = Cas2UserEntityFactory()
        .withServiceOrigin(Cas2ServiceOrigin.BAIL)
        .produce()
      externalUser.userType = Cas2UserType.EXTERNAL

      val createdAt = OffsetDateTime.now().randomDateTimeBefore(1)
      val jpaEntity = Cas2ApplicationNoteEntity(
        id = UUID.randomUUID(),
        createdByUser = externalUser,
        application = submittedApplication,
        body = "new note",
        createdAt = createdAt,
        assessment = Cas2AssessmentEntityFactory()
          .withServiceOrigin(Cas2ServiceOrigin.BAIL)
          .produce(),
      )

      val expectedRepresentation = Cas2v2ApplicationNote(
        id = jpaEntity.id,
        createdAt = createdAt.toInstant(),
        email = jpaEntity.createdByUser.email!!,
        name = jpaEntity.createdByUser.name,
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
      val nomisUser = Cas2UserEntityFactory()
        .withServiceOrigin(Cas2ServiceOrigin.BAIL)
        .produce()
      val createdAt = OffsetDateTime.now().randomDateTimeBefore(1)
      val jpaEntity = Cas2ApplicationNoteEntity(
        id = UUID.randomUUID(),
        createdByUser = nomisUser,
        application = submittedApplication,
        body = "new note",
        createdAt = createdAt,
        assessment = Cas2AssessmentEntityFactory()
          .withServiceOrigin(Cas2ServiceOrigin.BAIL)
          .produce(),
      )

      val expectedRepresentation = Cas2v2ApplicationNote(
        id = jpaEntity.id,
        createdAt = createdAt.toInstant(),
        email = jpaEntity.createdByUser.email!!,
        name = jpaEntity.createdByUser.name,
        body = jpaEntity.body,
      )

      val transformation = cas2v2ApplicationNotesTransformer.transformJpaToApi(jpaEntity)

      Assertions.assertThat(transformation).isEqualTo(expectedRepresentation)
    }
  }
}
