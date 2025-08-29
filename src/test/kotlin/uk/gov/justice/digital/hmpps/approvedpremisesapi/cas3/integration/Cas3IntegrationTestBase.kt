package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
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
      withStartDate(startDate)
      withEndDate(endDate)
    }.apply {
      premises.rooms
        .first { it.id == room.id }
        .beds.add(this)
    }

    return bedspace
  }

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

  protected fun createCas3Bedspace(bed: BedEntity, room: RoomEntity, bedspaceStatus: Cas3BedspaceStatus, archiveHistory: List<Cas3BedspaceArchiveAction> = emptyList()) = Cas3Bedspace(
    id = bed.id,
    reference = room.name,
    startDate = bed.startDate!!,
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
    notes = room.notes,
    archiveHistory = archiveHistory,
  )

  protected fun pickRandomCharacteristicAndRemoveFromList(characteristics: MutableList<CharacteristicEntity>): CharacteristicEntity {
    val randomCharacteristic = randomOf(characteristics)
    characteristics.remove(randomCharacteristic)
    return randomCharacteristic
  }

  protected fun getPremisesCharacteristics() = characteristicRepository.findAllByServiceAndModelScope(
    modelScope = "premises",
    serviceScope = ServiceName.temporaryAccommodation.value,
  )

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

  private fun getRoomCharacteristics() = characteristicRepository.findAllByServiceAndModelScope(
    modelScope = "room",
    serviceScope = ServiceName.temporaryAccommodation.value,
  )
}
