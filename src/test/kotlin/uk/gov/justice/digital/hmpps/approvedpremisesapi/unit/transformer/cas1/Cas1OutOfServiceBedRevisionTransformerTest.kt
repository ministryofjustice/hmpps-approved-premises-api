package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedRevisionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRevisionChangeType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.UserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OutOfServiceBedReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OutOfServiceBedRevisionTransformer
import java.util.EnumSet
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedRevisionType as ApiRevisionType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRevisionType as DomainRevisionType

class Cas1OutOfServiceBedRevisionTransformerTest {
  private val cas1OutOfServiceBedReasonTransformer = mockk<Cas1OutOfServiceBedReasonTransformer>()
  private val userTransformer = mockk<UserTransformer>()

  private val transformer = Cas1OutOfServiceBedRevisionTransformer(
    cas1OutOfServiceBedReasonTransformer,
    userTransformer,
  )

  private val detailsFactory = Cas1OutOfServiceBedRevisionEntityFactory()
    .withOutOfServiceBed {
      withBed {
        withRoom {
          withPremises(
            ApprovedPremisesEntityFactory()
              .withDefaults()
              .produce(),
          )
        }
      }
    }

  private val expectedReason = Cas1OutOfServiceBedReason(
    id = UUID.randomUUID(),
    name = "Some Reason",
    isActive = true,
  )

  private val expectedUser = ApprovedPremisesUser(
    qualifications = listOf(),
    roles = listOf(),
    apArea = ApArea(
      id = UUID.randomUUID(),
      identifier = "APA",
      name = "AP Area",
    ),
    service = ServiceName.approvedPremises.value,
    id = UUID.randomUUID(),
    name = "Some User",
    deliusUsername = "SOMEUSER",
    region = ProbationRegion(
      id = UUID.randomUUID(),
      name = "Some Probation Region",
    ),
  )

  @BeforeEach
  fun setup() {
    every { cas1OutOfServiceBedReasonTransformer.transformJpaToApi(any()) } returns expectedReason
    every { userTransformer.transformJpaToApi(any(), any()) } returns expectedUser
  }

  @Test
  fun `transformJpaToApi transforms correctly when the details are the INITIAL type`() {
    val details = detailsFactory
      .withDetailType(DomainRevisionType.INITIAL)
      .produce()

    val result = transformer.transformJpaToApi(details)

    assertThat(result.id).isEqualTo(details.id)
    assertThat(result.updatedAt).isEqualTo(details.createdAt.toInstant())
    assertThat(result.updatedBy).isEqualTo(expectedUser)
    assertThat(result.revisionType).isEqualTo(listOf(ApiRevisionType.created))
    assertThat(result.outOfServiceFrom).isEqualTo(details.startDate)
    assertThat(result.outOfServiceTo).isEqualTo(details.endDate)
    assertThat(result.reason).isEqualTo(expectedReason)
    assertThat(result.referenceNumber).isEqualTo(details.referenceNumber)
    assertThat(result.notes).isEqualTo(details.notes)
  }

  @Test
  fun `transformJpaToApi transforms correctly when the details are the UPDATE type and the change type is START_DATE`() {
    val details = detailsFactory
      .withDetailType(DomainRevisionType.UPDATE)
      .withChangeType(EnumSet.of(Cas1OutOfServiceBedRevisionChangeType.START_DATE))
      .produce()

    val result = transformer.transformJpaToApi(details)

    assertThat(result.id).isEqualTo(details.id)
    assertThat(result.updatedAt).isEqualTo(details.createdAt.toInstant())
    assertThat(result.updatedBy).isEqualTo(expectedUser)
    assertThat(result.revisionType).isEqualTo(listOf(ApiRevisionType.updatedStartDate))
    assertThat(result.outOfServiceFrom).isEqualTo(details.startDate)
    assertThat(result.outOfServiceTo).isNull()
    assertThat(result.reason).isNull()
    assertThat(result.referenceNumber).isNull()
    assertThat(result.notes).isNull()
  }

  @Test
  fun `transformJpaToApi transforms correctly when the details are the UPDATE type and the change type is END_DATE`() {
    val details = detailsFactory
      .withDetailType(DomainRevisionType.UPDATE)
      .withChangeType(EnumSet.of(Cas1OutOfServiceBedRevisionChangeType.END_DATE))
      .produce()

    val result = transformer.transformJpaToApi(details)

    assertThat(result.id).isEqualTo(details.id)
    assertThat(result.updatedAt).isEqualTo(details.createdAt.toInstant())
    assertThat(result.updatedBy).isEqualTo(expectedUser)
    assertThat(result.revisionType).isEqualTo(listOf(ApiRevisionType.updatedEndDate))
    assertThat(result.outOfServiceFrom).isNull()
    assertThat(result.outOfServiceTo).isEqualTo(details.endDate)
    assertThat(result.reason).isNull()
    assertThat(result.referenceNumber).isNull()
    assertThat(result.notes).isNull()
  }

  @Test
  fun `transformJpaToApi transforms correctly when the details are the UPDATE type and the change type is REFERENCE_NUMBER`() {
    val details = detailsFactory
      .withDetailType(DomainRevisionType.UPDATE)
      .withChangeType(EnumSet.of(Cas1OutOfServiceBedRevisionChangeType.REFERENCE_NUMBER))
      .produce()

    val result = transformer.transformJpaToApi(details)

    assertThat(result.id).isEqualTo(details.id)
    assertThat(result.updatedAt).isEqualTo(details.createdAt.toInstant())
    assertThat(result.updatedBy).isEqualTo(expectedUser)
    assertThat(result.revisionType).isEqualTo(listOf(ApiRevisionType.updatedReferenceNumber))
    assertThat(result.outOfServiceFrom).isNull()
    assertThat(result.outOfServiceTo).isNull()
    assertThat(result.reason).isNull()
    assertThat(result.referenceNumber).isEqualTo(details.referenceNumber)
    assertThat(result.notes).isNull()
  }

  @Test
  fun `transformJpaToApi transforms correctly when the details are the UPDATE type and the change type is REASON`() {
    val details = detailsFactory
      .withDetailType(DomainRevisionType.UPDATE)
      .withChangeType(EnumSet.of(Cas1OutOfServiceBedRevisionChangeType.REASON))
      .produce()

    val result = transformer.transformJpaToApi(details)

    assertThat(result.id).isEqualTo(details.id)
    assertThat(result.updatedAt).isEqualTo(details.createdAt.toInstant())
    assertThat(result.updatedBy).isEqualTo(expectedUser)
    assertThat(result.revisionType).isEqualTo(listOf(ApiRevisionType.updatedReason))
    assertThat(result.outOfServiceFrom).isNull()
    assertThat(result.outOfServiceTo).isNull()
    assertThat(result.reason).isEqualTo(expectedReason)
    assertThat(result.referenceNumber).isNull()
    assertThat(result.notes).isNull()
  }

  @Test
  fun `transformJpaToApi transforms correctly when the details are the UPDATE type and the change type is NOTES`() {
    val details = detailsFactory
      .withDetailType(DomainRevisionType.UPDATE)
      .withChangeType(EnumSet.of(Cas1OutOfServiceBedRevisionChangeType.NOTES))
      .produce()

    val result = transformer.transformJpaToApi(details)

    assertThat(result.id).isEqualTo(details.id)
    assertThat(result.updatedAt).isEqualTo(details.createdAt.toInstant())
    assertThat(result.updatedBy).isEqualTo(expectedUser)
    assertThat(result.revisionType).isEqualTo(listOf(ApiRevisionType.updatedNotes))
    assertThat(result.outOfServiceFrom).isNull()
    assertThat(result.outOfServiceTo).isNull()
    assertThat(result.reason).isNull()
    assertThat(result.referenceNumber).isNull()
    assertThat(result.notes).isEqualTo(details.notes)
  }

  @Test
  fun `transformJpaToApi transforms correctly when the details are the UPDATE type and the change type is multiple types`() {
    val details = detailsFactory
      .withDetailType(DomainRevisionType.UPDATE)
      .withChangeType(EnumSet.allOf(Cas1OutOfServiceBedRevisionChangeType::class.java))
      .produce()

    val result = transformer.transformJpaToApi(details)

    assertThat(result.id).isEqualTo(details.id)
    assertThat(result.updatedAt).isEqualTo(details.createdAt.toInstant())
    assertThat(result.updatedBy).isEqualTo(expectedUser)
    assertThat(result.revisionType).containsExactlyInAnyOrder(
      ApiRevisionType.updatedStartDate,
      ApiRevisionType.updatedEndDate,
      ApiRevisionType.updatedReferenceNumber,
      ApiRevisionType.updatedReason,
      ApiRevisionType.updatedNotes,
    )
    assertThat(result.outOfServiceFrom).isEqualTo(details.startDate)
    assertThat(result.outOfServiceTo).isEqualTo(details.endDate)
    assertThat(result.reason).isEqualTo(expectedReason)
    assertThat(result.referenceNumber).isEqualTo(details.referenceNumber)
    assertThat(result.notes).isEqualTo(details.notes)
  }
}
