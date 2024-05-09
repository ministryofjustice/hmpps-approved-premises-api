package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas2

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ExternalUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2ApplicationStatusSeeding
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ExternalUserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.StatusUpdateTransformer
import java.time.OffsetDateTime

class StatusUpdateTransformerTest {
  private val user = NomisUserEntityFactory().produce()
  private val submittedApplication = Cas2ApplicationEntityFactory()
    .withCreatedByUser(user)
    .withSubmittedAt(OffsetDateTime.now())
    .produce()

  private val mockExternalUserApi = mockk<ExternalUser>()
  private val mockExternalUserTransformer = mockk<ExternalUserTransformer>()
  private val statusUpdateTransformer = StatusUpdateTransformer(mockExternalUserTransformer)

  @BeforeEach
  fun setup() {
    every { mockExternalUserTransformer.transformJpaToApi(any()) } returns mockExternalUserApi
  }

  @Test
  fun `transforms JPA Cas2StatusUpdate db entity to API representation`() {
    val status = Cas2ApplicationStatusSeeding.statusList().random()

    val jpaEntity = Cas2StatusUpdateEntityFactory()
      .withStatusId(status.id)
      .withApplication(submittedApplication)
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

    val transformation = statusUpdateTransformer.transformJpaToApi(jpaEntity)

    Assertions.assertThat(transformation).isEqualTo(expectedRepresentation)
  }
}
