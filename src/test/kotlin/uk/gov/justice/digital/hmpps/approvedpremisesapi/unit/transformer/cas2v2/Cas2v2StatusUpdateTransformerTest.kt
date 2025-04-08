package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas2v2

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2StatusUpdateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2v2.Cas2v2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2v2.Cas2v2StatusUpdateDetailEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2v2.Cas2v2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2v2.Cas2v2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2ApplicationStatusSeeding
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2.Cas2v2StatusUpdateTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2.Cas2v2UserTransformer
import java.time.OffsetDateTime
import java.util.UUID

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
    val status = Cas2ApplicationStatusSeeding.statusList(ServiceName.cas2v2).random()

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

  @Test
  fun `test transformStatusUpdateDetailsJpaToApi accepts a CAS2v2 update detail`() {
    val mockStatusUpdate = mockk<Cas2v2StatusUpdateEntity>()

    val statusId = UUID.fromString("f5cd423b-08eb-4efb-96ff-5cc6bb073905")
    every { mockStatusUpdate.statusId } returns statusId

    val cas2v2StatusUpdateDetail = Cas2v2StatusUpdateDetail(
      id = UUID.fromString("3df29b1b-e2fc-4df7-b4b8-0527cd9e3a6f"),
      name = "applicantDetails",
      label = "Applicant details",
    )

    val updateDetail = Cas2v2StatusUpdateDetailEntityFactory()
      .withId(UUID.fromString("3df29b1b-e2fc-4df7-b4b8-0527cd9e3a6f"))
      .withLabel("Applicant details")
      .withStatusDetailId(UUID.fromString("3df29b1b-e2fc-4df7-b4b8-0527cd9e3a6f"))
      .withStatusUpdate(mockStatusUpdate)
      .produce()
    val transformation = cas2v2StatusUpdateTransformer.transformStatusUpdateDetailsJpaToApi(updateDetail)

    Assertions.assertThat(transformation).isEqualTo(cas2v2StatusUpdateDetail)
  }

  @Test
  fun `test transformStatusUpdateDetailsJpaToApi rejects a CAS2 update detail`() {
    val mockStatusUpdate = mockk<Cas2v2StatusUpdateEntity>()

    val statusId = UUID.fromString("f5cd423b-08eb-4efb-96ff-5cc6bb073905")
    every { mockStatusUpdate.statusId } returns statusId

    val updateDetail = Cas2v2StatusUpdateDetailEntityFactory()
      .withId(UUID.fromString("fabbb8c0-344e-4a9d-a964-7987b22d09c6"))
      .withLabel("Personal information")
      .withStatusDetailId(UUID.fromString("fabbb8c0-344e-4a9d-a964-7987b22d09c6"))
      .withStatusUpdate(mockStatusUpdate)
      .produce()

    assertThrows<IllegalStateException> {
      cas2v2StatusUpdateTransformer.transformStatusUpdateDetailsJpaToApi(updateDetail)
    }
  }
}
