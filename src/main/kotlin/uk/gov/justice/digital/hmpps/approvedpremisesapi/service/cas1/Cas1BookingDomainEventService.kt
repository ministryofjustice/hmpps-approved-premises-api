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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventTransferInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventTransferType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MetaDataName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TransferType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent.toEventBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.Cas1BookingCancelledEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.Cas1BookingChangedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.Cas1BookingCreatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.TransferInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.mapOfNonNullValues
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

    val domainEventId = UUID.randomUUID()
    val crn = booking.crn

    val offenderDetails = getOffenderDetails(crn, LaoStrategy.NeverRestricted)

    val staffDetails = getStaffDetails(user.deliusUsername)
    val approvedPremises = booking.premises
    val bookingCreatedAt = booking.createdAt

    domainEventService.saveBookingMadeDomainEvent(
      SaveCas1DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = crn,
        nomsNumber = offenderDetails?.nomsId,
        occurredAt = bookingCreatedAt.toInstant(),
        bookingId = null,
        cas1SpaceBookingId = booking.id,
        schemaVersion = 2,
        data = BookingMadeEnvelope(
          id = domainEventId,
          timestamp = bookingCreatedAt.toInstant(),
          eventType = EventType.bookingMade,
          eventDetails = BookingMade(
            applicationId = application.id,
            applicationUrl = applicationUrlTemplate.resolve("id", application.id.toString()),
            bookingId = booking.id,
            personReference = PersonReference(
              crn = crn,
              noms = offenderDetails?.nomsId ?: "Unknown NOMS Number",
            ),
            deliusEventNumber = application.eventNumber,
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
            arrivalOn = booking.canonicalArrivalDate,
            departureOn = booking.canonicalDepartureDate,
            applicationSubmittedOn = application.submittedAt?.toInstant(),
            releaseType = application.releaseType,
            sentenceType = application.sentenceType,
            situation = application.situation,
            characteristics = booking.criteria.toSpaceCharacteristics(),
            transferredFrom = cas1BookingCreatedEvent.transferredFrom?.toEventTransferInfo(),
          ),
        ),
        metadata = mapOfNonNullValues(
          MetaDataName.CAS1_PLACEMENT_REQUEST_ID to placementRequest.id.toString(),
        ),
      ),
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
      user.cas1LaoStrategy(),
    )

    val staffDetails = getStaffDetails(user.deliusUsername)

    domainEventService.saveBookingNotMadeEvent(
      SaveCas1DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = application.crn,
        nomsNumber = offenderDetails?.nomsId,
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
              noms = offenderDetails?.nomsId ?: "Unknown NOMS Number",
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

  fun spaceBookingChanged(bookingChanged: Cas1BookingChangedEvent) {
    val booking = bookingChanged.booking
    val application = booking.applicationFacade
    val domainEventId = UUID.randomUUID()

    val offenderDetails = getOffenderDetails(booking.crn, LaoStrategy.NeverRestricted)

    val staffDetails = getStaffDetails(bookingChanged.changedBy.deliusUsername)

    domainEventService.saveBookingChangedEvent(
      SaveCas1DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = booking.crn,
        nomsNumber = offenderDetails?.nomsId,
        occurredAt = bookingChanged.bookingChangedAt.toInstant(),
        schemaVersion = 2,
        bookingId = booking.id,
        cas1SpaceBookingId = booking.id,
        data = BookingChangedEnvelope(
          id = domainEventId,
          timestamp = bookingChanged.bookingChangedAt.toInstant(),
          eventType = EventType.bookingChanged,
          eventDetails = BookingChanged(
            applicationId = application.id,
            applicationUrl = applicationUrlTemplate.resolve("id", application.id.toString()),
            bookingId = booking.id,
            personReference = PersonReference(
              crn = booking.crn,
              noms = offenderDetails?.nomsId ?: "Unknown NOMS Number",
            ),
            deliusEventNumber = application.eventNumber!!,
            changedAt = bookingChanged.bookingChangedAt.toInstant(),
            changedBy = staffDetails.toStaffMember(),
            premises = Premises(
              id = booking.premises.id,
              name = booking.premises.name,
              apCode = booking.premises.apCode,
              legacyApCode = booking.premises.qCode,
              localAuthorityAreaName = booking.premises.localAuthorityArea!!.name,
            ),
            arrivalOn = booking.expectedArrivalDate,
            departureOn = booking.expectedDepartureDate,
            characteristics = booking.criteria.toSpaceCharacteristics(),
            previousArrivalOn = bookingChanged.previousArrivalDateIfChanged,
            previousDepartureOn = bookingChanged.previousDepartureDateIfChanged,
            previousCharacteristics = bookingChanged.previousCharacteristicsIfChanged?.toSpaceCharacteristics(),
            transferredTo = bookingChanged.transferredTo?.toEventTransferInfo(),
          ),
        ),
      ),
    )
  }

  fun spaceBookingCancelled(bookingCancelled: Cas1BookingCancelledEvent) {
    val booking = bookingCancelled.booking
    val application = booking.applicationFacade
    val now = OffsetDateTime.now()

    val offenderDetails = getOffenderDetails(booking.crn, bookingCancelled.user.cas1LaoStrategy())

    val staffDetails = getStaffDetails(bookingCancelled.user.deliusUsername)

    domainEventService.saveBookingCancelledEvent(
      SaveCas1DomainEvent(
        id = UUID.randomUUID(),
        applicationId = application.id,
        crn = booking.crn,
        nomsNumber = offenderDetails?.nomsId,
        occurredAt = now.toInstant(),
        bookingId = null,
        cas1SpaceBookingId = booking.id,
        schemaVersion = 2,
        data = BookingCancelledEnvelope(
          id = UUID.randomUUID(),
          timestamp = now.toInstant(),
          eventType = EventType.bookingCancelled,
          eventDetails = BookingCancelled(
            applicationId = application.id,
            applicationUrl = applicationUrlTemplate.resolve("id", application.id.toString()),
            bookingId = booking.id,
            personReference = PersonReference(
              crn = booking.crn,
              noms = offenderDetails?.nomsId ?: "Unknown NOMS Number",
            ),
            deliusEventNumber = application.eventNumber!!,
            premises = Premises(
              id = booking.premises.id,
              name = booking.premises.name,
              apCode = booking.premises.apCode,
              legacyApCode = booking.premises.qCode,
              localAuthorityAreaName = booking.premises.localAuthorityArea!!.name,
            ),
            cancelledBy = staffDetails.toStaffMember(),
            cancelledAt = bookingCancelled.booking.cancellationOccurredAt!!.atTime(0, 0).toInstant(ZoneOffset.UTC),
            cancelledAtDate = bookingCancelled.booking.cancellationOccurredAt!!,
            cancellationReason = bookingCancelled.reason.name,
            cancellationRecordedAt = now.toInstant(),
            appealChangeRequestId = bookingCancelled.appealChangeRequestId,
          ),
        ),
        metadata = mapOfNonNullValues(
          MetaDataName.CAS1_CANCELLATION_ID to bookingCancelled.appealChangeRequestId?.toString(),
        ),
      ),
    )
  }

  private fun getOffenderDetails(offenderCrn: String, laoStrategy: LaoStrategy): CaseSummary? {
    val offenderDetails = offenderService.getPersonSummaryInfoResult(
      offenderCrn,
      laoStrategy,
    ).let { offenderDetailsResult ->
      when (offenderDetailsResult) {
        is PersonSummaryInfoResult.Success.Full -> offenderDetailsResult.summary
        else -> null
      }
    }

    return offenderDetails
  }

  private fun getStaffDetails(deliusUsername: String) = when (val staffDetailsResult = apDeliusContextApiClient.getStaffDetail(deliusUsername)) {
    is ClientResult.Success -> staffDetailsResult.body
    is ClientResult.Failure -> staffDetailsResult.throwException()
  }

  private fun TransferInfo.toEventTransferInfo() = EventTransferInfo(
    type = when (this.type) {
      TransferType.PLANNED -> EventTransferType.PLANNED
      TransferType.EMERGENCY -> EventTransferType.EMERGENCY
    },
    changeRequestId = this.changeRequestId,
    booking = this.booking.toEventBookingSummary(),
  )

  private fun List<CharacteristicEntity>.toSpaceCharacteristics(): List<SpaceCharacteristic> = this.map { it.asSpaceCharacteristic() }

  private fun CharacteristicEntity.asSpaceCharacteristic() = SpaceCharacteristic.entries.first { it.value == this.propertyName }
}
