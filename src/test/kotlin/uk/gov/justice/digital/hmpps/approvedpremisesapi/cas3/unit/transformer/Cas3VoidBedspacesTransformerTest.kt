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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3VoidBedspaceReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3VoidBedspaceReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3CostCentre
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3VoidBedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3VoidBedspaceReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3VoidBedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3VoidBedspaceReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3VoidBedspacesTransformer
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas3VoidBedspacesTransformerTest {
  @MockK
  lateinit var cas3VoidBedspaceReasonTransformer: Cas3VoidBedspaceReasonTransformer

  @InjectMockKs
  lateinit var cas3VoidBedspacesTransformer: Cas3VoidBedspacesTransformer

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
