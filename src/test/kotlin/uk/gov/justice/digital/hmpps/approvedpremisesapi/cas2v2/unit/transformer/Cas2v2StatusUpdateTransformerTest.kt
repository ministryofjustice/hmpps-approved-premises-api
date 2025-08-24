package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.unit.transformer

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2StatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2StatusUpdateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2User
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2StatusUpdateDetailEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.factory.Cas2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.reporting.model.reference.Cas2ApplicationStatusSeeding
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer.Cas2v2StatusUpdateTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer.Cas2v2UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2v2.transformer.transformCas2UserEntityToNomisUserEntity
import java.time.OffsetDateTime
import java.util.UUID

class Cas2v2StatusUpdateTransformerTest {
  private val user = Cas2UserEntityFactory().produce()
  private val cas2v2SubmittedApplication = Cas2ApplicationEntityFactory()
    .withCreatedByUser(
      transformCas2UserEntityToNomisUserEntity(user),
    )
    .withCreatedByCas2User(user)
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

    val jpaEntity = Cas2StatusUpdateEntityFactory()
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
    val mockStatusUpdate = mockk<Cas2StatusUpdateEntity>()

    val statusId = UUID.fromString("f5cd423b-08eb-4efb-96ff-5cc6bb073905")
    every { mockStatusUpdate.statusId } returns statusId

    val cas2v2StatusUpdateDetail = Cas2v2StatusUpdateDetail(
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
    val transformation = cas2v2StatusUpdateTransformer.transformStatusUpdateDetailsJpaToApi(updateDetail)

    Assertions.assertThat(transformation).isEqualTo(cas2v2StatusUpdateDetail)
  }

  // TODO besscerule is the below test no longer relevant as we are only using CAS 2 update details now

//  @Test
//  fun `test transformStatusUpdateDetailsJpaToApi rejects a CAS2 update detail`() {
//    val mockStatusUpdate = mockk<Cas2StatusUpdateEntity>()
//
//    val statusId = UUID.fromString("f5cd423b-08eb-4efb-96ff-5cc6bb073905")
//    every { mockStatusUpdate.statusId } returns statusId
//
//    val updateDetail = Cas2StatusUpdateDetailEntityFactory()
//      .withId(UUID.fromString("fabbb8c0-344e-4a9d-a964-7987b22d09c6"))
//      .withLabel("Personal information")
//      .withStatusDetailId(UUID.fromString("fabbb8c0-344e-4a9d-a964-7987b22d09c6"))
//      .withStatusUpdate(mockStatusUpdate)
//      .produce()
//
//    assertThrows<IllegalStateException> {
//      cas2v2StatusUpdateTransformer.transformStatusUpdateDetailsJpaToApi(updateDetail)
//    }
//  }
}
