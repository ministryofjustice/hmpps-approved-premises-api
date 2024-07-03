package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMadeBookedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingNotMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Cru
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas1BookingDomainEventService(
  val domainEventService: DomainEventService,
  val offenderService: OffenderService,
  val communityApiClient: CommunityApiClient,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
) {

  fun bookingMade(
    application: ApprovedPremisesApplicationEntity,
    booking: BookingEntity,
    user: UserEntity,
  ) {
    bookingMade(
      applicationId = application.id,
      eventNumber = application.eventNumber,
      booking = booking,
      user = user,
      applicationSubmittedOn = application.submittedAt,
      releaseType = application.releaseType,
      sentenceType = application.sentenceType,
      situation = application.situation,
    )
  }

  fun adhocBookingMade(
    onlineApplication: ApprovedPremisesApplicationEntity?,
    offlineApplication: OfflineApplicationEntity?,
    eventNumber: String?,
    booking: BookingEntity,
    user: UserEntity,
  ) {
    val applicationId = (onlineApplication?.id ?: offlineApplication?.id)
    val eventNumberForDomainEvent =
      (onlineApplication?.eventNumber ?: offlineApplication?.eventNumber ?: eventNumber)

    bookingMade(
      applicationId = applicationId!!,
      eventNumber = eventNumberForDomainEvent!!,
      booking = booking,
      user = user,
      applicationSubmittedOn = onlineApplication?.submittedAt,
      releaseType = onlineApplication?.releaseType,
      sentenceType = onlineApplication?.sentenceType,
      situation = onlineApplication?.situation,
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
      DomainEvent(
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

  private fun getStaffDetails(deliusUsername: String) =
    when (val staffDetailsResult = communityApiClient.getStaffUserDetails(deliusUsername)) {
      is ClientResult.Success -> staffDetailsResult.body
      is ClientResult.Failure -> staffDetailsResult.throwException()
    }

  private fun bookingMade(
    applicationId: UUID,
    eventNumber: String,
    booking: BookingEntity,
    user: UserEntity,
    applicationSubmittedOn: OffsetDateTime?,
    sentenceType: String?,
    releaseType: String?,
    situation: String?,
  ) {
    val domainEventId = UUID.randomUUID()

    val offenderDetails =
      when (val offenderDetailsResult = offenderService.getOffenderByCrn(booking.crn, user.deliusUsername, true)) {
        is AuthorisableActionResult.Success -> offenderDetailsResult.entity
        else -> null
      }

    val staffDetails = getStaffDetails(user.deliusUsername)

    val approvedPremises = booking.premises as ApprovedPremisesEntity
    val bookingCreatedAt = booking.createdAt

    domainEventService.saveBookingMadeDomainEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = applicationId,
        crn = booking.crn,
        nomsNumber = offenderDetails?.otherIds?.nomsNumber,
        occurredAt = bookingCreatedAt.toInstant(),
        bookingId = booking.id,
        data = BookingMadeEnvelope(
          id = domainEventId,
          timestamp = bookingCreatedAt.toInstant(),
          eventType = EventType.bookingMade,
          eventDetails = BookingMade(
            applicationId = applicationId,
            applicationUrl = applicationUrlTemplate.resolve("id", applicationId.toString()),
            bookingId = booking.id,
            personReference = PersonReference(
              crn = booking.crn,
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
            arrivalOn = booking.arrivalDate,
            departureOn = booking.departureDate,
            applicationSubmittedOn = applicationSubmittedOn?.toInstant(),
            releaseType = releaseType,
            sentenceType = sentenceType,
            situation = situation,
          ),
        ),
      ),
    )
  }
}
