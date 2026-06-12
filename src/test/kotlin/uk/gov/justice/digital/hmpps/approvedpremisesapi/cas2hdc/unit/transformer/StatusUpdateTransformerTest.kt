package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.unit.transformer

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ExternalUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.dto.Cas2HdcStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.Cas2HdcExternalUserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.transformer.Cas2HdcStatusUpdateTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ApplicationStatusSeeding
import java.time.OffsetDateTime

class StatusUpdateTransformerTest {
  private val user = Cas2UserEntityFactory().produce()
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
  private val mockCas2HdcExternalUserTransformer = mockk<Cas2HdcExternalUserTransformer>()
  private val cas2HdcStatusUpdateTransformer = Cas2HdcStatusUpdateTransformer(mockCas2HdcExternalUserTransformer)

  @BeforeEach
  fun setup() {
    every { mockCas2HdcExternalUserTransformer.transformJpaToApi(ofType()) } returns mockExternalUserApi
  }

  fun `transforms JPA Cas2StatusUpdate db entity to API representation with application submitted by NomisUser`() {
    val status = Cas2ApplicationStatusSeeding.statusList(ServiceName.cas2).random()

    val jpaEntity = Cas2StatusUpdateEntityFactory()
      .withStatusId(status.id)
      .withApplication(submittedApplicationWithNomisUser)
      .produce()

    val expectedRepresentation = Cas2HdcStatusUpdate(
      id = jpaEntity.id,
      name = status.name,
      label = jpaEntity.label,
      description = jpaEntity.description,
      updatedBy = mockExternalUserApi,
      updatedAt = jpaEntity.createdAt.toInstant(),
      statusUpdateDetails = null,
    )

    val transformation = cas2HdcStatusUpdateTransformer.transformJpaToApi(jpaEntity)

    Assertions.assertThat(transformation).isEqualTo(expectedRepresentation)
  }

  @Test
  fun `transforms JPA Cas2StatusUpdate db entity to API representation with application submitted by Cas2User of type delius`() {
    val status = Cas2ApplicationStatusSeeding.statusList(ServiceName.cas2).random()
    val assessor = Cas2UserEntityFactory().withUserType(Cas2UserType.EXTERNAL).produce()
    val jpaEntity = Cas2StatusUpdateEntityFactory()
      .withStatusId(status.id)
      .withApplication(submittedApplicationWithCas2UserDelius)
      .withAssessor(assessor)
      .produce()

    val expectedRepresentation = Cas2HdcStatusUpdate(
      id = jpaEntity.id,
      name = status.name,
      label = jpaEntity.label,
      description = jpaEntity.description,
      updatedBy = mockExternalUserApi,
      updatedAt = jpaEntity.createdAt.toInstant(),
      statusUpdateDetails = null,
    )

    val transformation = cas2HdcStatusUpdateTransformer.transformJpaToApi(jpaEntity)

    Assertions.assertThat(transformation).isEqualTo(expectedRepresentation)
  }
}
