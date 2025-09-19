package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LocalAuthorityArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationDeliveryUnit
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BedspacesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceArchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceUnarchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3BedspaceUnarchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesArchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesArchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesUnarchiveEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.CAS3PremisesUnarchiveEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Bedspace
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceArchiveAction
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BedspaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.events.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateAfter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

abstract class Cas3IntegrationTestBase : IntegrationTestBase() {

  @SuppressWarnings("LongParameterList")
  protected fun getListPremisesByStatus(
    probationRegion: ProbationRegionEntity,
    probationDeliveryUnit: ProbationDeliveryUnitEntity,
    localAuthorityArea: LocalAuthorityAreaEntity,
    numberOfPremises: Int,
    propertyStatus: PropertyStatus,
    startDate: LocalDate = LocalDate.now().minusDays(180),
    endDate: LocalDate? = null,
  ): List<TemporaryAccommodationPremisesEntity> {
    val premisesCharacteristics = getPremisesCharacteristics().toMutableList()

    val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(numberOfPremises) {
      withProbationRegion(probationRegion)
      withProbationDeliveryUnit(probationDeliveryUnit)
      withLocalAuthorityArea(localAuthorityArea)
      withStatus(propertyStatus)
      withStartDate(startDate)
      withEndDate(endDate)
      withAddressLine2(randomStringUpperCase(10))
      withCharacteristics(
        mutableListOf(
          pickRandomCharacteristicAndRemoveFromList(premisesCharacteristics),
          pickRandomCharacteristicAndRemoveFromList(premisesCharacteristics),
          pickRandomCharacteristicAndRemoveFromList(premisesCharacteristics),
        ),
      )
    }

    return premises
  }

  protected fun createRoomsWithSingleBedInPremises(
    premises: List<TemporaryAccommodationPremisesEntity>,
    endDate: LocalDate? = null,
    numOfRoomsPerPremise: Int = 1,
  ): List<TemporaryAccommodationPremisesEntity> {
    premises.forEach { premise ->
      val roomCharacteristics = getRoomCharacteristics().toMutableList()
      val rooms = roomEntityFactory.produceAndPersistMultiple(numOfRoomsPerPremise) {
        withPremises(premise)
        withBeds()
        withCharacteristics(
          mutableListOf(
            pickRandomCharacteristicAndRemoveFromList(roomCharacteristics),
            pickRandomCharacteristicAndRemoveFromList(roomCharacteristics),
            pickRandomCharacteristicAndRemoveFromList(roomCharacteristics),
          ),
        )
      }.apply { premise.rooms.addAll(this) }

      rooms.forEach { room ->
        bedEntityFactory.produceAndPersist {
          withRoom(room)
          withEndDate(endDate)
          withStartDate(LocalDate.now().minusDays(90))
          withCreatedDate(LocalDate.now().minusDays(90))
        }.apply {
          premise.rooms
            .first { it.id == room.id }
            .beds.add(this)
        }
      }
    }
    return premises
  }

  protected fun createBedspaceInPremises(
    premises: TemporaryAccommodationPremisesEntity,
    startDate: LocalDate,
    endDate: LocalDate? = null,
  ): BedEntity {
    val roomCharacteristics = getRoomCharacteristics().toMutableList()
    val room = roomEntityFactory.produceAndPersist {
      withPremises(premises)
      withBeds()
      withCharacteristics(
        mutableListOf(
          pickRandomCharacteristicAndRemoveFromList(roomCharacteristics),
          pickRandomCharacteristicAndRemoveFromList(roomCharacteristics),
          pickRandomCharacteristicAndRemoveFromList(roomCharacteristics),
        ),
      )
    }.apply { premises.rooms.add(this) }

    val bedspace = bedEntityFactory.produceAndPersist {
      withRoom(room)
      withCreatedDate(startDate)
      withStartDate(startDate)
      withEndDate(endDate)
    }.apply {
      premises.rooms
        .first { it.id == room.id }
        .beds.add(this)
    }

    return bedspace
  }

  protected fun createBedspaces(premises: TemporaryAccommodationPremisesEntity, status: Cas3BedspaceStatus, withoutEndDate: Boolean = false): List<BedEntity> {
    var startDate: LocalDate?
    var endDate: LocalDate?
    val bedspaces = mutableListOf<BedEntity>()

    repeat(randomInt(1, 5)) {
      when (status) {
        Cas3BedspaceStatus.online -> {
          startDate = LocalDate.now().randomDateBefore(360)
          endDate = when {
            withoutEndDate -> null
            else -> LocalDate.now().plusDays(1).randomDateAfter(90)
          }
        }
        Cas3BedspaceStatus.upcoming -> {
          startDate = LocalDate.now().plusDays(1).randomDateAfter(30)
          endDate = when {
            withoutEndDate -> null
            else -> startDate.plusDays(1).randomDateAfter(90)
          }
        }
        Cas3BedspaceStatus.archived -> {
          endDate = LocalDate.now().minusDays(1).randomDateBefore(360)
          startDate = endDate!!.randomDateBefore(360)
        }
      }

      bedspaces.add(createBedspaceInPremises(premises, startDate, endDate))
    }

    return bedspaces
  }

  protected fun createPremisesSummary(premises: TemporaryAccommodationPremisesEntity, bedspaceCount: Int) = Cas3PremisesSummary(
    id = premises.id,
    name = premises.name,
    addressLine1 = premises.addressLine1,
    addressLine2 = premises.addressLine2,
    postcode = premises.postcode,
    pdu = premises.probationDeliveryUnit?.name!!,
    status = premises.status,
    bedspaceCount = bedspaceCount,
    localAuthorityAreaName = premises.localAuthorityArea?.name!!,
  )

  @SuppressWarnings("LongParameterList")
  fun createPremisesUnarchiveDomainEvent(
    premises: TemporaryAccommodationPremisesEntity,
    userEntity: UserEntity,
    currentStartDate: LocalDate,
    newStartDate: LocalDate,
    currentEndDate: LocalDate,
    cancelledAt: OffsetDateTime? = null,
  ) = domainEventFactory.produceAndPersist {
    withService(ServiceName.temporaryAccommodation)
    withCas3PremisesId(premises.id)
    withType(DomainEventType.CAS3_PREMISES_UNARCHIVED)
    withCas3CancelledAt(cancelledAt)
    withData(
      objectMapper.writeValueAsString(
        CAS3PremisesUnarchiveEvent(
          id = UUID.randomUUID(),
          timestamp = Instant.now(),
          eventType = EventType.premisesUnarchived,
          eventDetails =
          CAS3PremisesUnarchiveEventDetails(
            premisesId = premises.id,
            userId = userEntity.id,
            currentStartDate = currentStartDate,
            newStartDate = newStartDate,
            currentEndDate = currentEndDate,
          ),
        ),
      ),
    )
  }

  fun createPremisesArchiveDomainEvent(
    premises: TemporaryAccommodationPremisesEntity,
    userEntity: UserEntity,
    date: LocalDate,
    cancelledAt: OffsetDateTime? = null,
  ) = domainEventFactory.produceAndPersist {
    withService(ServiceName.temporaryAccommodation)
    withCas3PremisesId(premises.id)
    withType(DomainEventType.CAS3_PREMISES_ARCHIVED)
    withCas3CancelledAt(cancelledAt)
    withData(
      objectMapper.writeValueAsString(
        CAS3PremisesArchiveEvent(
          id = UUID.randomUUID(),
          timestamp = Instant.now(),
          eventType = EventType.premisesArchived,
          eventDetails =
          CAS3PremisesArchiveEventDetails(
            premisesId = premises.id,
            userId = userEntity.id,
            endDate = date,
          ),
        ),
      ),
    )
  }

  @SuppressWarnings("LongParameterList")
  protected fun createCas3Premises(
    premises: TemporaryAccommodationPremisesEntity,
    probationRegion: ProbationRegionEntity,
    probationDeliveryUnit: ProbationDeliveryUnitEntity,
    localAuthorityArea: LocalAuthorityAreaEntity,
    status: Cas3PremisesStatus,
    scheduleUnarchiveDate: LocalDate? = null,
    totalOnlineBedspaces: Int,
    totalUpcomingBedspaces: Int,
    totalArchivedBedspaces: Int,
  ) = Cas3Premises(
    id = premises.id,
    reference = premises.name,
    addressLine1 = premises.addressLine1,
    addressLine2 = premises.addressLine2,
    postcode = premises.postcode,
    town = premises.town,
    probationRegion = ProbationRegion(probationRegion.id, probationRegion.name),
    probationDeliveryUnit = ProbationDeliveryUnit(probationDeliveryUnit.id, probationDeliveryUnit.name),
    localAuthorityArea = LocalAuthorityArea(
      localAuthorityArea.id,
      localAuthorityArea.identifier,
      localAuthorityArea.name,
    ),
    startDate = premises.createdAt.toLocalDate(),
    endDate = premises.endDate,
    scheduleUnarchiveDate = scheduleUnarchiveDate,
    status = status,
    characteristics = premises.characteristics.sortedBy { it.id }.map { characteristic ->
      Characteristic(
        id = characteristic.id,
        name = characteristic.name,
        propertyName = characteristic.propertyName,
        serviceScope = Characteristic.ServiceScope.temporaryMinusAccommodation,
        modelScope = Characteristic.ModelScope.forValue(characteristic.modelScope),
      )
    },
    notes = premises.notes,
    turnaroundWorkingDays = premises.turnaroundWorkingDays,
    totalOnlineBedspaces = totalOnlineBedspaces,
    totalUpcomingBedspaces = totalUpcomingBedspaces,
    totalArchivedBedspaces = totalArchivedBedspaces,
    archiveHistory = emptyList(),
  )

  protected fun createCas3Bedspace(
    bed: BedEntity,
    room: RoomEntity,
    bedspaceStatus: Cas3BedspaceStatus,
    scheduleUnarchiveDate: LocalDate? = null,
    archiveHistory: List<Cas3BedspaceArchiveAction> = emptyList(),
  ) = Cas3Bedspace(
    id = bed.id,
    reference = room.name,
    startDate = bed.createdDate,
    characteristics = room.characteristics.map { characteristic ->
      Characteristic(
        id = characteristic.id,
        name = characteristic.name,
        propertyName = characteristic.propertyName,
        serviceScope = Characteristic.ServiceScope.temporaryMinusAccommodation,
        modelScope = Characteristic.ModelScope.forValue(characteristic.modelScope),
      )
    },
    endDate = bed.endDate,
    status = bedspaceStatus,
    scheduleUnarchiveDate = scheduleUnarchiveDate,
    notes = room.notes,
    archiveHistory = archiveHistory,
  )

  protected fun createCas3Bedspace(
    bedspace: Cas3BedspacesEntity,
    bedspaceStatus: Cas3BedspaceStatus,
    scheduleUnarchiveDate: LocalDate? = null,
    archiveHistory: List<Cas3BedspaceArchiveAction> = emptyList(),
  ) = Cas3Bedspace(
    id = bedspace.id,
    reference = bedspace.reference,
    startDate = bedspace.createdDate,
    bedspaceCharacteristics = bedspace.characteristics.map { characteristic ->
      Cas3BedspaceCharacteristic(
        id = characteristic.id,
        description = characteristic.description,
        name = characteristic.name,
      )
    },
    endDate = bedspace.endDate,
    status = bedspaceStatus,
    scheduleUnarchiveDate = scheduleUnarchiveDate,
    notes = bedspace.notes,
    archiveHistory = archiveHistory,
  )

  protected fun pickRandomCharacteristicAndRemoveFromList(characteristics: MutableList<CharacteristicEntity>): CharacteristicEntity {
    val randomCharacteristic = randomOf(characteristics)
    characteristics.remove(randomCharacteristic)
    return randomCharacteristic
  }

  @SuppressWarnings("LongParameterList")
  protected fun createBedspaceArchiveDomainEvent(
    bedspaceId: UUID,
    premisesId: UUID,
    userId: UUID,
    currentEndDate: LocalDate?,
    endDate: LocalDate,
    cancelledAt: OffsetDateTime? = null,
  ) = domainEventFactory.produceAndPersist {
    withService(ServiceName.temporaryAccommodation)
    withCas3BedspaceId(bedspaceId)
    withType(DomainEventType.CAS3_BEDSPACE_ARCHIVED)
    withCas3CancelledAt(cancelledAt)
    withData(
      objectMapper.writeValueAsString(
        CAS3BedspaceArchiveEvent(
          id = UUID.randomUUID(),
          timestamp = OffsetDateTime.now().toInstant(),
          eventType = EventType.bedspaceArchived,
          eventDetails = CAS3BedspaceArchiveEventDetails(
            bedspaceId = bedspaceId,
            userId = userId,
            premisesId = premisesId,
            currentEndDate = currentEndDate,
            endDate = endDate,
          ),
        ),
      ),
    )
  }

  @SuppressWarnings("LongParameterList")
  protected fun createBedspaceUnarchiveDomainEvent(
    bedspace: BedEntity,
    premisesId: UUID,
    userId: UUID,
    newStartDate: LocalDate,
    cancelledAt: OffsetDateTime? = null,
  ) = domainEventFactory.produceAndPersist {
    withService(ServiceName.temporaryAccommodation)
    withCas3BedspaceId(bedspace.id)
    withType(DomainEventType.CAS3_BEDSPACE_UNARCHIVED)
    withCas3CancelledAt(cancelledAt)
    withData(
      objectMapper.writeValueAsString(
        CAS3BedspaceUnarchiveEvent(
          id = UUID.randomUUID(),
          timestamp = OffsetDateTime.now().toInstant(),
          eventType = EventType.bedspaceUnarchived,
          eventDetails = CAS3BedspaceUnarchiveEventDetails(
            bedspaceId = bedspace.id,
            premisesId = premisesId,
            userId = userId,
            currentStartDate = bedspace.startDate!!,
            currentEndDate = bedspace.endDate!!,
            newStartDate = newStartDate,
          ),
        ),
      ),
    )
  }

  @SuppressWarnings("LongParameterList")
  protected fun createBedspaceUnarchiveDomainEvent(
    bedspace: Cas3BedspacesEntity,
    premisesId: UUID,
    userId: UUID,
    newStartDate: LocalDate,
    cancelledAt: OffsetDateTime? = null,
  ) = domainEventFactory.produceAndPersist {
    withService(ServiceName.temporaryAccommodation)
    withCas3BedspaceId(bedspace.id)
    withType(DomainEventType.CAS3_BEDSPACE_UNARCHIVED)
    withCas3CancelledAt(cancelledAt)
    withData(
      objectMapper.writeValueAsString(
        CAS3BedspaceUnarchiveEvent(
          id = UUID.randomUUID(),
          timestamp = OffsetDateTime.now().toInstant(),
          eventType = EventType.bedspaceUnarchived,
          eventDetails = CAS3BedspaceUnarchiveEventDetails(
            bedspaceId = bedspace.id,
            premisesId = premisesId,
            userId = userId,
            currentStartDate = bedspace.startDate!!,
            currentEndDate = bedspace.endDate!!,
            newStartDate = newStartDate,
          ),
        ),
      ),
    )
  }

  protected fun getPremisesCharacteristics() = characteristicRepository.findAllByServiceAndModelScope(
    modelScope = "premises",
    serviceScope = ServiceName.temporaryAccommodation.value,
  )

  protected fun getRoomCharacteristics() = characteristicRepository.findAllByServiceAndModelScope(
    modelScope = "room",
    serviceScope = ServiceName.temporaryAccommodation.value,
  )
}
