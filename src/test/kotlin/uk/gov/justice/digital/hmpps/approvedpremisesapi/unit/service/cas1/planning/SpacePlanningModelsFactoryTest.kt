package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.planning

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedRevisionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1PlanningBedSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRevisionType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_ARSON_SUITABLE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_SINGLE_ROOM
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_STEP_FREE_DESIGNATED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.BedEnded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.BedOutOfService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningModelsFactory
import java.time.Instant
import java.time.LocalDate

class SpacePlanningModelsFactoryTest {

  val factory = SpacePlanningModelsFactory()

  @Nested
  inner class AllBeds {

    @Test
    fun `all room and bed properties including active characteristics are correctly mapped`() {
      val bedSummary = Cas1PlanningBedSummaryFactory()
        .withBedName("the bed name")
        .withRoomName("the room name")
        .withCharacteristicsPropertyNames(
          listOf(
            CAS1_PROPERTY_NAME_STEP_FREE_DESIGNATED,
            CAS1_PROPERTY_NAME_ARSON_SUITABLE,
            CAS1_PROPERTY_NAME_SINGLE_ROOM,
            "not in allow list",
          ),
        ).produce()

      val result = factory.allBeds(listOf(bedSummary))

      assertThat(result).hasSize(1)

      val bed = result[0]
      assertThat(bed.id).isEqualTo(bedSummary.bedId)
      assertThat(bed.label).isEqualTo("the bed name")

      val room = result[0].room
      assertThat(room.id).isEqualTo(bedSummary.roomId)
      assertThat(room.label).isEqualTo("the room name")

      val characteristics = room.characteristics
      assertThat(characteristics).hasSize(3)

      assertThat(characteristics).containsOnly(
        Characteristic(
          label = CAS1_PROPERTY_NAME_STEP_FREE_DESIGNATED,
          propertyName = CAS1_PROPERTY_NAME_STEP_FREE_DESIGNATED,
          weighting = 100,
          singleRoom = false,
        ),
        Characteristic(
          label = CAS1_PROPERTY_NAME_ARSON_SUITABLE,
          propertyName = CAS1_PROPERTY_NAME_ARSON_SUITABLE,
          weighting = 100,
          singleRoom = false,
        ),
        Characteristic(
          label = CAS1_PROPERTY_NAME_SINGLE_ROOM,
          propertyName = CAS1_PROPERTY_NAME_SINGLE_ROOM,
          weighting = 100,
          singleRoom = true,
        ),
      )
    }
  }

  @Nested
  inner class AllBedsDayState {

    @Test
    fun `no beds defined, return empty list`() {
      val result = factory.allBedsDayState(
        day = LocalDate.of(2020, 1, 1),
        beds = emptyList(),
        outOfServiceBedRecordsToConsider = emptyList(),
      )

      assertThat(result).isEmpty()
    }

    @Test
    fun `all room and bed properties including active characteristics are correctly mapped`() {
      val bedSummary = Cas1PlanningBedSummaryFactory()
        .withBedName("the bed name")
        .withRoomName("the room name")
        .withCharacteristicsPropertyNames(
          listOf(
            CAS1_PROPERTY_NAME_STEP_FREE_DESIGNATED,
            CAS1_PROPERTY_NAME_ARSON_SUITABLE,
            CAS1_PROPERTY_NAME_SINGLE_ROOM,
            "not in allow list",
          ),
        ).produce()

      val result = factory.allBedsDayState(
        day = LocalDate.of(2020, 1, 1),
        beds = listOf(bedSummary),
        outOfServiceBedRecordsToConsider = emptyList(),
      )

      assertThat(result).hasSize(1)

      val bedDayState = result[0]
      assertThat(bedDayState.day).isEqualTo(LocalDate.of(2020, 1, 1))
      assertThat(bedDayState.inactiveReason).isNull()

      val bed = bedDayState.bed
      assertThat(bed.id).isEqualTo(bedSummary.bedId)
      assertThat(bed.label).isEqualTo("the bed name")

      val room = bed.room
      assertThat(room.id).isEqualTo(bedSummary.roomId)
      assertThat(room.label).isEqualTo("the room name")

      val characteristics = room.characteristics
      assertThat(characteristics).hasSize(3)

      assertThat(characteristics).containsOnly(
        Characteristic(
          label = CAS1_PROPERTY_NAME_STEP_FREE_DESIGNATED,
          propertyName = CAS1_PROPERTY_NAME_STEP_FREE_DESIGNATED,
          weighting = 100,
          singleRoom = false,
        ),
        Characteristic(
          label = CAS1_PROPERTY_NAME_ARSON_SUITABLE,
          propertyName = CAS1_PROPERTY_NAME_ARSON_SUITABLE,
          weighting = 100,
          singleRoom = false,
        ),
        Characteristic(
          label = CAS1_PROPERTY_NAME_SINGLE_ROOM,
          propertyName = CAS1_PROPERTY_NAME_SINGLE_ROOM,
          weighting = 100,
          singleRoom = true,
        ),
      )
    }

    @Test
    fun `mark beds with end date in the past as inactive`() {
      val bed1Active = Cas1PlanningBedSummaryFactory()
        .withBedName("the active bed name")
        .withRoomName("the room name")
        .withBedEndDate(LocalDate.of(2020, 4, 5))
        .produce()

      val bed2EndedYesterday = Cas1PlanningBedSummaryFactory()
        .withBedName("the ended bed name")
        .withRoomName("the room name")
        .withBedEndDate(LocalDate.of(2020, 4, 3))
        .produce()

      val result = factory.allBedsDayState(
        day = LocalDate.of(2020, 4, 4),
        beds = listOf(bed1Active, bed2EndedYesterday),
        outOfServiceBedRecordsToConsider = emptyList(),
      )

      assertThat(result).hasSize(2)

      val activeBedDayState = result[0]
      assertThat(activeBedDayState.inactiveReason).isNull()
      assertThat(activeBedDayState.bed.id).isEqualTo(bed1Active.bedId)
      assertThat(activeBedDayState.bed.label).isEqualTo("the active bed name")

      val inactiveBedDayState = result[1]
      assertThat(inactiveBedDayState.inactiveReason).isInstanceOf(BedEnded::class.java)
      assertThat(inactiveBedDayState.bed.id).isEqualTo(bed2EndedYesterday.bedId)
      assertThat(inactiveBedDayState.bed.label).isEqualTo("the ended bed name")
    }

    @Test
    fun `exclude out of service beds`() {
      val roomEntity = RoomEntityFactory()
        .withDefaults()
        .withName("the room name")
        .produce()

      val bed1EntityActive = BedEntityFactory()
        .withDefaults()
        .withName("the active bed name")
        .produce().apply {
          roomEntity.beds.add(this)
        }

      val bed2EntityOutOfService = BedEntityFactory()
        .withDefaults()
        .withName("the oosb bed name")
        .produce().apply {
          roomEntity.beds.add(this)
        }

      val bed1ActiveSummary = Cas1PlanningBedSummaryFactory()
        .withBedId(bed1EntityActive.id)
        .withBedName("the active bed name")
        .withRoomName("the room name")
        .withBedEndDate(LocalDate.of(2020, 4, 5))
        .produce()

      val bed2EndedYesterdaySummary = Cas1PlanningBedSummaryFactory()
        .withBedId(bed2EntityOutOfService.id)
        .withBedName("the oosb bed name")
        .withRoomName("the room name")
        .withBedEndDate(LocalDate.of(2020, 4, 3))
        .produce()

      val result = factory.allBedsDayState(
        day = LocalDate.of(2020, 4, 4),
        beds = listOf(bed1ActiveSummary, bed2EndedYesterdaySummary),
        outOfServiceBedRecordsToConsider = listOf(
          Cas1OutOfServiceBedEntityFactory()
            .withBed(bed1EntityActive)
            .produce().apply {
              revisionHistory += Cas1OutOfServiceBedRevisionEntityFactory()
                .withDetailType(Cas1OutOfServiceBedRevisionType.INITIAL)
                .withStartDate(LocalDate.of(2020, 4, 1))
                .withEndDate(LocalDate.of(2020, 4, 3))
                .withOutOfServiceBed(this)
                .produce()
            },
          Cas1OutOfServiceBedEntityFactory()
            .withBed(bed2EntityOutOfService)
            .produce().apply {
              revisionHistory += Cas1OutOfServiceBedRevisionEntityFactory()
                .withDetailType(Cas1OutOfServiceBedRevisionType.INITIAL)
                .withStartDate(LocalDate.of(2020, 4, 3))
                .withEndDate(LocalDate.of(2020, 4, 5))
                .withOutOfServiceBed(this)
                .produce()
            },
        ),
      )

      assertThat(result).hasSize(2)

      val activeBedDayState = result[0]
      assertThat(activeBedDayState.inactiveReason).isNull()
      assertThat(activeBedDayState.bed.id).isEqualTo(bed1EntityActive.id)
      assertThat(activeBedDayState.bed.label).isEqualTo("the active bed name")

      val inactiveBedDayState = result[1]
      assertThat(inactiveBedDayState.inactiveReason).isInstanceOf(BedOutOfService::class.java)
      assertThat(inactiveBedDayState.bed.id).isEqualTo(bed2EntityOutOfService.id)
      assertThat(inactiveBedDayState.bed.label).isEqualTo("the oosb bed name")
    }
  }

  @Nested
  inner class SpaceBookingsForDay {

    @Test
    fun `no space bookings defined, return empty list`() {
      val result = factory.spaceBookingsForDay(
        day = LocalDate.of(2020, 4, 4),
        spaceBookingsToConsider = emptyList(),
      )

      assertThat(result).isEmpty()
    }

    @Test
    fun `all booking properties are correctly mapped`() {
      val characteristic1 = CharacteristicEntityFactory().withPropertyName(CAS1_PROPERTY_NAME_STEP_FREE_DESIGNATED).withIsActive(true).withModelScope("room").produce()
      val characteristic2 = CharacteristicEntityFactory().withPropertyName(CAS1_PROPERTY_NAME_ARSON_SUITABLE).withIsActive(true).withModelScope("room").produce()
      val characteristicSingleRoom = CharacteristicEntityFactory().withPropertyName(CAS1_PROPERTY_NAME_SINGLE_ROOM).withModelScope("room").withIsActive(true).produce()
      val characteristicNotAllowed = CharacteristicEntityFactory().withPropertyName("not in allow list").withIsActive(true).withModelScope("room").produce()

      val booking1 = Cas1SpaceBookingEntityFactory()
        .withCrn("booking1")
        .withCanonicalArrivalDate(LocalDate.of(2020, 4, 4))
        .withCanonicalDepartureDate(LocalDate.of(2020, 4, 5))
        .withCriteria(mutableListOf(characteristic1, characteristic2))
        .produce()

      val booking2 = Cas1SpaceBookingEntityFactory()
        .withCrn("booking2")
        .withCanonicalArrivalDate(LocalDate.of(2020, 4, 4))
        .withCanonicalDepartureDate(LocalDate.of(2020, 4, 5))
        .withCriteria(mutableListOf(characteristicSingleRoom, characteristicNotAllowed))
        .produce()

      val result = factory.spaceBookingsForDay(
        day = LocalDate.of(2020, 4, 4),
        spaceBookingsToConsider = listOf(booking1, booking2),
      )

      assertThat(result).hasSize(2)
      assertThat(result).containsOnly(
        SpaceBooking(
          id = booking1.id,
          label = "booking1",
          requiredRoomCharacteristics = setOf(
            Characteristic(
              label = characteristic1.propertyName!!,
              propertyName = characteristic1.propertyName!!,
              weighting = 100,
              singleRoom = false,
            ),
            Characteristic(
              label = characteristic2.propertyName!!,
              propertyName = characteristic2.propertyName!!,
              weighting = 100,
              singleRoom = false,
            ),
          ),
        ),
        SpaceBooking(
          id = booking2.id,
          label = "booking2",
          requiredRoomCharacteristics = setOf(
            Characteristic(
              label = characteristicSingleRoom.propertyName!!,
              propertyName = characteristicSingleRoom.propertyName!!,
              weighting = 100,
              singleRoom = true,
            ),
          ),
        ),
      )
    }

    @Test
    fun `include bookings arriving today`() {
      val booking1 = Cas1SpaceBookingEntityFactory()
        .withCanonicalArrivalDate(LocalDate.of(2020, 4, 4))
        .withCanonicalDepartureDate(LocalDate.of(2020, 4, 5))
        .produce()

      val result = factory.spaceBookingsForDay(
        day = LocalDate.of(2020, 4, 4),
        spaceBookingsToConsider = listOf(booking1),
      )

      assertThat(result.map { it.id }).containsExactly(booking1.id)
    }

    @Test
    fun `exclude bookings arriving after today`() {
      val booking1 = Cas1SpaceBookingEntityFactory()
        .withCanonicalArrivalDate(LocalDate.of(2020, 4, 5))
        .withCanonicalDepartureDate(LocalDate.of(2020, 4, 6))
        .produce()

      val result = factory.spaceBookingsForDay(
        day = LocalDate.of(2020, 4, 4),
        spaceBookingsToConsider = listOf(booking1),
      )

      assertThat(result).isEmpty()
    }

    @Test
    fun `exclude bookings departing before today`() {
      val booking1 = Cas1SpaceBookingEntityFactory()
        .withCanonicalArrivalDate(LocalDate.of(2020, 4, 1))
        .withCanonicalDepartureDate(LocalDate.of(2020, 4, 3))
        .produce()

      val result = factory.spaceBookingsForDay(
        day = LocalDate.of(2020, 4, 4),
        spaceBookingsToConsider = listOf(booking1),
      )

      assertThat(result).isEmpty()
    }

    @Test
    fun `exclude bookings departing today`() {
      val booking1 = Cas1SpaceBookingEntityFactory()
        .withCanonicalArrivalDate(LocalDate.of(2020, 4, 1))
        .withCanonicalDepartureDate(LocalDate.of(2020, 4, 4))
        .produce()

      val result = factory.spaceBookingsForDay(
        day = LocalDate.of(2020, 4, 4),
        spaceBookingsToConsider = listOf(booking1),
      )

      assertThat(result).isEmpty()
    }

    @Test
    fun `exclude cancelled bookings`() {
      val booking1 = Cas1SpaceBookingEntityFactory()
        .withCanonicalArrivalDate(LocalDate.of(2020, 4, 4))
        .withCanonicalDepartureDate(LocalDate.of(2020, 4, 5))
        .withCancellationOccurredAt(LocalDate.now())
        .produce()

      val result = factory.spaceBookingsForDay(
        day = LocalDate.of(2020, 4, 4),
        spaceBookingsToConsider = listOf(booking1),
      )

      assertThat(result).isEmpty()
    }

    @Test
    fun `exclude non arrivals`() {
      val booking1 = Cas1SpaceBookingEntityFactory()
        .withCanonicalArrivalDate(LocalDate.of(2020, 4, 4))
        .withCanonicalDepartureDate(LocalDate.of(2020, 4, 5))
        .withNonArrivalConfirmedAt(Instant.now())
        .produce()

      val result = factory.spaceBookingsForDay(
        day = LocalDate.of(2020, 4, 4),
        spaceBookingsToConsider = listOf(booking1),
      )

      assertThat(result).isEmpty()
    }
  }
}
