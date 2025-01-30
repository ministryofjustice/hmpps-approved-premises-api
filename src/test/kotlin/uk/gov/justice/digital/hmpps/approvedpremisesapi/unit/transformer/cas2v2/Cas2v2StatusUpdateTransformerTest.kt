package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas2v2

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2v2.Cas2v2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2v2.Cas2v2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2v2.Cas2v2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2ApplicationStatusSeeding
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2.Cas2v2StatusUpdateTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2.Cas2v2UserTransformer
import java.time.OffsetDateTime

class Cas2v2StatusUpdateTransformerTest {
  private val user = Cas2v2UserEntityFactory().produce()
  private val cas2v2SubmittedApplication = Cas2v2ApplicationEntityFactory()
    .withCreatedByUser(user)
    .withSubmittedAt(OffsetDateTime.now())
    .produce()

  private val mockCas2v2UserApi = mockk<Cas2v2User>()
  private val mockCas2v2UserTransformer = mockk<Cas2v2UserTransformer>()
  private val cas2v2StatusUpdateTransformer = Cas2v2StatusUpdateTransformer(mockCas2v2UserTransformer)

  @BeforeEach
  fun setup() {
    every { mockCas2v2UserTransformer.transformJpaToApi(any()) } returns mockCas2v2UserApi
  }

  @Test
  fun `transforms JPA Cas2v2StatusUpdate db entity to API representation`() {
    val status = Cas2ApplicationStatusSeeding.statusList().random()

    val jpaEntity = Cas2v2StatusUpdateEntityFactory()
      .withStatusId(status.id)
      .withApplication(cas2v2SubmittedApplication)
      .produce()

    val expectedRepresentation = Cas2v2StatusUpdate(
      id = jpaEntity.id,
      name = status.name,
      label = jpaEntity.label,
      description = jpaEntity.description,
      updatedBy = mockCas2v2UserApi,
      updatedAt = jpaEntity.createdAt.toInstant(),
      statusUpdateDetails = null,
    )

    val transformation = cas2v2StatusUpdateTransformer.transformJpaToApi(jpaEntity)

    Assertions.assertThat(transformation).isEqualTo(expectedRepresentation)
  }
}
