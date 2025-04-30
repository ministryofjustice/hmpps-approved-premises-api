package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationSubmitted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationWithdrawn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AssessmentAllocated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AssessmentAppealed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingCancelled
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingChanged
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Cas1DomainEventPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.FurtherInformationRequested
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.MatchRequestWithdrawn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonArrived
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonDeparted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonNotArrived
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementApplicationAllocated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementApplicationWithdrawn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Problem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementAssessed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.GetCas1DomainEvent
import java.util.UUID

@SuppressWarnings("TooManyFunctions", "CyclomaticComplexMethod", "TooGenericExceptionThrown")
@RestController
@RequestMapping("\${api.base-path:}")
class Cas1DomainEventsController(
  private val domainEventService: Cas1DomainEventService,
) {

  @Operation(
    summary = "An 'application-assessed' event",
    operationId = "eventsApplicationAssessedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'application-assessed' event corresponding to the provided `eventId`",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No application-assessed event found for the provided `eventId`",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/events/application-assessed/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsApplicationAssessedEventIdGet(
    @Parameter(description = "UUID of the event", required = true)
    @PathVariable("eventId")
    eventId: UUID,
  ): ResponseEntity<Cas1DomainEventEnvelope<ApplicationAssessed>> = getDomainEvent(eventId, domainEventService::getApplicationAssessedDomainEvent)

  @Operation(
    summary = "An application-submitted event",
    operationId = "eventsApplicationSubmittedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The application-submitted corresponding to the provided `eventId`",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No application-submitted event found for the provided `eventId`",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/events/application-submitted/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsApplicationSubmittedEventIdGet(
    @Parameter(description = "UUID of the event", required = true)
    @PathVariable("eventId")
    eventId: UUID,
  ): ResponseEntity<Cas1DomainEventEnvelope<ApplicationSubmitted>> = getDomainEvent(eventId, domainEventService::getApplicationSubmittedDomainEvent)

  @Operation(
    summary = "An application-withdrawn event",
    operationId = "eventsApplicationWithdrawnEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The application-withdrawn event corresponding to the provided `eventId`",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No application-withdrawn event found for the provided `eventId`",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/events/application-withdrawn/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsApplicationWithdrawnEventIdGet(
    @Parameter(description = "UUID of the event", required = true)
    @PathVariable("eventId") eventId: UUID,
  ): ResponseEntity<Cas1DomainEventEnvelope<ApplicationWithdrawn>> = getDomainEvent(eventId, domainEventService::getApplicationWithdrawnEvent)

  @Operation(
    summary = "An assessment-allocated event",
    operationId = "eventsAssessmentAllocatedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The assessment-allocated event corresponding to the provided `eventId`",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No assessment-allocated event found for the provided `eventId`",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/events/assessment-allocated/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsAssessmentAllocatedEventIdGet(
    @Parameter(description = "UUID of the event", required = true)
    @PathVariable("eventId") eventId: UUID,
  ): ResponseEntity<Cas1DomainEventEnvelope<AssessmentAllocated>> = getDomainEvent(eventId, domainEventService::getAssessmentAllocatedEvent)

  @Operation(
    summary = "An 'assessment-appealed' event",
    operationId = "eventsAssessmentAppealedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'assessment-appealed' event corresponding to the provided `eventId`",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No assessment-appealed event found for the provided `eventId`",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/events/assessment-appealed/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsAssessmentAppealedEventIdGet(
    @Parameter(description = "UUID of the event", required = true)
    @PathVariable("eventId") eventId: UUID,
  ): ResponseEntity<Cas1DomainEventEnvelope<AssessmentAppealed>> = getDomainEvent(eventId, domainEventService::getAssessmentAppealedEvent)

  @Operation(
    summary = "A 'booking-cancelled' event",
    operationId = "eventsBookingCancelledEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'booking-cancelled' event corresponding to the provided `eventId`",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No 'booking-made' event found for the provided `eventId`",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/events/booking-cancelled/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsBookingCancelledEventIdGet(
    @Parameter(description = "UUID of the event", required = true)
    @PathVariable("eventId") eventId: UUID,
  ): ResponseEntity<Cas1DomainEventEnvelope<BookingCancelled>> = getDomainEvent(eventId, domainEventService::getBookingCancelledEvent)

  @Operation(
    summary = "A 'booking-changed' event",
    operationId = "eventsBookingChangedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'booking-changed' event corresponding to the provided `eventId`",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No 'booking-changed' event found for the provided `eventId`",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/events/booking-changed/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsBookingChangedEventIdGet(
    @Parameter(description = "UUID of the event", required = true)
    @PathVariable("eventId") eventId: UUID,
  ): ResponseEntity<Cas1DomainEventEnvelope<BookingChanged>> = getDomainEvent(eventId, domainEventService::getBookingChangedEvent)

  @Operation(
    summary = "A 'booking-made' event",
    operationId = "eventsBookingMadeEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'booking-made' event corresponding to the provided `eventId`",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No 'booking-made' event found for the provided `eventId`",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/events/booking-made/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsBookingMadeEventIdGet(
    @Parameter(description = "UUID of the event", required = true)
    @PathVariable("eventId") eventId: UUID,
  ): ResponseEntity<Cas1DomainEventEnvelope<BookingMade>> = getDomainEvent(eventId, domainEventService::getBookingMadeEvent)

  @Operation(
    summary = "A 'booking-not-made' event",
    operationId = "eventsBookingNotMadeEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'booking-not-made' event corresponding to the provided `eventId`",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No 'booking-not-made' event found for the provided `eventId`",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/events/booking-not-made/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsBookingNotMadeEventIdGet(
    @Parameter(description = "UUID of the event", required = true)
    @PathVariable("eventId") eventId: UUID,
  ): ResponseEntity<Cas1DomainEventEnvelope<BookingNotMade>> = getDomainEvent(eventId, domainEventService::getBookingNotMadeEvent)

  @Operation(
    summary = "A 'further-information-requested' event",
    operationId = "eventsFurtherInformationRequestedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'further-information-requested' event corresponding to the provided `eventId`",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No 'person-not-arrived' event found for the provided `eventId`",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/events/further-information-requested/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsFurtherInformationRequestedEventIdGet(
    @Parameter(description = "UUID of the event", required = true)
    @PathVariable("eventId") eventId: UUID,
  ): ResponseEntity<Cas1DomainEventEnvelope<FurtherInformationRequested>> = getDomainEvent(eventId, domainEventService::getFurtherInformationRequestMadeEvent)

  @Operation(
    summary = "A 'match-request-withdrawn' event",
    operationId = "eventsMatchRequestWithdrawnEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'match-request-withdrawn' event corresponding to the provided `eventId`",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No 'match-request-withdrawn' event found for the provided `eventId`",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/events/match-request-withdrawn/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsMatchRequestWithdrawnEventIdGet(
    @Parameter(description = "UUID of the event", required = true)
    @PathVariable("eventId") eventId: UUID,
  ): ResponseEntity<Cas1DomainEventEnvelope<MatchRequestWithdrawn>> = getDomainEvent(eventId, domainEventService::getMatchRequestWithdrawnEvent)

  @Operation(
    summary = "A 'person-arrived' event",
    operationId = "eventsPersonArrivedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'person-arrived' event corresponding to the provided `eventId`",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No 'person-arrived' event found for the provided `eventId`",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/events/person-arrived/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsPersonArrivedEventIdGet(
    @Parameter(description = "UUID of the event", required = true)
    @PathVariable("eventId") eventId: UUID,
  ): ResponseEntity<Cas1DomainEventEnvelope<PersonArrived>> = getDomainEvent(eventId, domainEventService::getPersonArrivedEvent)

  @Operation(
    summary = "A 'person-departed' event",
    operationId = "eventsPersonDepartedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'person-departed' event corresponding to the provided `eventId`",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No 'person-not-arrived' event found for the provided `eventId`",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/events/person-departed/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsPersonDepartedEventIdGet(
    @Parameter(description = "UUID of the event", required = true)
    @PathVariable("eventId") eventId: UUID,
  ): ResponseEntity<Cas1DomainEventEnvelope<PersonDeparted>> = getDomainEvent(eventId, domainEventService::getPersonDepartedEvent)

  @Operation(
    summary = "A 'person-not-arrived' event",
    operationId = "eventsPersonNotArrivedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'person-not-arrived' event corresponding to the provided `eventId`",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No 'person-not-arrived' event found for the provided `eventId`",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/events/person-not-arrived/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsPersonNotArrivedEventIdGet(
    @Parameter(description = "UUID of the event", required = true)
    @PathVariable("eventId") eventId: UUID,
  ): ResponseEntity<Cas1DomainEventEnvelope<PersonNotArrived>> = getDomainEvent(eventId, domainEventService::getPersonNotArrivedEvent)

  @Operation(
    summary = "A 'placement-application-allocated' event",
    operationId = "eventsPlacementApplicationAllocatedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'placement-application-allocated' event corresponding to the provided `eventId`",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No 'placement-application-allocated' event found for the provided `eventId`",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/events/placement-application-allocated/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsPlacementApplicationAllocatedEventIdGet(
    @Parameter(description = "UUID of the event", required = true)
    @PathVariable("eventId") eventId: UUID,
  ): ResponseEntity<Cas1DomainEventEnvelope<PlacementApplicationAllocated>> = getDomainEvent(eventId, domainEventService::getPlacementApplicationAllocatedEvent)

  @Operation(
    summary = "A 'placement-application-withdrawn' event",
    operationId = "eventsPlacementApplicationWithdrawnEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'placement-application-withdrawn' event corresponding to the provided `eventId`",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No 'placement-application-withdrawn' event found for the provided `eventId`",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/events/placement-application-withdrawn/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsPlacementApplicationWithdrawnEventIdGet(
    @Parameter(description = "UUID of the event", required = true)
    @PathVariable("eventId") eventId: UUID,
  ): ResponseEntity<Cas1DomainEventEnvelope<PlacementApplicationWithdrawn>> = getDomainEvent(eventId, domainEventService::getPlacementApplicationWithdrawnEvent)

  @Operation(
    summary = "A 'request-for-placement-assessed' event",
    operationId = "eventsRequestForPlacementAssessedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'request-for-placement-assessed' event corresponding to the provided `eventId`",
      ),
      ApiResponse(
        responseCode = "404",
        description = "No 'request-for-placement-assessed' event found for the provided `eventId`",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/events/request-for-placement-assessed/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsRequestForPlacementAssessedEventIdGet(
    @Parameter(description = "UUID of the event", required = true)
    @PathVariable("eventId") eventId: UUID,
  ): ResponseEntity<Cas1DomainEventEnvelope<RequestForPlacementAssessed>> = getDomainEvent(eventId, domainEventService::getRequestForPlacementAssessedEvent)

  private fun <T : Cas1DomainEventPayload> getDomainEvent(eventId: UUID, serviceMethod: (id: UUID) -> GetCas1DomainEvent<Cas1DomainEventEnvelope<T>>?): ResponseEntity<Cas1DomainEventEnvelope<T>> {
    val event = serviceMethod(eventId) ?: throw NotFoundProblem(eventId, "DomainEvent")
    return ResponseEntity.ok(event.data) as ResponseEntity<Cas1DomainEventEnvelope<T>>
  }
}
