package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingCancelled
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingCancelledEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingChanged
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingChangedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMadeBookedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingNotMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cru
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MetaDataName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ApplicationFacade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.Cas1BookingCancelledEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.Cas1BookingChangedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.Cas1BookingCreatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.mapOfNonNullValues
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class Cas1BookingDomainEventService(
  val domainEventService: Cas1DomainEventService,
  val offenderService: OffenderService,
  val apDeliusContextApiClient: ApDeliusContextApiClient,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
) {

  fun spaceBookingMade(cas1BookingCreatedEvent: Cas1BookingCreatedEvent) {
    val booking = cas1BookingCreatedEvent.booking
    val user = cas1BookingCreatedEvent.createdBy
    val placementRequest = booking.placementRequest!!
    val application = placementRequest.application
    bookingMade(
      applicationId = application.id,
      eventNumber = application.eventNumber,
      bookingInfo = booking.toBookingInfo(),
      user = user,
      applicationSubmittedOn = application.submittedAt,
      releaseType = application.releaseType,
      sentenceType = application.sentenceType,
      situation = application.situation,
      placementRequestId = placementRequest.id,
    )
  }

  fun bookingMade(
    application: ApprovedPremisesApplicationEntity,
    booking: BookingEntity,
    user: UserEntity,
    placementRequest: PlacementRequestEntity,
  ) {
    bookingMade(
      applicationId = application.id,
      eventNumber = application.eventNumber,
      bookingInfo = booking.toBookingInfo(),
      user = user,
      applicationSubmittedOn = application.submittedAt,
      releaseType = application.releaseType,
      sentenceType = application.sentenceType,
      situation = application.situation,
      placementRequestId = placementRequest.id,
    )
  }

  fun bookingNotMade(
    user: UserEntity,
    placementRequest: PlacementRequestEntity,
    bookingNotCreatedAt: OffsetDateTime,
    notes: String?,
  ) {
    val domainEventId = UUID.randomUUID()

    val application = placementRequest.application

    val offenderDetails = getOffenderDetails(
      application.crn,
      user.deliusUsername,
      user.hasQualification(UserQualification.LAO),
    )

    val staffDetails = getStaffDetails(user.deliusUsername)

    domainEventService.saveBookingNotMadeEvent(
      SaveCas1DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = application.crn,
        nomsNumber = offenderDetails?.otherIds?.nomsNumber,
        occurredAt = bookingNotCreatedAt.toInstant(),
        data = BookingNotMadeEnvelope(
          id = domainEventId,
          timestamp = bookingNotCreatedAt.toInstant(),
          eventType = EventType.bookingNotMade,
          eventDetails = BookingNotMade(
            applicationId = application.id,
            applicationUrl = applicationUrlTemplate.resolve("id", application.id.toString()),
            personReference = PersonReference(
              crn = application.crn,
              noms = offenderDetails?.otherIds?.nomsNumber ?: "Unknown NOMS Number",
            ),
            deliusEventNumber = application.eventNumber,
            attemptedAt = bookingNotCreatedAt.toInstant(),
            attemptedBy = BookingMadeBookedBy(
              staffMember = staffDetails.toStaffMember(),
              cru = Cru(
                name = user.apArea?.name ?: "Unknown CRU",
              ),
            ),
            failureDescription = notes,
          ),
        ),
        metadata = mapOfNonNullValues(
          MetaDataName.CAS1_PLACEMENT_REQUEST_ID to placementRequest.id.toString(),
        ),
      ),
    )
  }

  fun spaceBookingChanged(
    bookingChanged: Cas1BookingChangedEvent,
  ) = bookingChanged(
    BookingChangedInfo(
      bookingId = bookingChanged.booking.id,
      crn = bookingChanged.booking.crn,
      arrivalDate = bookingChanged.booking.expectedArrivalDate,
      departureDate = bookingChanged.booking.expectedDepartureDate,
      applicationFacade = bookingChanged.booking.applicationFacade,
      approvedPremises = bookingChanged.booking.premises,
      changedAt = bookingChanged.bookingChangedAt,
      changedBy = bookingChanged.changedBy,
      previousArrivalDateIfChanged = bookingChanged.previousArrivalDateIfChanged,
      previousDepartureDateIfChanged = bookingChanged.previousDepartureDateIfChanged,
      isSpaceBooking = true,
      characteristics = bookingChanged.booking.criteria.toSpaceCharacteristics(),
      previousCharacteristics = bookingChanged.previousCharacteristicsIfChanged?.toSpaceCharacteristics(),
    ),
  )

  fun bookingChanged(
    booking: BookingEntity,
    changedBy: UserEntity,
    bookingChangedAt: OffsetDateTime,
    previousArrivalDateIfChanged: LocalDate?,
    previousDepartureDateIfChanged: LocalDate?,
  ) = bookingChanged(
    BookingChangedInfo(
      bookingId = booking.id,
      crn = booking.crn,
      arrivalDate = booking.arrivalDate,
      departureDate = booking.departureDate,
      applicationFacade = booking.cas1ApplicationFacade,
      approvedPremises = booking.premises as ApprovedPremisesEntity,
      changedAt = bookingChangedAt,
      changedBy = changedBy,
      previousArrivalDateIfChanged = previousArrivalDateIfChanged,
      previousDepartureDateIfChanged = previousDepartureDateIfChanged,
      isSpaceBooking = false,
    ),
  )

  private fun bookingChanged(
    bookingChangedInfo: BookingChangedInfo,
  ) {
    val domainEventId = UUID.randomUUID()
    val applicationFacade = bookingChangedInfo.applicationFacade
    val applicationId = applicationFacade.id
    val eventNumber = applicationFacade.eventNumber!!
    val crn = bookingChangedInfo.crn
    val changedBy = bookingChangedInfo.changedBy
    val changedAt = bookingChangedInfo.changedAt
    val bookingId = bookingChangedInfo.bookingId

    val offenderDetails = getOffenderDetails(
      crn,
      changedBy.deliusUsername,
      ignoreLaoRestrictions = true,
    )

    val staffDetails = when (val staffDetailsResult = apDeliusContextApiClient.getStaffDetail(changedBy.deliusUsername)) {
      is ClientResult.Success -> staffDetailsResult.body
      is ClientResult.Failure -> staffDetailsResult.throwException()
    }

    val approvedPremises = bookingChangedInfo.approvedPremises

    domainEventService.saveBookingChangedEvent(
      SaveCas1DomainEvent(
        id = domainEventId,
        applicationId = applicationId,
        crn = crn,
        nomsNumber = offenderDetails?.otherIds?.nomsNumber,
        occurredAt = changedAt.toInstant(),
        schemaVersion = 2,
        bookingId = if (bookingChangedInfo.isSpaceBooking) {
          null
        } else {
          bookingId
        },
        cas1SpaceBookingId = if (bookingChangedInfo.isSpaceBooking) {
          bookingId
        } else {
          null
        },
        data = BookingChangedEnvelope(
          id = domainEventId,
          timestamp = changedAt.toInstant(),
          eventType = EventType.bookingChanged,
          eventDetails = BookingChanged(
            applicationId = applicationId,
            applicationUrl = applicationUrlTemplate.resolve("id", applicationId.toString()),
            bookingId = bookingId,
            personReference = PersonReference(
              crn = crn,
              noms = offenderDetails?.otherIds?.nomsNumber ?: "Unknown NOMS Number",
            ),
            deliusEventNumber = eventNumber,
            changedAt = changedAt.toInstant(),
            changedBy = staffDetails.toStaffMember(),
            premises = Premises(
              id = approvedPremises.id,
              name = approvedPremises.name,
              apCode = approvedPremises.apCode,
              legacyApCode = approvedPremises.qCode,
              localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name,
            ),
            arrivalOn = bookingChangedInfo.arrivalDate,
            departureOn = bookingChangedInfo.departureDate,
            characteristics = bookingChangedInfo.characteristics,
            previousArrivalOn = bookingChangedInfo.previousArrivalDateIfChanged,
            previousDepartureOn = bookingChangedInfo.previousDepartureDateIfChanged,
            previousCharacteristics = bookingChangedInfo.previousCharacteristics,
          ),
        ),
      ),
    )
  }

  fun bookingCancelled(
    booking: BookingEntity,
    user: UserEntity,
    cancellation: CancellationEntity,
    reason: CancellationReasonEntity,
  ) = bookingCancelled(
    CancellationInfo(
      bookingId = booking.id,
      applicationFacade = booking.cas1ApplicationFacade,
      cancellationId = cancellation.id,
      crn = booking.crn,
      cancelledAt = cancellation.date,
      reason = reason,
      cancelledBy = user,
      premises = booking.premises as ApprovedPremisesEntity,
      isSpaceBooking = false,
    ),
  )

  fun spaceBookingCancelled(bookingCancelled: Cas1BookingCancelledEvent) = bookingCancelled(
    CancellationInfo(
      bookingId = bookingCancelled.booking.id,
      applicationFacade = bookingCancelled.booking.applicationFacade,
      cancellationId = null,
      crn = bookingCancelled.booking.crn,
      cancelledAt = bookingCancelled.booking.cancellationOccurredAt!!,
      reason = bookingCancelled.reason,
      cancelledBy = bookingCancelled.user,
      premises = bookingCancelled.booking.premises,
      isSpaceBooking = true,
    ),
  )

  private fun bookingMade(
    applicationId: UUID,
    eventNumber: String,
    bookingInfo: BookingInfo,
    user: UserEntity,
    applicationSubmittedOn: OffsetDateTime?,
    sentenceType: String?,
    releaseType: String?,
    situation: String?,
    placementRequestId: UUID?,
  ) {
    val domainEventId = UUID.randomUUID()
    val crn = bookingInfo.crn

    val offenderDetails =
      when (val offenderDetailsResult = offenderService.getOffenderByCrn(crn, user.deliusUsername, true)) {
        is AuthorisableActionResult.Success -> offenderDetailsResult.entity
        else -> null
      }

    val staffDetails = getStaffDetails(user.deliusUsername)

    val approvedPremises = bookingInfo.premises
    val bookingCreatedAt = bookingInfo.createdAt
    val isSpaceBooking = bookingInfo.isSpaceBooking

    domainEventService.saveBookingMadeDomainEvent(
      SaveCas1DomainEvent(
        id = domainEventId,
        applicationId = applicationId,
        crn = crn,
        nomsNumber = offenderDetails?.otherIds?.nomsNumber,
        occurredAt = bookingCreatedAt.toInstant(),
        bookingId = if (isSpaceBooking) {
          null
        } else {
          bookingInfo.id
        },
        cas1SpaceBookingId = if (isSpaceBooking) {
          bookingInfo.id
        } else {
          null
        },
        schemaVersion = 2,
        data = BookingMadeEnvelope(
          id = domainEventId,
          timestamp = bookingCreatedAt.toInstant(),
          eventType = EventType.bookingMade,
          eventDetails = BookingMade(
            applicationId = applicationId,
            applicationUrl = applicationUrlTemplate.resolve("id", applicationId.toString()),
            bookingId = bookingInfo.id,
            personReference = PersonReference(
              crn = crn,
              noms = offenderDetails?.otherIds?.nomsNumber ?: "Unknown NOMS Number",
            ),
            deliusEventNumber = eventNumber,
            createdAt = bookingCreatedAt.toInstant(),
            bookedBy = BookingMadeBookedBy(
              staffMember = staffDetails.toStaffMember(),
              cru = Cru(
                name = user.apArea?.name ?: "Unknown CRU",
              ),
            ),
            premises = Premises(
              id = approvedPremises.id,
              name = approvedPremises.name,
              apCode = approvedPremises.apCode,
              legacyApCode = approvedPremises.qCode,
              localAuthorityAreaName = approvedPremises.localAuthorityArea!!.name,
            ),
            arrivalOn = bookingInfo.arrivalDate,
            departureOn = bookingInfo.departureDate,
            applicationSubmittedOn = applicationSubmittedOn?.toInstant(),
            releaseType = releaseType,
            sentenceType = sentenceType,
            situation = situation,
            characteristics = bookingInfo.characteristics,
          ),
        ),
        metadata = mapOfNonNullValues(
          MetaDataName.CAS1_PLACEMENT_REQUEST_ID to placementRequestId?.toString(),
        ),
      ),
    )
  }

  private fun bookingCancelled(
    cancellationInfo: CancellationInfo,
  ) {
    val bookingId = cancellationInfo.bookingId
    val now = OffsetDateTime.now()
    val user = cancellationInfo.cancelledBy
    val crn = cancellationInfo.crn
    val premises = cancellationInfo.premises
    val isSpaceBooking = cancellationInfo.isSpaceBooking

    val domainEventId = UUID.randomUUID()

    val offenderDetails =
      when (val offenderDetailsResult = offenderService.getOffenderByCrn(crn, user.deliusUsername, user.hasQualification(UserQualification.LAO))) {
        is AuthorisableActionResult.Success -> offenderDetailsResult.entity
        else -> null
      }

    val staffDetails = getStaffDetails(user.deliusUsername)

    val applicationId = cancellationInfo.applicationFacade.id
    val eventNumber = cancellationInfo.applicationFacade.eventNumber!!

    domainEventService.saveBookingCancelledEvent(
      SaveCas1DomainEvent(
        id = domainEventId,
        applicationId = applicationId,
        crn = crn,
        nomsNumber = offenderDetails?.otherIds?.nomsNumber,
        occurredAt = now.toInstant(),
        bookingId = if (isSpaceBooking) {
          null
        } else {
          bookingId
        },
        cas1SpaceBookingId = if (isSpaceBooking) {
          bookingId
        } else {
          null
        },
        schemaVersion = 2,
        data = BookingCancelledEnvelope(
          id = domainEventId,
          timestamp = now.toInstant(),
          eventType = EventType.bookingCancelled,
          eventDetails = BookingCancelled(
            applicationId = applicationId,
            applicationUrl = applicationUrlTemplate.resolve("id", applicationId.toString()),
            bookingId = bookingId,
            personReference = PersonReference(
              crn = crn,
              noms = offenderDetails?.otherIds?.nomsNumber ?: "Unknown NOMS Number",
            ),
            deliusEventNumber = eventNumber,
            premises = Premises(
              id = premises.id,
              name = premises.name,
              apCode = premises.apCode,
              legacyApCode = premises.qCode,
              localAuthorityAreaName = premises.localAuthorityArea!!.name,
            ),
            cancelledBy = staffDetails.toStaffMember(),
            cancelledAt = cancellationInfo.cancelledAt.atTime(0, 0).toInstant(ZoneOffset.UTC),
            cancelledAtDate = cancellationInfo.cancelledAt,
            cancellationReason = cancellationInfo.reason.name,
            cancellationRecordedAt = now.toInstant(),
          ),
        ),
        metadata = mapOfNonNullValues(
          MetaDataName.CAS1_CANCELLATION_ID to cancellationInfo.cancellationId?.toString(),
        ),
      ),
    )
  }

  private fun getOffenderDetails(
    crn: String,
    deliusUsername: String,
    ignoreLaoRestrictions: Boolean,
  ) = when (val offenderDetailsResult = offenderService.getOffenderByCrn(crn, deliusUsername, ignoreLaoRestrictions)) {
    is AuthorisableActionResult.Success -> offenderDetailsResult.entity
    else -> null
  }

  private fun getStaffDetails(deliusUsername: String) = when (val staffDetailsResult = apDeliusContextApiClient.getStaffDetail(deliusUsername)) {
    is ClientResult.Success -> staffDetailsResult.body
    is ClientResult.Failure -> staffDetailsResult.throwException()
  }

  private data class BookingInfo(
    val id: UUID,
    val createdAt: OffsetDateTime,
    val crn: String,
    val premises: ApprovedPremisesEntity,
    val arrivalDate: LocalDate,
    val departureDate: LocalDate,
    val isSpaceBooking: Boolean,
    val characteristics: List<SpaceCharacteristic>? = null,
  )

  private fun BookingEntity.toBookingInfo() = BookingInfo(
    id = id,
    createdAt = createdAt,
    crn = crn,
    premises = premises as ApprovedPremisesEntity,
    arrivalDate = arrivalDate,
    departureDate = departureDate,
    isSpaceBooking = false,
  )

  private fun Cas1SpaceBookingEntity.toBookingInfo() = BookingInfo(
    id = id,
    createdAt = createdAt,
    crn = crn,
    premises = premises,
    arrivalDate = canonicalArrivalDate,
    departureDate = canonicalDepartureDate,
    isSpaceBooking = true,
    characteristics = criteria.toSpaceCharacteristics(),
  )

  private data class CancellationInfo(
    val bookingId: UUID,
    val applicationFacade: Cas1ApplicationFacade,
    val crn: String,
    val premises: ApprovedPremisesEntity,
    val cancellationId: UUID?,
    val cancelledBy: UserEntity,
    val cancelledAt: LocalDate,
    val reason: CancellationReasonEntity,
    val isSpaceBooking: Boolean,
  )

  private data class BookingChangedInfo(
    val bookingId: UUID,
    val crn: String,
    val arrivalDate: LocalDate,
    val departureDate: LocalDate,
    val approvedPremises: ApprovedPremisesEntity,
    val applicationFacade: Cas1ApplicationFacade,
    val changedBy: UserEntity,
    val changedAt: OffsetDateTime,
    val previousArrivalDateIfChanged: LocalDate?,
    val previousDepartureDateIfChanged: LocalDate?,
    val isSpaceBooking: Boolean,
    val characteristics: List<SpaceCharacteristic>? = null,
    val previousCharacteristics: List<SpaceCharacteristic>? = null,
  )

  private fun List<CharacteristicEntity>.toSpaceCharacteristics(): List<SpaceCharacteristic> = this.map { it.asSpaceCharacteristic() }

  private fun CharacteristicEntity.asSpaceCharacteristic() = SpaceCharacteristic.entries.first { it.value == this.propertyName }
}
