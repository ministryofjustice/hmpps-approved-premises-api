package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.transformer

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3VoidBedspaceCancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3VoidBedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3VoidBedspaceReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3CostCentre
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3VoidBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3VoidBedspaceReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3VoidBedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3VoidBedspaceCancellationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3VoidBedspaceReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3VoidBedspacesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas3VoidBedspacesTransformerTest {
  @MockK
  lateinit var cas3VoidBedspaceReasonTransformer: Cas3VoidBedspaceReasonTransformer

  @MockK
  lateinit var cas3VoidBedspaceCancellationTransformer: Cas3VoidBedspaceCancellationTransformer

  @InjectMockKs
  lateinit var cas3VoidBedspacesTransformer: Cas3VoidBedspacesTransformer

  @Nested
  inner class V1TransformerTests {

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
      .produce().apply {
        costCentre = Cas3CostCentre.HMPPS
      }

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
      assertThat(result.costCentre).isEqualTo(voidBedspace.costCentre)
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
      assertThat(result.costCentre).isEqualTo(voidBedspace.costCentre)
    }
  }

  @Nested
  inner class Cas3v2VoidBedspaces {

    @BeforeEach
    fun setup() {
      reason = Cas3VoidBedspaceReasonEntityFactory().produce()
      every { cas3VoidBedspaceReasonTransformer.toCas3VoidBedspaceReason(any()) } returns Cas3VoidBedspaceReason(
        reason.id,
        reason.name,
        reason.isActive,
      )
    }

    lateinit var reason: Cas3VoidBedspaceReasonEntity

    private fun createVoidBedspaceEntity(
      cancellationDate: OffsetDateTime? = null,
      cancellationNotes: String? = null,
    ) = Cas3VoidBedspaceEntity(
      id = UUID.randomUUID(),
      startDate = LocalDate.now(),
      endDate = LocalDate.now().plusDays(100),
      bedspace = Cas3BedspaceEntityFactory().produce(),
      reason = reason,
      referenceNumber = "REFERENCE",
      notes = "NOTES",
      cancellationDate = cancellationDate,
      cancellationNotes = cancellationNotes,
      premises = null,
      cancellation = null,
      bed = null,
      costCentre = Cas3CostCentre.HMPPS,
    )

    @Test
    fun `active void bedspace is correctly transformed`() {
      val voidBedspaceEntity = createVoidBedspaceEntity()
      val transformed = cas3VoidBedspacesTransformer.toCas3VoidBedspace(voidBedspaceEntity)
      assertCommonFields(transformed, voidBedspaceEntity)
      assertThat(transformed.status).isEqualTo(Cas3VoidBedspaceStatus.ACTIVE)
      assertThat(transformed.reason.isActive).isEqualTo(voidBedspaceEntity.reason.isActive)
      assertThat(transformed.cancellationDate).isNull()
      assertThat(transformed.cancellationNotes).isNull()
    }

    @Test
    fun `cancelled void bedspace is correctly transformed`() {
      val cancelledDate = OffsetDateTime.now()
      val cancelledNotes = "some cancellations notes"
      val voidBedspaceEntity =
        createVoidBedspaceEntity(cancellationDate = cancelledDate, cancellationNotes = cancelledNotes)

      val transformed = cas3VoidBedspacesTransformer.toCas3VoidBedspace(voidBedspaceEntity)
      assertCommonFields(transformed, voidBedspaceEntity)
      assertThat(transformed.status).isEqualTo(Cas3VoidBedspaceStatus.CANCELLED)
      assertThat(transformed.reason.isActive).isEqualTo(voidBedspaceEntity.reason.isActive)
      assertThat(transformed.cancellationDate).isEqualTo(cancelledDate.toLocalDate())
      assertThat(transformed.cancellationNotes).isEqualTo(cancelledNotes)
    }

    fun assertCommonFields(transformed: Cas3VoidBedspace, voidBedspaceEntity: Cas3VoidBedspaceEntity) {
      assertAll({
        assertThat(transformed.id).isEqualTo(voidBedspaceEntity.id)
        assertThat(transformed.startDate).isEqualTo(voidBedspaceEntity.startDate)
        assertThat(transformed.endDate).isEqualTo(voidBedspaceEntity.endDate)
        assertThat(transformed.bedspaceId).isEqualTo(voidBedspaceEntity.bedspace!!.id)
        assertThat(transformed.bedspaceName).isEqualTo(voidBedspaceEntity.bedspace!!.reference)
        assertThat(transformed.reason.id).isEqualTo(voidBedspaceEntity.reason.id)
        assertThat(transformed.reason.name).isEqualTo(voidBedspaceEntity.reason.name)
        assertThat(transformed.referenceNumber).isEqualTo(voidBedspaceEntity.referenceNumber)
        assertThat(transformed.notes).isEqualTo(voidBedspaceEntity.notes)
        assertThat(transformed.costCentre).isEqualTo(voidBedspaceEntity.costCentre)
      })
    }
  }
}
