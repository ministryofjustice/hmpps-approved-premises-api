package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DateChange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DateChangeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DateChangeTransformer
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class DateChangeTransformerTest {
  private val dateChangeTransformer = DateChangeTransformer()

  @Test
  fun `transformJpaToApi transforms correctly`() {
    val premises = ApprovedPremisesEntityFactory()
      .withUnitTestControlTestProbationAreaAndLocalAuthority()
      .produce()

    val room = RoomEntityFactory()
      .withPremises(premises)
      .produce()

    val bed = BedEntityFactory()
      .withRoom(room)
      .produce()

    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val booking = BookingEntityFactory()
      .withPremises(premises)
      .withBed(bed)
      .produce()

    val dateChangeEntity = DateChangeEntity(
      id = UUID.fromString("22931e1d-9a8c-4087-90d9-9fd7638837fa"),
      changedAt = OffsetDateTime.parse("2023-07-13T00:00+01:00"),
      previousArrivalDate = LocalDate.parse("2023-07-15"),
      previousDepartureDate = LocalDate.parse("2023-07-17"),
      newArrivalDate = LocalDate.parse("2023-07-18"),
      newDepartureDate = LocalDate.parse("2023-07-20"),
      booking = booking,
      changedByUser = user,
    )

    val result = dateChangeTransformer.transformJpaToApi(dateChangeEntity)

    assertThat(result).isEqualTo(
      DateChange(
        id = dateChangeEntity.id,
        bookingId = booking.id,
        previousArrivalDate = dateChangeEntity.previousArrivalDate,
        newArrivalDate = dateChangeEntity.newArrivalDate,
        previousDepartureDate = dateChangeEntity.previousDepartureDate,
        newDepartureDate = dateChangeEntity.newDepartureDate,
        createdAt = dateChangeEntity.changedAt.toInstant(),
      ),
    )
  }
}
