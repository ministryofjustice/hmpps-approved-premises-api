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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3VoidBedspaceCancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3VoidBedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3VoidBedspaceReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3VoidBedspaceCancellationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3VoidBedspaceReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas3.Cas3VoidBedspacesTransformer
import java.time.Instant

class Cas3VoidBedspacesTransformerTest {
  private val cas3VoidBedspaceReasonTransformer = mockk<Cas3VoidBedspaceReasonTransformer>()
  private val cas3VoidBedspaceCancellationTransformer = mockk<Cas3VoidBedspaceCancellationTransformer>()

  private val cas3VoidBedspacesTransformer = Cas3VoidBedspacesTransformer(cas3VoidBedspaceReasonTransformer, cas3VoidBedspaceCancellationTransformer)

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

  private val voidBedspace = Cas3VoidBedspaceEntityFactory()
    .withYieldedReason {
      Cas3VoidBedspaceReasonEntityFactory()
        .produce()
    }
    .withYieldedPremises { premises }
    .withYieldedBed { bed }
    .produce()

  @Test
  fun `Void Bedspace entity is correctly transformed`() {
    every { cas3VoidBedspaceReasonTransformer.transformJpaToApi(voidBedspace.reason) } returns LostBedReason(
      id = voidBedspace.reason.id,
      name = voidBedspace.reason.name,
      isActive = true,
      serviceScope = "approved-premises",
    )

    val result = cas3VoidBedspacesTransformer.transformJpaToApi(voidBedspace)

    assertThat(result.id).isEqualTo(voidBedspace.id)
    assertThat(result.startDate).isEqualTo(voidBedspace.startDate)
    assertThat(result.endDate).isEqualTo(voidBedspace.endDate)
    assertThat(result.reason.id).isEqualTo(voidBedspace.reason.id)
    assertThat(result.notes).isEqualTo(voidBedspace.notes)
    assertThat(result.referenceNumber).isEqualTo(voidBedspace.referenceNumber)
    assertThat(result.status).isEqualTo(LostBedStatus.active)
    assertThat(result.cancellation).isNull()
    assertThat(result.bedId).isEqualTo(bed.id)
    assertThat(result.bedName).isEqualTo(bed.name)
    assertThat(result.roomName).isEqualTo(room.name)
  }

  @Test
  fun `A cancelled void bedspace entity is correctly transformed`() {
    val lostBedCancellation = Cas3VoidBedspaceCancellationEntityFactory()
      .withYieldedVoidBedspace { voidBedspace }
      .produce()

    voidBedspace.cancellation = lostBedCancellation

    every { cas3VoidBedspaceReasonTransformer.transformJpaToApi(voidBedspace.reason) } returns LostBedReason(
      id = voidBedspace.reason.id,
      name = voidBedspace.reason.name,
      isActive = true,
      serviceScope = "approved-premises",
    )

    val now = Instant.now()
    every { cas3VoidBedspaceCancellationTransformer.transformJpaToApi(voidBedspace.cancellation!!) } returns LostBedCancellation(
      id = voidBedspace.cancellation!!.id,
      createdAt = now,
      notes = "Some notes",
    )

    val result = cas3VoidBedspacesTransformer.transformJpaToApi(voidBedspace)

    assertThat(result.id).isEqualTo(voidBedspace.id)
    assertThat(result.startDate).isEqualTo(voidBedspace.startDate)
    assertThat(result.endDate).isEqualTo(voidBedspace.endDate)
    assertThat(result.reason.id).isEqualTo(voidBedspace.reason.id)
    assertThat(result.notes).isEqualTo(voidBedspace.notes)
    assertThat(result.referenceNumber).isEqualTo(voidBedspace.referenceNumber)
    assertThat(result.status).isEqualTo(LostBedStatus.cancelled)
    assertThat(result.cancellation).isNotNull
    assertThat(result.cancellation!!.id).isEqualTo(voidBedspace.cancellation!!.id)
    assertThat(result.cancellation!!.createdAt).isEqualTo(now)
    assertThat(result.cancellation!!.notes).isEqualTo("Some notes")
    assertThat(result.bedId).isEqualTo(bed.id)
    assertThat(result.bedName).isEqualTo(bed.name)
    assertThat(result.roomName).isEqualTo(room.name)
  }
}
