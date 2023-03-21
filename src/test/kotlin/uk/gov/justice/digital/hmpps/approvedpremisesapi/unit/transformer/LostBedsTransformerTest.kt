package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LostBedCancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LostBedReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LostBedsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LostBedCancellationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LostBedReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.LostBedsTransformer
import java.time.OffsetDateTime

class LostBedsTransformerTest {
  private val lostBedReasonTransformer = mockk<LostBedReasonTransformer>()
  private val lostBedCancellationTransformer = mockk<LostBedCancellationTransformer>()

  private val lostBedsTransformer = LostBedsTransformer(lostBedReasonTransformer, lostBedCancellationTransformer)

  @Test
  fun `Lost Bed entity is correctly transformed`() {
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory()
              .produce()
          }
          .produce()
      }
      .withYieldedLocalAuthorityArea {
        LocalAuthorityEntityFactory()
          .produce()
      }
      .produce()

    val lostBed = LostBedsEntityFactory()
      .withYieldedReason {
        LostBedReasonEntityFactory()
          .produce()
      }
      .withYieldedPremises { premises }
      .withYieldedBed {
        BedEntityFactory()
          .withYieldedRoom {
            RoomEntityFactory()
              .withYieldedPremises { premises }
              .produce()
          }
          .produce()
      }
      .produce()

    every { lostBedReasonTransformer.transformJpaToApi(lostBed.reason) } returns LostBedReason(
      id = lostBed.reason.id,
      name = lostBed.reason.name,
      isActive = true,
      serviceScope = "approved-premises",
    )

    val result = lostBedsTransformer.transformJpaToApi(lostBed)

    assertThat(result.id).isEqualTo(lostBed.id)
    assertThat(result.startDate).isEqualTo(lostBed.startDate)
    assertThat(result.endDate).isEqualTo(lostBed.endDate)
    assertThat(result.reason.id).isEqualTo(lostBed.reason.id)
    assertThat(result.notes).isEqualTo(lostBed.notes)
    assertThat(result.referenceNumber).isEqualTo(lostBed.referenceNumber)
    assertThat(result.status).isEqualTo(LostBedStatus.active)
    assertThat(result.cancellation).isNull()
    assertThat(result.bedId).isEqualTo(lostBed.bed.id)
  }

  @Test
  fun `A cancelled lost bed entity is correctly transformed`() {
    val premises = TemporaryAccommodationPremisesEntityFactory()
      .withYieldedProbationRegion {
        ProbationRegionEntityFactory()
          .withYieldedApArea {
            ApAreaEntityFactory()
              .produce()
          }
          .produce()
      }
      .withYieldedLocalAuthorityArea {
        LocalAuthorityEntityFactory()
          .produce()
      }
      .produce()

    val lostBed = LostBedsEntityFactory()
      .withYieldedReason {
        LostBedReasonEntityFactory()
          .produce()
      }
      .withYieldedPremises { premises }
      .withYieldedBed {
        BedEntityFactory()
          .withYieldedRoom {
            RoomEntityFactory()
              .withYieldedPremises { premises }
              .produce()
          }
          .produce()
      }
      .produce()

    val lostBedCancellation = LostBedCancellationEntityFactory()
      .withYieldedLostBed { lostBed }
      .produce()

    lostBed.cancellation = lostBedCancellation

    every { lostBedReasonTransformer.transformJpaToApi(lostBed.reason) } returns LostBedReason(
      id = lostBed.reason.id,
      name = lostBed.reason.name,
      isActive = true,
      serviceScope = "approved-premises",
    )

    val now = OffsetDateTime.now()
    every { lostBedCancellationTransformer.transformJpaToApi(lostBed.cancellation!!) } returns LostBedCancellation(
      id = lostBed.cancellation!!.id,
      createdAt = now,
      notes = "Some notes",
    )

    val result = lostBedsTransformer.transformJpaToApi(lostBed)

    assertThat(result.id).isEqualTo(lostBed.id)
    assertThat(result.startDate).isEqualTo(lostBed.startDate)
    assertThat(result.endDate).isEqualTo(lostBed.endDate)
    assertThat(result.reason.id).isEqualTo(lostBed.reason.id)
    assertThat(result.notes).isEqualTo(lostBed.notes)
    assertThat(result.referenceNumber).isEqualTo(lostBed.referenceNumber)
    assertThat(result.status).isEqualTo(LostBedStatus.cancelled)
    assertThat(result.cancellation).isNotNull
    assertThat(result.cancellation!!.id).isEqualTo(lostBed.cancellation!!.id)
    assertThat(result.cancellation!!.createdAt).isEqualTo(now)
    assertThat(result.cancellation!!.notes).isEqualTo("Some notes")
    assertThat(result.bedId).isEqualTo(lostBed.bed.id)
  }
}
