package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.unit.transformer

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ApplicationStatusSeeding
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2StatusUpdateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.Cas2StatusUpdateTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.transformer.Cas2UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2StatusUpdateDetailEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2StatusUpdateEntity
import java.time.OffsetDateTime
import java.util.UUID

class Cas2v2StatusUpdateTransformerTest {
  private val user = Cas2UserEntityFactory()
    .withServiceOrigin(Cas2ServiceOrigin.BAIL)
    .produce()
  private val cas2v2SubmittedApplication = Cas2ApplicationEntityFactory()
    .withServiceOrigin(Cas2ServiceOrigin.BAIL)
    .withCreatedByUser(user)
    .withSubmittedAt(OffsetDateTime.now())
    .produce()

  private val mockCas2UserApi = mockk<Cas2User>()
  private val mockCas2UserTransformer = mockk<Cas2UserTransformer>()
  private val cas2StatusUpdateTransformer = Cas2StatusUpdateTransformer(mockCas2UserTransformer)

  @BeforeEach
  fun setup() {
    every { mockCas2UserTransformer.transformJpaToApi(any()) } returns mockCas2UserApi
  }

  @Test
  fun `transforms JPA Cas2v2StatusUpdate db entity to API representation`() {
    val status = Cas2ApplicationStatusSeeding.statusList(ServiceName.cas2v2).random()
    val assessor = Cas2UserEntityFactory()
      .withServiceOrigin(Cas2ServiceOrigin.BAIL)
      .produce()
    val jpaEntity = Cas2StatusUpdateEntityFactory()
      .withStatusId(status.id)
      .withApplication(cas2v2SubmittedApplication)
      .withAssessor(assessor)
      .produce()

    val expectedRepresentation = Cas2StatusUpdate(
      id = jpaEntity.id,
      name = status.name,
      label = jpaEntity.label,
      description = jpaEntity.description,
      updatedBy = mockCas2UserApi,
      updatedAt = jpaEntity.createdAt.toInstant(),
      statusUpdateDetails = null,
    )

    val transformation = cas2StatusUpdateTransformer.transformJpaToApi(jpaEntity)

    Assertions.assertThat(transformation).isEqualTo(expectedRepresentation)
  }

  @Test
  fun `test transformStatusUpdateDetailsJpaToApi accepts a CAS2v2 update detail`() {
    val mockStatusUpdate = mockk<Cas2StatusUpdateEntity>()

    val statusId = UUID.fromString("f5cd423b-08eb-4efb-96ff-5cc6bb073905")
    every { mockStatusUpdate.statusId } returns statusId

    val cas2StatusUpdateDetail = Cas2StatusUpdateDetail(
      id = UUID.fromString("3df29b1b-e2fc-4df7-b4b8-0527cd9e3a6f"),
      name = "applicantDetails",
      label = "Applicant details",
    )

    val updateDetail = Cas2StatusUpdateDetailEntityFactory()
      .withId(UUID.fromString("3df29b1b-e2fc-4df7-b4b8-0527cd9e3a6f"))
      .withLabel("Applicant details")
      .withStatusDetailId(UUID.fromString("3df29b1b-e2fc-4df7-b4b8-0527cd9e3a6f"))
      .withStatusUpdate(mockStatusUpdate)
      .produce()
    val transformation = cas2StatusUpdateTransformer.transformStatusUpdateDetailsJpaToApi(updateDetail)

    Assertions.assertThat(transformation).isEqualTo(cas2StatusUpdateDetail)
  }
}
