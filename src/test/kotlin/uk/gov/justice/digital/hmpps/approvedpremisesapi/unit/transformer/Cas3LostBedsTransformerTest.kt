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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3LostBedCancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3LostBedReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3LostBedsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3LostBedCancellationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3LostBedReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3LostBedsTransformer
import java.time.Instant

class Cas3LostBedsTransformerTest {
  private val cas3LostBedReasonTransformer = mockk<Cas3LostBedReasonTransformer>()
  private val cas3LostBedCancellationTransformer = mockk<Cas3LostBedCancellationTransformer>()

  private val cas3LostBedsTransformer = Cas3LostBedsTransformer(cas3LostBedReasonTransformer, cas3LostBedCancellationTransformer)

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

  val room = RoomEntityFactory()
    .withYieldedPremises { premises }
    .produce()

  val bed = BedEntityFactory()
    .withYieldedRoom { room }
    .produce()

  private val lostBed = Cas3LostBedsEntityFactory()
    .withYieldedReason {
      Cas3LostBedReasonEntityFactory()
        .produce()
    }
    .withYieldedPremises { premises }
    .withYieldedBed { bed }
    .produce()

  @Test
  fun `Lost Bed entity is correctly transformed`() {
    every { cas3LostBedReasonTransformer.transformJpaToApi(lostBed.reason) } returns LostBedReason(
      id = lostBed.reason.id,
      name = lostBed.reason.name,
      isActive = true,
      serviceScope = "approved-premises",
    )

    val result = cas3LostBedsTransformer.transformJpaToApi(lostBed)

    assertThat(result.id).isEqualTo(lostBed.id)
    assertThat(result.startDate).isEqualTo(lostBed.startDate)
    assertThat(result.endDate).isEqualTo(lostBed.endDate)
    assertThat(result.reason.id).isEqualTo(lostBed.reason.id)
    assertThat(result.notes).isEqualTo(lostBed.notes)
    assertThat(result.referenceNumber).isEqualTo(lostBed.referenceNumber)
    assertThat(result.status).isEqualTo(LostBedStatus.active)
    assertThat(result.cancellation).isNull()
    assertThat(result.bedId).isEqualTo(bed.id)
    assertThat(result.bedName).isEqualTo(bed.name)
    assertThat(result.roomName).isEqualTo(room.name)
  }

  @Test
  fun `A cancelled lost bed entity is correctly transformed`() {
    val lostBedCancellation = Cas3LostBedCancellationEntityFactory()
      .withYieldedLostBed { lostBed }
      .produce()

    lostBed.cancellation = lostBedCancellation

    every { cas3LostBedReasonTransformer.transformJpaToApi(lostBed.reason) } returns LostBedReason(
      id = lostBed.reason.id,
      name = lostBed.reason.name,
      isActive = true,
      serviceScope = "approved-premises",
    )

    val now = Instant.now()
    every { cas3LostBedCancellationTransformer.transformJpaToApi(lostBed.cancellation!!) } returns LostBedCancellation(
      id = lostBed.cancellation!!.id,
      createdAt = now,
      notes = "Some notes",
    )

    val result = cas3LostBedsTransformer.transformJpaToApi(lostBed)

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
    assertThat(result.bedId).isEqualTo(bed.id)
    assertThat(result.bedName).isEqualTo(bed.name)
    assertThat(result.roomName).isEqualTo(room.name)
  }
}
