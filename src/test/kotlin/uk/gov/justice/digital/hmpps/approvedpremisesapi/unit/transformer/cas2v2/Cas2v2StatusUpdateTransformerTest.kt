package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas2v2

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ExternalUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2v2.Cas2v2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2v2.Cas2v2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2ApplicationStatusSeeding
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ExternalUserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2.Cas2v2StatusUpdateTransformer
import java.time.OffsetDateTime

class Cas2v2StatusUpdateTransformerTest {
  private val user = NomisUserEntityFactory().produce()
  private val cas2v2SubmittedApplication = Cas2v2ApplicationEntityFactory()
    .withCreatedByUser(user)
    .withSubmittedAt(OffsetDateTime.now())
    .produce()

  private val mockExternalUserApi = mockk<ExternalUser>()
  private val mockExternalUserTransformer = mockk<ExternalUserTransformer>()
  private val cas2v2StatusUpdateTransformer = Cas2v2StatusUpdateTransformer(mockExternalUserTransformer)

  @BeforeEach
  fun setup() {
    every { mockExternalUserTransformer.transformJpaToApi(any()) } returns mockExternalUserApi
  }

  @Test
  fun `transforms JPA Cas2v2StatusUpdate db entity to API representation`() {
    val status = Cas2ApplicationStatusSeeding.statusList().random()

    val jpaEntity = Cas2v2StatusUpdateEntityFactory()
      .withStatusId(status.id)
      .withApplication(cas2v2SubmittedApplication)
      .produce()

    val expectedRepresentation = Cas2StatusUpdate(
      id = jpaEntity.id,
      name = status.name,
      label = jpaEntity.label,
      description = jpaEntity.description,
      updatedBy = mockExternalUserApi,
      updatedAt = jpaEntity.createdAt.toInstant(),
      statusUpdateDetails = null,
    )

    val transformation = cas2v2StatusUpdateTransformer.transformJpaToApi(jpaEntity)

    Assertions.assertThat(transformation).isEqualTo(expectedRepresentation)
  }
}
