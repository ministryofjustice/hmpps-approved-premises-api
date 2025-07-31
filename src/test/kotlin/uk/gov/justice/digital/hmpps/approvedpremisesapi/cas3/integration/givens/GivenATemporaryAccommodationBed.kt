package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import java.time.LocalDate

fun IntegrationTestBase.givenATemporaryAccommodationBed(
  room: RoomEntity? = null,
  startDate: LocalDate = LocalDate.now().minusDays(30),
  endDate: LocalDate? = null,
  block: ((bed: BedEntity) -> Unit)? = null,
): BedEntity {
  val resolvedRoom = room ?: givenATemporaryAccommodationRoom()

  val bed = bedEntityFactory.produceAndPersist {
    withRoom(resolvedRoom)
    withStartDate(startDate)
    withEndDate(endDate)
  }

  block?.invoke(bed)
  return bed
}

fun IntegrationTestBase.givenATemporaryAccommodationBeds(
  room: RoomEntity? = null,
  count: Int = 1,
  startDates: List<LocalDate> = emptyList(),
  endDates: List<LocalDate?> = emptyList(),
  block: ((beds: List<BedEntity>) -> Unit)? = null,
): List<BedEntity> {
  val resolvedRoom = room ?: givenATemporaryAccommodationRoom()
  val beds = mutableListOf<BedEntity>()

  repeat(count) { index ->
    val startDate = if (startDates.size > index) startDates[index] else LocalDate.now().minusDays(30)
    val endDate = if (endDates.size > index) endDates[index] else null

    val bed = givenATemporaryAccommodationBed(
      room = resolvedRoom,
      startDate = startDate,
      endDate = endDate,
    )
    beds.add(bed)
  }

  block?.invoke(beds)
  return beds
}

fun IntegrationTestBase.givenATemporaryAccommodationBedWithPremises(
  premises: TemporaryAccommodationPremisesEntity? = null,
  startDate: LocalDate = LocalDate.now().minusDays(30),
  endDate: LocalDate? = null,
  block: ((bed: BedEntity, room: RoomEntity) -> Unit)? = null,
): BedEntity {
  val resolvedPremises = premises ?: givenATemporaryAccommodationPremises()
  val room = givenATemporaryAccommodationRoom(premises = resolvedPremises)
  val bed = givenATemporaryAccommodationBed(
    room = room,
    startDate = startDate,
    endDate = endDate,
  )

  block?.invoke(bed, room)
  return bed
}
