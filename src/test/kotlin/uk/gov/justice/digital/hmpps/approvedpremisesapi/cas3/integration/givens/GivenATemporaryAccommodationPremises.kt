package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate

fun IntegrationTestBase.givenATemporaryAccommodationPremises(
  region: ProbationRegionEntity = givenAProbationRegion(),
  status: PropertyStatus = PropertyStatus.active,
  startDate: LocalDate = LocalDate.now().minusDays(180),
  endDate: LocalDate? = null,
  block: ((premises: TemporaryAccommodationPremisesEntity) -> Unit)? = null,
): TemporaryAccommodationPremisesEntity {
  val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
    withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
    withYieldedProbationRegion { region }
    withYieldedProbationDeliveryUnit {
      probationDeliveryUnitFactory.produceAndPersist {
        withProbationRegion(region)
      }
    }
    withStatus(status)
    withStartDate(startDate)
    withEndDate(endDate)
  }

  block?.invoke(premises)
  return premises
}

/**
 * Temporary accommodation premises with user creation
 */
fun IntegrationTestBase.givenATemporaryAccommodationPremisesWithUser(
  roles: List<UserRole> = emptyList(),
  probationRegion: ProbationRegionEntity? = null,
  premisesStatus: PropertyStatus = PropertyStatus.active,
  premisesStartDate: LocalDate = LocalDate.now().minusDays(180),
  premisesEndDate: LocalDate? = null,
  block: (user: UserEntity, jwt: String, premises: TemporaryAccommodationPremisesEntity) -> Unit,
) {
  givenAUser(
    roles = roles,
    probationRegion = probationRegion,
  ) { user, jwt ->
    val premises = givenATemporaryAccommodationPremises(
      region = user.probationRegion,
      status = premisesStatus,
      startDate = premisesStartDate,
      endDate = premisesEndDate,
    )
    block(user, jwt, premises)
  }
}

/**
 * Temporary accommodation premises with rooms
 */
fun IntegrationTestBase.givenATemporaryAccommodationPremisesWithRooms(
  region: ProbationRegionEntity = givenAProbationRegion(),
  roomCount: Int = 1,
  roomNames: List<String> = emptyList(),
  roomCharacteristics: List<CharacteristicEntity> = emptyList(),
  block: (premises: TemporaryAccommodationPremisesEntity, rooms: List<RoomEntity>) -> Unit,
) {
  val premises = givenATemporaryAccommodationPremises(region)
  val rooms = mutableListOf<RoomEntity>()

  repeat(roomCount) { index ->
    val roomName = if (roomNames.size > index) roomNames[index] else randomStringMultiCaseWithNumbers(8)
    val room = roomEntityFactory.produceAndPersist {
      withPremises(premises)
      withName(roomName)
      withCharacteristics(roomCharacteristics.toMutableList())
    }
    rooms.add(room)
  }

  block(premises, rooms)
}

fun IntegrationTestBase.givenATemporaryAccommodationPremisesWithRoomsAndBeds(
  region: ProbationRegionEntity = givenAProbationRegion(),
  roomCount: Int = 1,
  bedStartDates: List<LocalDate> = emptyList(),
  bedEndDates: List<LocalDate?> = emptyList(),
  roomNames: List<String> = emptyList(),
  roomCharacteristics: List<CharacteristicEntity> = emptyList(),
  block: (premises: TemporaryAccommodationPremisesEntity, rooms: List<RoomEntity>, beds: List<BedEntity>) -> Unit,
) {
  givenATemporaryAccommodationPremisesWithRooms(
    region = region,
    roomCount = roomCount,
    roomNames = roomNames,
    roomCharacteristics = roomCharacteristics,
  ) { premises, rooms ->
    val allBeds = mutableListOf<BedEntity>()

    rooms.forEachIndexed { roomIndex, room ->
      val startDate = if (bedStartDates.size > roomIndex) bedStartDates[roomIndex] else LocalDate.now().minusDays(30)
      val endDate = if (bedEndDates.size > roomIndex) bedEndDates[roomIndex] else null

      val bed = bedEntityFactory.produceAndPersist {
        withRoom(room)
        withStartDate(startDate)
        withEndDate(endDate)
      }
      allBeds.add(bed)
    }

    block(premises, rooms, allBeds)
  }
}

fun IntegrationTestBase.givenATemporaryAccommodationPremisesComplete(
  roles: List<UserRole> = emptyList(),
  roomCount: Int = 1,
  premisesStatus: PropertyStatus = PropertyStatus.active,
  premisesEndDate: LocalDate? = null,
  bedStartDates: List<LocalDate> = emptyList(),
  bedEndDates: List<LocalDate?> = emptyList(),
  roomNames: List<String> = emptyList(),
  roomCharacteristics: List<CharacteristicEntity> = emptyList(),
  block: (user: UserEntity, jwt: String, premises: TemporaryAccommodationPremisesEntity, rooms: List<RoomEntity>, beds: List<BedEntity>) -> Unit,
) {
  givenAUser(
    roles = roles,
  ) { user, jwt ->
    givenATemporaryAccommodationPremisesWithRoomsAndBeds(
      region = user.probationRegion,
      roomCount = roomCount,
      bedStartDates = bedStartDates,
      bedEndDates = bedEndDates,
      roomNames = roomNames,
      roomCharacteristics = roomCharacteristics,
    ) { premises, rooms, beds ->
      // Update premises with status and end date if provided
      if (premisesStatus != premises.status || premisesEndDate != premises.endDate) {
        premises.status = premisesStatus
        premises.endDate = premisesEndDate
        temporaryAccommodationPremisesRepository.save(premises)
      }

      block(user, jwt, premises, rooms, beds)
    }
  }
}

fun IntegrationTestBase.givenATemporaryAccommodationPremisesWithUserScheduledForArchive(
  roles: List<UserRole> = emptyList(),
  archiveDate: LocalDate = LocalDate.now().plusDays(10),
  premisesStatus: PropertyStatus = PropertyStatus.active,
  block: (user: UserEntity, jwt: String, premises: TemporaryAccommodationPremisesEntity) -> Unit,
) {
  givenATemporaryAccommodationPremisesWithUser(
    roles = roles,
    premisesEndDate = archiveDate,
    premisesStatus = premisesStatus,
  ) { user, jwt, premises ->
    block(user, jwt, premises)
  }
}
