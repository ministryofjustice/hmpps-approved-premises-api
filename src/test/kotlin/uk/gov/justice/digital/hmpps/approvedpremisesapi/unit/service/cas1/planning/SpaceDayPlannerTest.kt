package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.planning

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.Bed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.Room
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpaceBookingDayPlanner
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpaceDayPlanRenderer.render
import java.util.UUID

class SpaceDayPlannerTest {

  companion object Constants {
    val CHARACTERISTIC_1 = Characteristic("c1", "c1", weighting = 100, singleRoom = false)
    val CHARACTERISTIC_2 = Characteristic("c2", "c2", weighting = 100, singleRoom = false)
    val CHARACTERISTIC_3 = Characteristic("c3", "c3", weighting = 100, singleRoom = false)
    val CHARACTERISTIC_4 = Characteristic("c4", "c4", weighting = 100, singleRoom = false)
    val CHARACTERISTIC_5_WEIGHTING_1000 = Characteristic("c5", "c5", weighting = 1000, singleRoom = false)
    val CHARACTERISTIC_SINGLE_ROOM = Characteristic("single", "single", weighting = 100, singleRoom = true)
  }

  val service = SpaceBookingDayPlanner()

  @Test
  fun `No rooms or bookings do nothing`() {
    assertPlanningOutcome(
      beds = emptySet(),
      bookings = emptySet(),
      expected = """
        Planned: 0
        
        Unplanned: 0
        """,
    )
  }

  @Test
  fun `If no rooms all bookings are unplanned`() {
    val booking1 = booking("booking1")
    val booking2 = booking("booking2")

    assertPlanningOutcome(
      beds = emptySet(),
      bookings = setOf(booking1, booking2),
      expected = """
        Planned: 0
        
        Unplanned: 2
        
        | Booking         | Characteristics                |
        | --------------- | ------------------------------ |
        | booking1        |                                |
        | booking2        |                                |""",
    )
  }

  @Test
  fun `Sufficient rooms for each booking, no characteristics`() {
    val booking1 = booking("booking1")
    val booking2 = booking("booking2")

    val bed1 = bed("bed1")
    val bed2 = bed("bed2")

    assertPlanningOutcome(
      beds = setOf(bed1, bed2),
      bookings = setOf(booking1, booking2),
      expected = """
        Planned: 2
        
        | Bed             | Booking         | Characteristics                |
        | --------------- | --------------- | ------------------------------ |
        | bed1            | booking1        |                                |
        | bed2            | booking2        |                                |
        
        Unplanned: 0
      """,
    )
  }

  @Test
  fun `Sufficient rooms for each booking, single characteristics`() {
    val booking1 = booking("booking1", requiredCharacteristics = setOf(CHARACTERISTIC_2))
    val booking2 = booking("booking2", requiredCharacteristics = setOf(CHARACTERISTIC_1))

    val bed1 = bed("bed1", roomCharacteristics = setOf(CHARACTERISTIC_1))
    val bed2 = bed("bed2", roomCharacteristics = setOf(CHARACTERISTIC_2))

    assertPlanningOutcome(
      beds = setOf(bed1, bed2),
      bookings = setOf(booking1, booking2),
      expected = """
        Planned: 2
        
        | Bed             | Booking         | Characteristics                |
        | --------------- | --------------- | ------------------------------ |
        | bed1            | booking2        | c1(rb)                         |
        | bed2            | booking1        | c2(rb)                         |
        
        Unplanned: 0
      """,
    )
  }

  @Test
  fun `If bed doesn't exist with required characteristic cannot plan booking`() {
    val bookingWithCharacteristic = booking(
      label = "booking1",
      requiredCharacteristics = setOf(CHARACTERISTIC_1),
    )

    val bed1 = bed("bed1")
    val bed2 = bed("bed2")

    assertPlanningOutcome(
      beds = setOf(bed1, bed2),
      bookings = setOf(bookingWithCharacteristic),
      expected = """
        Planned: 0
        
        Unplanned: 1
        
        | Booking         | Characteristics                |
        | --------------- | ------------------------------ |
        | booking1        | c1                             |
        """,
    )
  }

  @Test
  fun `If bed with required characteristic is already taken cannot plan booking`() {
    val booking1 = booking("booking1", requiredCharacteristics = setOf(CHARACTERISTIC_1))
    val booking2 = booking("booking2", requiredCharacteristics = setOf(CHARACTERISTIC_1))

    val bed1 = bed("bed1", roomCharacteristics = setOf(CHARACTERISTIC_1))
    val bed2 = bed("bed2")

    assertPlanningOutcome(
      beds = setOf(bed1, bed2),
      bookings = setOf(booking1, booking2),
      expected = """
      Planned: 1
  
      | Bed             | Booking         | Characteristics                |
      | --------------- | --------------- | ------------------------------ |
      | bed1            | booking1        | c1(rb)                         |
      | bed2            |                 |                                |
      
      Unplanned: 1
      
      | Booking         | Characteristics                |
      | --------------- | ------------------------------ |
      | booking2        | c1                             |
      """,
    )
  }

  @Test
  fun `Bookings with multiple characteristics take precedence over bookings with less`() {
    val booking1 = booking("booking1", requiredCharacteristics = setOf(CHARACTERISTIC_1))
    val booking2 = booking("booking2", requiredCharacteristics = setOf(CHARACTERISTIC_1, CHARACTERISTIC_3))
    val booking3 = booking("booking3", requiredCharacteristics = setOf(CHARACTERISTIC_1, CHARACTERISTIC_2, CHARACTERISTIC_4))

    val bed1 = bed("bed1", roomCharacteristics = setOf(CHARACTERISTIC_1, CHARACTERISTIC_2, CHARACTERISTIC_3, CHARACTERISTIC_4))
    val bed2 = bed("bed2", roomCharacteristics = setOf(CHARACTERISTIC_1, CHARACTERISTIC_2, CHARACTERISTIC_3, CHARACTERISTIC_4))

    assertPlanningOutcome(
      beds = setOf(bed1, bed2),
      bookings = setOf(booking1, booking2, booking3),
      expected = """
      Planned: 2
      
      | Bed             | Booking         | Characteristics                |
      | --------------- | --------------- | ------------------------------ |
      | bed1            | booking3        | c1(rb),c2(rb),c3(r),c4(rb)     |
      | bed2            | booking2        | c1(rb),c2(r),c3(rb),c4(r)      |
      
      Unplanned: 1
      
      | Booking         | Characteristics                |
      | --------------- | ------------------------------ |
      | booking1        | c1                             |
      """,
    )
  }

  @Test
  fun `Use rooms with the least surplus characteristics`() {
    val booking1 = booking("booking1", requiredCharacteristics = setOf())
    val booking2 = booking("booking2", requiredCharacteristics = setOf(CHARACTERISTIC_1, CHARACTERISTIC_3))
    val booking3 = booking("booking3", requiredCharacteristics = setOf(CHARACTERISTIC_1, CHARACTERISTIC_2))
    val booking4 = booking("booking4", requiredCharacteristics = setOf(CHARACTERISTIC_4))

    val bed1 = bed("bed1", roomCharacteristics = setOf(CHARACTERISTIC_1, CHARACTERISTIC_2, CHARACTERISTIC_3, CHARACTERISTIC_4))
    val bed2 = bed("bed2", roomCharacteristics = setOf(CHARACTERISTIC_1, CHARACTERISTIC_3))
    val bed3 = bed("bed3", roomCharacteristics = setOf(CHARACTERISTIC_1, CHARACTERISTIC_2))
    val bed4 = bed("bed4", roomCharacteristics = setOf(CHARACTERISTIC_4))
    val bed5 = bed("bed5", roomCharacteristics = setOf())

    assertPlanningOutcome(
      beds = setOf(bed1, bed2, bed3, bed4, bed5),
      bookings = setOf(booking1, booking2, booking3, booking4),
      expected = """
      Planned: 4
  
      | Bed             | Booking         | Characteristics                |
      | --------------- | --------------- | ------------------------------ |
      | bed1            |                 | c1(r),c2(r),c3(r),c4(r)        |
      | bed2            | booking2        | c1(rb),c3(rb)                  |
      | bed3            | booking3        | c1(rb),c2(rb)                  |
      | bed4            | booking4        | c4(rb)                         |
      | bed5            | booking1        |                                |
      
      Unplanned: 0
      """,
    )
  }

  @Test
  fun `Use booking with greatest total characteristic weighting when determining planning order`() {
    val booking1 = booking("booking1", requiredCharacteristics = setOf(CHARACTERISTIC_1, CHARACTERISTIC_2, CHARACTERISTIC_3, CHARACTERISTIC_4))
    val booking2 = booking("booking2", requiredCharacteristics = setOf(CHARACTERISTIC_5_WEIGHTING_1000))

    val bed1 = bed("bed1", roomCharacteristics = setOf(CHARACTERISTIC_1, CHARACTERISTIC_2, CHARACTERISTIC_3, CHARACTERISTIC_4, CHARACTERISTIC_5_WEIGHTING_1000))
    val bed2 = bed("bed2", roomCharacteristics = setOf())
    val bed3 = bed("bed3", roomCharacteristics = setOf())

    assertPlanningOutcome(
      beds = setOf(bed1, bed2, bed3),
      bookings = setOf(booking1, booking2),
      expected = """
      Planned: 1
      
      | Bed             | Booking         | Characteristics                |
      | --------------- | --------------- | ------------------------------ |
      | bed1            | booking2        | c1(r),c2(r),c3(r),c4(r),c5(rb) |
      | bed2            |                 |                                |
      | bed3            |                 |                                |
      
      Unplanned: 1
      
      | Booking         | Characteristics                |
      | --------------- | ------------------------------ |
      | booking1        | c1,c2,c3,c4                    |
      """,
    )
  }

  @Test
  fun `Booking requiring a single room is not planned if an unoccupied room is not available`() {
    val booking2 = booking("booking1", requiredCharacteristics = setOf(CHARACTERISTIC_1, CHARACTERISTIC_2))
    val booking1 = booking("booking2", requiredCharacteristics = setOf(CHARACTERISTIC_SINGLE_ROOM))

    val room1 = Room(UUID.randomUUID(), "room1", characteristics = setOf(CHARACTERISTIC_1, CHARACTERISTIC_2))
    val room1Bed1 = bed("room1 bed1", room = room1)
    val room1Bed2 = bed("room1 bed2", room = room1)
    val room1Bed3 = bed("room1 bed3", room = room1)

    assertPlanningOutcome(
      beds = setOf(room1Bed1, room1Bed2, room1Bed3),
      bookings = setOf(booking1, booking2),
      expected = """
      Planned: 1
  
      | Bed             | Booking         | Characteristics                |
      | --------------- | --------------- | ------------------------------ |
      | room1 bed1      | booking1        | c1(rb),c2(rb)                  |
      | room1 bed2      |                 | c1(r),c2(r)                    |
      | room1 bed3      |                 | c1(r),c2(r)                    |
      
      Unplanned: 1
      
      | Booking         | Characteristics                |
      | --------------- | ------------------------------ |
      | booking2        | single                         |
      """,
    )
  }

  @Test
  fun `Booking requiring a single room is treated with weighting like other bookings with characteristics`() {
    val booking1 = booking("booking1", requiredCharacteristics = setOf())
    val booking2 = booking("booking2", requiredCharacteristics = setOf(CHARACTERISTIC_SINGLE_ROOM))
    val booking3 = booking("booking3", requiredCharacteristics = setOf(CHARACTERISTIC_SINGLE_ROOM, CHARACTERISTIC_1))

    val bed1 = bed("bed1", roomCharacteristics = setOf(CHARACTERISTIC_1))
    val bed2 = bed("bed2", roomCharacteristics = setOf(CHARACTERISTIC_1))

    assertPlanningOutcome(
      beds = setOf(bed1, bed2),
      bookings = setOf(booking1, booking2, booking3),
      expected = """
      Planned: 2
      
      | Bed             | Booking         | Characteristics                |
      | --------------- | --------------- | ------------------------------ |
      | bed1            | booking3        | c1(rb),single(b)               |
      | bed2            | booking2        | c1(r),single(b)                |
      
      Unplanned: 1
      
      | Booking         | Characteristics                |
      | --------------- | ------------------------------ |
      | booking1        |                                |
      """,
    )
  }

  @Test
  fun `Booking requiring a single room is never put into a room with other bookings, even if other rooms have less surplus characteristics`() {
    val booking2 = booking("booking1", requiredCharacteristics = setOf(CHARACTERISTIC_1, CHARACTERISTIC_2))
    val booking1 = booking("booking2", requiredCharacteristics = setOf(CHARACTERISTIC_SINGLE_ROOM))

    val room1 = Room(UUID.randomUUID(), "room1", characteristics = setOf(CHARACTERISTIC_1, CHARACTERISTIC_2))
    val room1Bed1 = bed("room1 bed1", room = room1)
    val room1Bed2 = bed("room1 bed2", room = room1)

    val room2 = Room(UUID.randomUUID(), "room2", characteristics = setOf(CHARACTERISTIC_1, CHARACTERISTIC_2, CHARACTERISTIC_4))
    val room2Bed1 = bed("room2 bed1", room = room2)
    val room2Bed2 = bed("room2 bed2", room = room2)

    assertPlanningOutcome(
      beds = setOf(room1Bed1, room1Bed2, room2Bed1, room2Bed2),
      bookings = setOf(booking1, booking2),
      expected = """
      Planned: 2
    
      | Bed             | Booking         | Characteristics                |
      | --------------- | --------------- | ------------------------------ |
      | room1 bed1      | booking1        | c1(rb),c2(rb)                  |
      | room1 bed2      |                 | c1(r),c2(r)                    |
      | room2 bed1      | booking2        | c1(r),c2(r),c4(r),single(b)    |
      | room2 bed2      | booking2        | c1(r),c2(r),c4(r),single(b)    |
      
      Unplanned: 0""",
    )
  }

  @Test
  fun `Single room booking will block use of other beds in the room`() {
    val booking1 = booking("booking1", requiredCharacteristics = setOf(CHARACTERISTIC_SINGLE_ROOM))
    val booking2 = booking("booking2", requiredCharacteristics = setOf(CHARACTERISTIC_SINGLE_ROOM))
    val booking3 = booking("booking3", requiredCharacteristics = setOf())

    val room1 = Room(UUID.randomUUID(), "room1", characteristics = setOf())
    val room1Bed1 = bed("room1 bed1", room = room1)
    val room1Bed2 = bed("room1 bed2", room = room1)

    val room2 = Room(UUID.randomUUID(), "room2", characteristics = setOf())
    val room2Bed1 = bed("room2 bed1", room = room2)
    val room2Bed2 = bed("room2 bed2", room = room2)
    val room2Bed3 = bed("room2 bed3", room = room2)

    assertPlanningOutcome(
      beds = setOf(room1Bed1, room1Bed2, room2Bed1, room2Bed2, room2Bed3),
      bookings = setOf(booking1, booking2, booking3),
      expected = """
      Planned: 2
      
      | Bed             | Booking         | Characteristics                |
      | --------------- | --------------- | ------------------------------ |
      | room1 bed1      | booking1        | single(b)                      |
      | room1 bed2      | booking1        | single(b)                      |
      | room2 bed1      | booking2        | single(b)                      |
      | room2 bed2      | booking2        | single(b)                      |
      | room2 bed3      | booking2        | single(b)                      |
      
      Unplanned: 1
      
      | Booking         | Characteristics                |
      | --------------- | ------------------------------ |
      | booking3        |                                |
      """,
    )
  }

  @Test
  fun `Booking requiring a single room is put into a room with the least beds`() {
    val booking2 = booking("booking1", requiredCharacteristics = setOf(CHARACTERISTIC_SINGLE_ROOM))

    val room1 = Room(UUID.randomUUID(), "room1", characteristics = setOf())
    val room1Bed1 = bed("room1 bed1", room = room1)
    val room1Bed2 = bed("room1 bed2", room = room1)
    val room1Bed3 = bed("room1 bed3", room = room1)

    val room2 = Room(UUID.randomUUID(), "room2", characteristics = setOf())
    val room2Bed1 = bed("room2 bed1", room = room2)
    val room2Bed2 = bed("room2 bed2", room = room2)

    assertPlanningOutcome(
      beds = setOf(room1Bed1, room1Bed2, room1Bed3, room2Bed1, room2Bed2),
      bookings = setOf(booking2),
      expected = """
      Planned: 1
    
      | Bed             | Booking         | Characteristics                |
      | --------------- | --------------- | ------------------------------ |
      | room1 bed1      |                 |                                |
      | room1 bed2      |                 |                                |
      | room1 bed3      |                 |                                |
      | room2 bed1      | booking1        | single(b)                      |
      | room2 bed2      | booking1        | single(b)                      |
      
      Unplanned: 0
      """,
    )
  }

  @Test
  fun `Booking requiring a single room is put into a room with single room characteristic vs room with no characteristics`() {
    val booking2 = booking("booking1", requiredCharacteristics = setOf(CHARACTERISTIC_SINGLE_ROOM))

    val room1 = Room(UUID.randomUUID(), "room1", characteristics = setOf())
    val room1Bed1 = bed("room1 bed1", room = room1)
    val room1Bed2 = bed("room1 bed2", room = room1)
    val room1Bed3 = bed("room1 bed3", room = room1)

    val room2 = Room(UUID.randomUUID(), "room2", characteristics = setOf(CHARACTERISTIC_SINGLE_ROOM))
    val room2Bed1 = bed("room2 bed1", room = room2)

    assertPlanningOutcome(
      beds = setOf(room1Bed1, room1Bed2, room1Bed3, room2Bed1),
      bookings = setOf(booking2),
      expected = """
      Planned: 1
    
      | Bed             | Booking         | Characteristics                |
      | --------------- | --------------- | ------------------------------ |
      | room1 bed1      |                 |                                |
      | room1 bed2      |                 |                                |
      | room1 bed3      |                 |                                |
      | room2 bed1      | booking1        | single(rb)                     |
      
      Unplanned: 0
      """,
    )
  }

  private fun assertPlanningOutcome(
    beds: Set<Bed>,
    bookings: Set<SpaceBooking>,
    expected: String,
  ) {
    val result = service.plan(beds, bookings)
    assertThat(render(beds, result)).isEqualTo(expected.trimIndent())
  }

  fun bed(
    label: String,
    roomCharacteristics: Set<Characteristic> = emptySet(),
    room: Room = Room(
      id = UUID.randomUUID(),
      label = label,
      characteristics = roomCharacteristics,
    ),
  ) = Bed(
    id = UUID.randomUUID(),
    label = label,
    room = room,
  )

  fun booking(label: String, requiredCharacteristics: Set<Characteristic> = emptySet()) =
    SpaceBooking(
      UUID.randomUUID(),
      label,
      requiredCharacteristics,
    )
}
