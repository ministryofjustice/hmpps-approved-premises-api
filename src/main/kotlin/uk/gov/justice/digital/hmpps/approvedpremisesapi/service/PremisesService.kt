package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesLostBedsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedCancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedCancellationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationLostBedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Availability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.validated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.getDaysUntilExclusiveEnd
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class PremisesService(
  private val premisesRepository: PremisesRepository,
  private val lostBedsRepository: LostBedsRepository,
  private val bookingRepository: BookingRepository,
  private val lostBedReasonRepository: LostBedReasonRepository,
  private val localAuthorityAreaRepository: LocalAuthorityAreaRepository,
  private val probationRegionRepository: ProbationRegionRepository,
  private val lostBedCancellationRepository: LostBedCancellationRepository,
  private val roomRepository: RoomRepository,
  private val characteristicService: CharacteristicService
) {
  private val serviceNameToEntityType = mapOf(
    ServiceName.approvedPremises to ApprovedPremisesEntity::class.java,
    ServiceName.temporaryAccommodation to TemporaryAccommodationPremisesEntity::class.java,
  )

  fun getAllPremises(): List<PremisesEntity> {
    val premises = premisesRepository.findAll()

    return loadDependentEntities(
      premises,
      loadProbationRegions = true,
      loadProbationRegionApAreas = true,
      loadLocalAuthorityAreas = true,
      loadBookings = true,
      loadLostBeds = true,
      loadRooms = true,
      loadRoomBeds = true,
      loadCharacteristics = true,
    )
  }

  fun getAllPremisesInRegion(probationRegionId: UUID): List<PremisesEntity> {
    val premises = premisesRepository.findAllByProbationRegion_Id(probationRegionId)

    return loadDependentEntities(
      premises,
      loadLocalAuthorityAreas = true,
      loadBookings = true,
      loadLostBeds = true,
      loadRooms = true,
      loadRoomBeds = true,
      loadCharacteristics = true,
    )
  }

  fun getAllPremisesForService(service: ServiceName): List<PremisesEntity> {
    val premises = serviceNameToEntityType[service]?.let {
      premisesRepository.findAllByType(it)
    } ?: listOf()

    return loadDependentEntities(
      premises,
      loadProbationRegions = true,
      loadProbationRegionApAreas = true,
      loadLocalAuthorityAreas = true,
      loadBookings = true,
      loadLostBeds = true,
      loadRooms = true,
      loadRoomBeds = true,
      loadCharacteristics = true,
    )
  }

  fun getAllPremisesInRegionForService(
    probationRegionId: UUID,
    service: ServiceName,
  ): List<PremisesEntity> {
    val premises = serviceNameToEntityType[service]?.let {
      premisesRepository.findAllByProbationRegion_IdAndType(probationRegionId, it)
    } ?: listOf()

    return loadDependentEntities(
      premises,
      loadLocalAuthorityAreas = true,
      loadBookings = true,
      loadLostBeds = true,
      loadRooms = true,
      loadRoomBeds = true,
      loadCharacteristics = true,
    )
  }

  fun getPremises(premisesId: UUID): PremisesEntity? {
    val premises = listOfNotNull(premisesRepository.findByIdOrNull(premisesId))

    return loadDependentEntities(
      premises,
      loadProbationRegions = true,
      loadProbationRegionApAreas = true,
      loadLocalAuthorityAreas = true,
      loadBookings = true,
      loadLostBeds = true,
      loadRooms = true,
      loadRoomBeds = true,
      loadCharacteristics = true,
    ).firstOrNull()
  }

  fun getLastBookingDate(premises: PremisesEntity) = bookingRepository.getHighestBookingDate(premises.id)
  fun getLastLostBedsDate(premises: PremisesEntity) = lostBedsRepository.getHighestBookingDate(premises.id)

  fun getAvailabilityForRange(
    premises: PremisesEntity,
    startDate: LocalDate,
    endDate: LocalDate
  ): Map<LocalDate, Availability> {
    if (endDate.isBefore(startDate)) throw RuntimeException("startDate must be before endDate when calculating availability for range")

    val bookings = when (premises.bookingsLoaded) {
      true -> premises.bookings.filter { it.arrivalDate <= endDate && it.departureDate >= startDate }
      else -> bookingRepository.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate)
    }

    val lostBeds = when (premises.lostBedsLoaded) {
      true -> premises.lostBeds.filter { it.startDate <= endDate && it.endDate >= startDate }
      false -> lostBedsRepository.findAllByPremisesIdAndOverlappingDate(premises.id, startDate, endDate)
    }

    return startDate.getDaysUntilExclusiveEnd(endDate).map { date ->
      val bookingsOnDay = bookings.filter { booking -> booking.arrivalDate <= date && booking.departureDate > date }
      val lostBedsOnDay = lostBeds.filter { lostBed -> lostBed.startDate <= date && lostBed.endDate > date && lostBed.cancellation == null }

      Availability(
        date = date,
        pendingBookings = bookingsOnDay.count { it.arrival == null && it.nonArrival == null && it.cancellation == null },
        arrivedBookings = bookingsOnDay.count { it.arrival != null },
        nonArrivedBookings = bookingsOnDay.count { it.nonArrival != null },
        cancelledBookings = bookingsOnDay.count { it.cancellation != null },
        lostBeds = lostBedsOnDay.sumOf { if (it is ApprovedPremisesLostBedsEntity) it.numberOfBeds else 1 }
      )
    }.associateBy { it.date }
  }

  fun createLostBeds(
    premises: PremisesEntity,
    startDate: LocalDate,
    endDate: LocalDate,
    reasonId: UUID,
    referenceNumber: String?,
    notes: String?,
    service: ServiceName,
    numberOfBeds: Int?,
    bedId: UUID?,
  ): ValidatableActionResult<LostBedsEntity> =
    validated {
      if (endDate.isBefore(startDate)) {
        "$.endDate" hasValidationError "beforeStartDate"
      }

      if (service == ServiceName.approvedPremises && numberOfBeds!! <= 0) {
        "$.numberOfBeds" hasValidationError "isZero"
      }

      val bed = if (service == ServiceName.temporaryAccommodation) {
        val bed = premises.rooms.flatMap { it.beds }.firstOrNull { it.id == bedId }
        if (bed == null) {
          "$.bedId" hasValidationError "doesNotExist"
        }
        bed
      } else {
        null
      }

      val reason = lostBedReasonRepository.findByIdOrNull(reasonId)
      if (reason == null) {
        "$.reason" hasValidationError "doesNotExist"
      } else if (!serviceScopeMatches(reason.serviceScope, premises)) {
        "$.reason" hasValidationError "incorrectLostBedReasonServiceScope"
      }

      if (validationErrors.any()) {
        return fieldValidationError
      }

      val lostBedsEntity = when (service) {
        ServiceName.approvedPremises -> lostBedsRepository.save(
          ApprovedPremisesLostBedsEntity(
            id = UUID.randomUUID(),
            premises = premises,
            startDate = startDate,
            endDate = endDate,
            numberOfBeds = numberOfBeds!!,
            reason = reason!!,
            referenceNumber = referenceNumber,
            notes = notes,
            lostBedCancellation = null,
          )
        )
        ServiceName.temporaryAccommodation -> lostBedsRepository.save(
          TemporaryAccommodationLostBedEntity(
            id = UUID.randomUUID(),
            premises = premises,
            startDate = startDate,
            endDate = endDate,
            bed = bed!!,
            reason = reason!!,
            referenceNumber = referenceNumber,
            notes = notes,
            lostBedCancellation = null,
          )
        )
      }

      return success(lostBedsEntity)
    }

  fun updateLostBeds(
    lostBedId: UUID,
    startDate: LocalDate,
    endDate: LocalDate,
    reasonId: UUID,
    referenceNumber: String?,
    notes: String?,
    service: ServiceName,
    numberOfBeds: Int?,
  ): AuthorisableActionResult<ValidatableActionResult<LostBedsEntity>> {
    val lostBed = lostBedsRepository.findByIdOrNull(lostBedId)
      ?: return AuthorisableActionResult.NotFound()

    return AuthorisableActionResult.Success(
      validated {
        if ((lostBed is ApprovedPremisesLostBedsEntity && service != ServiceName.approvedPremises) ||
          (lostBed is TemporaryAccommodationLostBedEntity && service != ServiceName.temporaryAccommodation)
        ) {
          "$.serviceName" hasValidationError "incorrectLostBedServiceScope"
        }

        if (endDate.isBefore(startDate)) {
          "$.endDate" hasValidationError "beforeStartDate"
        }

        if (service == ServiceName.approvedPremises && numberOfBeds!! <= 0) {
          "$.numberOfBeds" hasValidationError "isZero"
        }

        val reason = lostBedReasonRepository.findByIdOrNull(reasonId)
        if (reason == null) {
          "$.reason" hasValidationError "doesNotExist"
        } else if (!serviceScopeMatches(reason.serviceScope, lostBed.premises)) {
          "$.reason" hasValidationError "incorrectLostBedReasonServiceScope"
        }

        if (validationErrors.any()) {
          return@validated fieldValidationError
        }

        val updatedLostBedsEntity = when (service) {
          ServiceName.approvedPremises -> lostBedsRepository.save(
            ApprovedPremisesLostBedsEntity(
              id = lostBedId,
              premises = lostBed.premises,
              startDate = startDate,
              endDate = endDate,
              numberOfBeds = numberOfBeds!!,
              reason = reason!!,
              referenceNumber = referenceNumber,
              notes = notes,
              lostBedCancellation = null,
            )
          )
          ServiceName.temporaryAccommodation -> lostBedsRepository.save(
            TemporaryAccommodationLostBedEntity(
              id = lostBedId,
              premises = lostBed.premises,
              startDate = startDate,
              endDate = endDate,
              bed = (lostBed as TemporaryAccommodationLostBedEntity).bed,
              reason = reason!!,
              referenceNumber = referenceNumber,
              notes = notes,
              lostBedCancellation = null,
            )
          )
        }

        success(updatedLostBedsEntity)
      }
    )
  }

  fun cancelLostBed(
    lostBed: LostBedsEntity,
    notes: String?
  ) = validated<LostBedCancellationEntity> {
    if (lostBed.cancellation != null) {
      return generalError("This Lost Bed already has a cancellation set")
    }

    val cancellationEntity = lostBedCancellationRepository.save(
      LostBedCancellationEntity(
        id = UUID.randomUUID(),
        lostBed = lostBed,
        notes = notes,
        createdAt = OffsetDateTime.now(),
      )
    )

    return success(cancellationEntity)
  }

  fun createNewPremises(
    addressLine1: String,
    addressLine2: String?,
    town: String?,
    postcode: String,
    latitude: Double?,
    longitude: Double?,
    service: String,
    localAuthorityAreaId: UUID?,
    probationRegionId: UUID,
    name: String,
    notes: String?,
    characteristicIds: List<UUID>,
    status: PropertyStatus,
    pdu: String?,
  ) = validated<PremisesEntity> {

    val probationRegion = probationRegionRepository.findByIdOrNull(probationRegionId)
    if (probationRegion == null) {
      "$.probationRegionId" hasValidationError "doesNotExist"
    }

    val localAuthorityArea = if (localAuthorityAreaId == null) {
      if (service == ServiceName.approvedPremises.value) {
        "$.localAuthorityAreaId" hasValidationError "empty"
      }
      null
    } else {
      val localAuthorityArea = localAuthorityAreaRepository.findByIdOrNull(localAuthorityAreaId)
      if (localAuthorityArea == null) {
        "$.localAuthorityAreaId" hasValidationError "doesNotExist"
      }
      localAuthorityArea
    }

    // start of validation
    if (addressLine1.isEmpty()) {
      "$.address" hasValidationError "empty"
    }

    if (postcode.isEmpty()) {
      "$.postcode" hasValidationError "empty"
    }

    if (service.isEmpty()) {
      "$.service" hasValidationError "empty"
    } else if (service != ServiceName.temporaryAccommodation.value) {
      "$.service" hasValidationError "onlyCas3Supported"
    }

    if (name.isEmpty()) {
      "$.name" hasValidationError "empty"
    }

    if (!premisesRepository.nameIsUniqueForType(name, TemporaryAccommodationPremisesEntity::class.java)) {
      "$.name" hasValidationError "notUnique"
    }

    if (pdu.isNullOrBlank()) {
      "$.pdu" hasValidationError "empty"
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }

    val premises = TemporaryAccommodationPremisesEntity(
      id = UUID.randomUUID(),
      name = name,
      addressLine1 = addressLine1,
      addressLine2 = addressLine2,
      town = town,
      postcode = postcode,
      latitude = null,
      longitude = null,
      probationRegion = probationRegion!!,
      localAuthorityArea = localAuthorityArea,
      bookings = mutableListOf(),
      lostBeds = mutableListOf(),
      notes = if (notes.isNullOrEmpty()) "" else notes,
      totalBeds = 0,
      rooms = mutableListOf(),
      characteristics = mutableListOf(),
      status = status,
      pdu = pdu!!,
    )

    val characteristicEntities = characteristicIds.mapIndexed { index, uuid ->
      val entity = characteristicService.getCharacteristic(uuid)

      if (entity == null) {
        "$.characteristics[$index]" hasValidationError "doesNotExist"
      } else {
        if (!characteristicService.modelScopeMatches(entity, premises)) {
          "$.characteristics[$index]" hasValidationError "incorrectCharacteristicModelScope"
        }
        if (!characteristicService.serviceScopeMatches(entity, premises)) {
          "$.characteristics[$index]" hasValidationError "incorrectCharacteristicServiceScope"
        }
      }

      entity
    }

    if (validationErrors.any()) {
      return fieldValidationError
    }
    // end of validation
    premises.characteristics.addAll(characteristicEntities.map { it!! })
    premisesRepository.save(premises)

    return success(premises)
  }

  fun updatePremises(
    premisesId: UUID,
    addressLine1: String,
    addressLine2: String?,
    town: String?,
    postcode: String,
    localAuthorityAreaId: UUID?,
    probationRegionId: UUID,
    characteristicIds: List<UUID>,
    notes: String?,
    status: PropertyStatus,
    pdu: String?,
  ): AuthorisableActionResult<ValidatableActionResult<PremisesEntity>> {

    val premises = premisesRepository.findByIdOrNull(premisesId)
      ?: return AuthorisableActionResult.NotFound()

    val validationErrors = ValidationErrors()

    val localAuthorityArea = if (localAuthorityAreaId == null) {
      if (premises is ApprovedPremisesEntity) {
        validationErrors["$.localAuthorityAreaId"] = "empty"
      }
      null
    } else {
      val localAuthorityArea = localAuthorityAreaRepository.findByIdOrNull(localAuthorityAreaId)
      if (localAuthorityArea == null) {
        validationErrors["$.localAuthorityAreaId"] = "doesNotExist"
      }
      localAuthorityArea
    }

    val probationRegion = probationRegionRepository.findByIdOrNull(probationRegionId)

    if (probationRegion == null) {
      validationErrors["$.probationRegionId"] = "doesNotExist"
    }

    if (premises is TemporaryAccommodationPremisesEntity && pdu.isNullOrBlank()) {
      validationErrors["$.pdu"] = "empty"
    }

    val characteristicEntities = characteristicIds.mapIndexed { index, uuid ->
      val entity = characteristicService.getCharacteristic(uuid)

      if (entity == null) {
        validationErrors["$.characteristics[$index]"] = "doesNotExist"
      } else {
        if (!characteristicService.modelScopeMatches(entity, premises)) {
          validationErrors["$.characteristics[$index]"] = "incorrectCharacteristicModelScope"
        }
        if (!characteristicService.serviceScopeMatches(entity, premises)) {
          validationErrors["$.characteristics[$index]"] = "incorrectCharacteristicServiceScope"
        }
      }

      entity
    }

    if (validationErrors.any()) {
      return AuthorisableActionResult.Success(
        ValidatableActionResult.FieldValidationError(validationErrors)
      )
    }

    premises.let {
      it.addressLine1 = addressLine1
      it.addressLine2 = addressLine2
      it.town = town
      it.postcode = postcode
      it.localAuthorityArea = localAuthorityArea
      it.probationRegion = probationRegion!!
      it.characteristics = characteristicEntities.map { it!! }.toMutableList()
      it.notes = if (notes.isNullOrEmpty()) "" else notes
      it.status = status
      if (it is TemporaryAccommodationPremisesEntity) {
        it.pdu = pdu!!
      }
    }

    val savedPremises = premisesRepository.save(premises)

    return AuthorisableActionResult.Success(
      ValidatableActionResult.Success(savedPremises)
    )
  }

  private fun loadDependentEntities(
    premises: List<PremisesEntity>,
    loadProbationRegions: Boolean = false,
    loadProbationRegionApAreas: Boolean = false,
    loadLocalAuthorityAreas: Boolean = false,
    loadBookings: Boolean = false,
    loadLostBeds: Boolean = false,
    loadRooms: Boolean = false,
    loadRoomBeds: Boolean = false,
    loadCharacteristics: Boolean = false,
  ): List<PremisesEntity> {
    var resultPremises = premises

    if (loadProbationRegions) {
      resultPremises = premisesRepository.loadPremisesProbationRegions(resultPremises)
      resultPremises.forEach { it.probationRegionLoaded = true }
    }
    if (loadProbationRegionApAreas) {
      val probationRegions = probationRegionRepository
        .loadApAreas(resultPremises.map { it.probationRegion })
        .onEach { it.apAreaLoaded = true }
        .associateBy { it.id }

      resultPremises.forEach {
        it.probationRegion = probationRegions[it.probationRegion.id]!!
      }
    }

    if (loadLocalAuthorityAreas) {
      resultPremises = premisesRepository.loadPremisesLocalAuthorityAreas(resultPremises)
      resultPremises.forEach { it.localAuthorityAreaLoaded = true }
    }

    if (loadBookings) {
      resultPremises = premisesRepository.loadPremisesBookings(resultPremises)
      resultPremises.forEach { it.bookingsLoaded = true }
    }

    if (loadLostBeds) {
      resultPremises = premisesRepository.loadPremisesLostBeds(resultPremises)
      resultPremises.forEach { it.lostBedsLoaded = true }
    }

    if (loadRooms) {
      resultPremises = premisesRepository.loadPremisesRooms(resultPremises)
      resultPremises.forEach { it.roomsLoaded = true }
    }

    if (loadRoomBeds) {
      val rooms = roomRepository
        .loadRoomsBeds(resultPremises.flatMap { it.rooms })
        .onEach { it.bedsLoaded = true }
        .associateBy { it.id }

      resultPremises.forEach {
        it.rooms.replaceAll { r -> rooms[r.id]!! }
      }
    }

    if (loadCharacteristics) {
      resultPremises = premisesRepository.loadPremisesCharacteristics(resultPremises)
      resultPremises.forEach { it.characteristicsLoaded = true }
    }

    return resultPremises
  }

  private fun serviceScopeMatches(scope: String, premises: PremisesEntity) = when (scope) {
    "*" -> true
    ServiceName.approvedPremises.value -> premises is ApprovedPremisesEntity
    ServiceName.temporaryAccommodation.value -> premises is TemporaryAccommodationPremisesEntity
    else -> false
  }
}
