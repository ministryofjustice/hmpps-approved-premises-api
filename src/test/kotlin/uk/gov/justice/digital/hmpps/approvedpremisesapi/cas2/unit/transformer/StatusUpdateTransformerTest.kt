package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.transformer

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ExternalUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.reporting.model.reference.Cas2ApplicationStatusSeeding
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.ExternalUserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.StatusUpdateTransformer
import java.time.OffsetDateTime

class StatusUpdateTransformerTest {
  private val user = Cas2UserEntityFactory().withUserType(Cas2UserType.NOMIS).produce()
  private val cas2User = Cas2UserEntityFactory()
    .withUserType(Cas2UserType.DELIUS)
    .produce()

  private val submittedApplicationWithNomisUser = Cas2ApplicationEntityFactory()
    .withCreatedByUser(user)
    .withSubmittedAt(OffsetDateTime.now())
    .produce()

  private val submittedApplicationWithCas2UserDelius = Cas2ApplicationEntityFactory()
    .withCreatedByUser(cas2User)
    .withSubmittedAt(OffsetDateTime.now())
    .produce()

  private val mockExternalUserApi = mockk<ExternalUser>()
  private val mockExternalUserTransformer = mockk<ExternalUserTransformer>()
  private val statusUpdateTransformer = StatusUpdateTransformer(mockExternalUserTransformer)

  @BeforeEach
  fun setup() {
    every { mockExternalUserTransformer.transformJpaToApi(any()) } returns mockExternalUserApi
  }

  fun `transforms JPA Cas2StatusUpdate db entity to API representation with application submitted by NomisUser`() {
    val status = Cas2ApplicationStatusSeeding.statusList(ServiceName.cas2).random()

    val jpaEntity = Cas2StatusUpdateEntityFactory()
      .withStatusId(status.id)
      .withApplication(submittedApplicationWithNomisUser)
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

  @Test
  fun `transforms JPA Cas2StatusUpdate db entity to API representation with application submitted by Cas2User of type delius`() {
    val status = Cas2ApplicationStatusSeeding.statusList(ServiceName.cas2).random()

    val jpaEntity = Cas2StatusUpdateEntityFactory()
      .withStatusId(status.id)
      .withApplication(submittedApplicationWithCas2UserDelius)
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
