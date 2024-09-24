package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.MoveOnCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonArrived
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonDeparted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonDepartedDestination
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
class Cas1SpaceBookingManagementDomainEventService(
  val domainEventService: DomainEventService,
  val offenderService: OffenderService,
  val communityApiClient: CommunityApiClient,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
) {

  fun arrivalRecorded(
    updatedCas1SpaceBooking: Cas1SpaceBookingEntity,
    previousExpectedDepartureOn: LocalDate? = null,
  ) {
    val domainEventId = UUID.randomUUID()

    val application = updatedCas1SpaceBooking.application
    val premises = updatedCas1SpaceBooking.premises
    val offenderDetails = getOffenderForCrn(updatedCas1SpaceBooking.crn)
    val keyWorker = getStaffMemberDetails(updatedCas1SpaceBooking.keyWorkerStaffCode)

    val actualArrivalDate = updatedCas1SpaceBooking.actualArrivalDateTime!!

    domainEventService.savePersonArrivedEvent(
      emit = false,
      domainEvent = DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = updatedCas1SpaceBooking.crn,
        nomsNumber = offenderDetails?.nomsId,
        occurredAt = actualArrivalDate,
        cas1SpaceBookingId = updatedCas1SpaceBooking.id,
        bookingId = null,
        data = PersonArrivedEnvelope(
          id = domainEventId,
          timestamp = OffsetDateTime.now().toInstant(),
          eventType = EventType.personArrived,
          eventDetails = PersonArrived(
            applicationId = application.id,
            applicationUrl = applicationUrlTemplate.resolve("id", application.id.toString()),
            bookingId = updatedCas1SpaceBooking.id,
            personReference = PersonReference(
              crn = updatedCas1SpaceBooking.crn,
              noms = offenderDetails?.nomsId ?: "Unknown NOMS Id",
            ),
            deliusEventNumber = application.eventNumber,
            premises = Premises(
              id = premises.id,
              name = premises.name,
              apCode = premises.apCode,
              legacyApCode = premises.qCode,
              localAuthorityAreaName = premises.localAuthorityArea!!.name,
            ),
            applicationSubmittedOn = application.submittedAt!!.toLocalDate(),
            keyWorker = keyWorker,
            arrivedAt = actualArrivalDate,
            expectedDepartureOn = updatedCas1SpaceBooking.expectedDepartureDate,
            previousExpectedDepartureOn = previousExpectedDepartureOn,
            notes = null,
          ),
        ),
      ),
    )
  }

  fun departureRecorded(
    departedCas1SpaceBooking: Cas1SpaceBookingEntity,
    departureReason: DepartureReasonEntity,
    moveOnCategory: MoveOnCategoryEntity,
  ) {
    val domainEventId = UUID.randomUUID()

    val application = departedCas1SpaceBooking.application
    val premises = departedCas1SpaceBooking.premises
    val offenderDetails = getOffenderForCrn(departedCas1SpaceBooking.crn)
    val keyWorker = getStaffMemberDetails(departedCas1SpaceBooking.keyWorkerStaffCode)

    val actualDepartureDate = departedCas1SpaceBooking.actualDepartureDateTime!!

    domainEventService.savePersonDepartedEvent(
      emit = false,
      domainEvent = DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = departedCas1SpaceBooking.crn,
        nomsNumber = offenderDetails?.nomsId,
        occurredAt = actualDepartureDate,
        cas1SpaceBookingId = departedCas1SpaceBooking.id,
        bookingId = null,
        data = PersonDepartedEnvelope(
          id = domainEventId,
          timestamp = OffsetDateTime.now().toInstant(),
          eventType = EventType.personDeparted,
          eventDetails = PersonDeparted(
            applicationId = application.id,
            applicationUrl = applicationUrlTemplate.resolve("id", application.id.toString()),
            bookingId = departedCas1SpaceBooking.id,
            personReference = PersonReference(
              crn = departedCas1SpaceBooking.crn,
              noms = offenderDetails?.nomsId ?: "Unknown NOMS Id",
            ),
            deliusEventNumber = application.eventNumber,
            premises = Premises(
              id = premises.id,
              name = premises.name,
              apCode = premises.apCode,
              legacyApCode = premises.qCode,
              localAuthorityAreaName = premises.localAuthorityArea!!.name,
            ),
            keyWorker = keyWorker!!,
            departedAt = actualDepartureDate,
            reason = departureReason.name,
            legacyReasonCode = departureReason.legacyDeliusReasonCode!!,
            destination = PersonDepartedDestination(
              moveOnCategory = MoveOnCategory(
                description = moveOnCategory.name,
                legacyMoveOnCategoryCode = moveOnCategory.legacyDeliusCategoryCode!!,
                id = moveOnCategory.id,
              ),
            ),
          ),
        ),
      ),
    )
  }

  private fun getStaffMemberDetails(staffCode: String?): StaffMember? {
    val staffMember = staffCode?.let {
      val staffMemberDetailsResult =
        communityApiClient.getStaffUserDetailsForStaffCode(staffCode!!)
      when (staffMemberDetailsResult) {
        is ClientResult.Success -> {
          val keyWorker = staffMemberDetailsResult.body
          StaffMember(
            staffCode = keyWorker.staffCode!!,
            staffIdentifier = keyWorker.staffIdentifier,
            forenames = keyWorker.staff.forenames,
            surname = keyWorker.staff.surname,
            username = null,
          )
        }
        is ClientResult.Failure -> staffMemberDetailsResult.throwException()
      }
    } ?: null
    return staffMember
  }

  private fun getOffenderForCrn(offenderCrn: String): CaseSummary? {
    val offenderDetails =
      when (
        val offenderDetailsResult =
          offenderService.getPersonSummaryInfoResults(
            setOf(offenderCrn),
            OffenderService.LimitedAccessStrategy.IgnoreLimitedAccess,
          )
            .firstOrNull()
      ) {
        is PersonSummaryInfoResult.Success.Full -> offenderDetailsResult.summary
        else -> null
      }
    return offenderDetails
  }
}
