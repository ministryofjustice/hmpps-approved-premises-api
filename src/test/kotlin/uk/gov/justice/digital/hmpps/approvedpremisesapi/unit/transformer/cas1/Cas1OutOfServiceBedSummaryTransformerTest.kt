package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedRevisionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OutOfServiceBedReasonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1OutOfServiceBedSummaryTransformer
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1OutOfServiceBedSummaryTransformerTest {

  private val cas1OutOfServiceBedReasonTransformer = mockk<Cas1OutOfServiceBedReasonTransformer>()

  private val transformer = Cas1OutOfServiceBedSummaryTransformer(
    cas1OutOfServiceBedReasonTransformer,
  )

  @Test
  fun toCas1OutOfServiceBedSummary() {
    val roomCharacteristic = CharacteristicEntity(
      id = UUID.randomUUID(),
      propertyName = "hasLift",
      name = "hasLift",
      serviceScope = "approved-premises",
      modelScope = "room",
      isActive = true,
    )
    val room = RoomEntityFactory()
      .withName("BedRoom 1")
      .withCharacteristics(mutableListOf(roomCharacteristic))
      .withDefaults()
      .produce()
    val startDate = LocalDate.now().plusDays(1)
    val endDate = startDate.plusDays(10)
    val bed = BedEntityFactory()
      .withDefaults()
      .withRoom(room)
      .produce()
    val outOfServiceReason = Cas1OutOfServiceBedReasonEntityFactory()
      .produce()

    val expectedReason = Cas1OutOfServiceBedReason(
      id = outOfServiceReason.id,
      name = outOfServiceReason.name,
      isActive = true,
    )

    val outOfServiceBed = Cas1OutOfServiceBedEntityFactory()
      .withBed(bed)
      .produce()
      .apply {
        this.revisionHistory += Cas1OutOfServiceBedRevisionEntityFactory()
          .withOutOfServiceBed(this)
          .withStartDate(startDate)
          .withEndDate(endDate)
          .withReason(outOfServiceReason)
          .produce()
      }

    every { cas1OutOfServiceBedReasonTransformer.transformJpaToApi(any()) } returns expectedReason

    val result = transformer.toCas1OutOfServiceBedSummary(outOfServiceBed)

    assertThat(result.id).isEqualTo(outOfServiceBed.id)
    assertThat(result.roomName).isEqualTo(bed.room.name)
    assertThat(result.startDate).isEqualTo(outOfServiceBed.startDate)
    assertThat(result.endDate).isEqualTo(outOfServiceBed.endDate)
    assertThat(result.characteristics.size).isEqualTo(1)
    assertThat(result.characteristics[0].name).isEqualTo(roomCharacteristic.name)
    val reason = result.reason
    assertThat(reason.id).isEqualTo(expectedReason.id)
    assertThat(reason.name).isEqualTo(expectedReason.name)
    assertThat(reason.isActive).isEqualTo(expectedReason.isActive)
  }
}
