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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AssessmentAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AssessmentAppealedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingCancelledEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingChangedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingNotMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.FurtherInformationRequestedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.MatchRequestWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementApplicationAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Problem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
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
        content = [Content(schema = Schema(implementation = ApplicationAssessedEnvelope::class))],
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
    eventId: java.util.UUID,
  ): ResponseEntity<ApplicationAssessedEnvelope> = getDomainEvent<ApplicationAssessedEnvelope>(eventId)

  @Operation(
    summary = "An application-submitted event",
    operationId = "eventsApplicationSubmittedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The application-submitted corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = ApplicationSubmittedEnvelope::class))],
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
    eventId: java.util.UUID,
  ): ResponseEntity<ApplicationSubmittedEnvelope> = getDomainEvent<ApplicationSubmittedEnvelope>(eventId)

  @Operation(
    summary = "An application-withdrawn event",
    operationId = "eventsApplicationWithdrawnEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The application-withdrawn event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = ApplicationWithdrawnEnvelope::class))],
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
    @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<ApplicationWithdrawnEnvelope> = getDomainEvent<ApplicationWithdrawnEnvelope>(eventId)

  @Operation(
    summary = "An assessment-allocated event",
    operationId = "eventsAssessmentAllocatedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The assessment-allocated event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = AssessmentAllocatedEnvelope::class))],
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
    @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<AssessmentAllocatedEnvelope> = getDomainEvent<AssessmentAllocatedEnvelope>(eventId)

  @Operation(
    summary = "An 'assessment-appealed' event",
    operationId = "eventsAssessmentAppealedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'assessment-appealed' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = AssessmentAppealedEnvelope::class))],
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
    @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<AssessmentAppealedEnvelope> = getDomainEvent<AssessmentAppealedEnvelope>(eventId)

  @Operation(
    summary = "A 'booking-cancelled' event",
    operationId = "eventsBookingCancelledEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'booking-cancelled' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = BookingCancelledEnvelope::class))],
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
    @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<BookingCancelledEnvelope> = getDomainEvent<BookingCancelledEnvelope>(eventId)

  @Operation(
    summary = "A 'booking-changed' event",
    operationId = "eventsBookingChangedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'booking-changed' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = BookingChangedEnvelope::class))],
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
    @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<BookingChangedEnvelope> = getDomainEvent<BookingChangedEnvelope>(eventId)

  @Operation(
    summary = "A 'booking-made' event",
    operationId = "eventsBookingMadeEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'booking-made' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = BookingMadeEnvelope::class))],
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
    @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<BookingMadeEnvelope> = getDomainEvent<BookingMadeEnvelope>(eventId)

  @Operation(
    summary = "A 'booking-not-made' event",
    operationId = "eventsBookingNotMadeEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'booking-not-made' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = BookingNotMadeEnvelope::class))],
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
    @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<BookingNotMadeEnvelope> = getDomainEvent<BookingNotMadeEnvelope>(eventId)

  @Operation(
    summary = "A 'further-information-requested' event",
    operationId = "eventsFurtherInformationRequestedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'further-information-requested' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = FurtherInformationRequestedEnvelope::class))],
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
    @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<FurtherInformationRequestedEnvelope> = getDomainEvent<FurtherInformationRequestedEnvelope>(eventId)

  @Operation(
    summary = "A 'match-request-withdrawn' event",
    operationId = "eventsMatchRequestWithdrawnEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'match-request-withdrawn' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = MatchRequestWithdrawnEnvelope::class))],
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
    @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<MatchRequestWithdrawnEnvelope> = getDomainEvent<MatchRequestWithdrawnEnvelope>(eventId)

  @Operation(
    summary = "A 'person-arrived' event",
    operationId = "eventsPersonArrivedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'person-arrived' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = PersonArrivedEnvelope::class))],
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
    @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<PersonArrivedEnvelope> = getDomainEvent<PersonArrivedEnvelope>(eventId)

  @Operation(
    summary = "A 'person-departed' event",
    operationId = "eventsPersonDepartedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'person-departed' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = PersonDepartedEnvelope::class))],
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
    @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<PersonDepartedEnvelope> = getDomainEvent<PersonDepartedEnvelope>(eventId)

  @Operation(
    summary = "A 'person-not-arrived' event",
    operationId = "eventsPersonNotArrivedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'person-not-arrived' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = PersonNotArrivedEnvelope::class))],
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
    @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<PersonNotArrivedEnvelope> = getDomainEvent<PersonNotArrivedEnvelope>(eventId)

  @Operation(
    summary = "A 'placement-application-allocated' event",
    operationId = "eventsPlacementApplicationAllocatedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'placement-application-allocated' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = PlacementApplicationAllocatedEnvelope::class))],
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
    @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<PlacementApplicationAllocatedEnvelope> = getDomainEvent<PlacementApplicationAllocatedEnvelope>(eventId)

  @Operation(
    summary = "A 'placement-application-withdrawn' event",
    operationId = "eventsPlacementApplicationWithdrawnEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'placement-application-withdrawn' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = PlacementApplicationWithdrawnEnvelope::class))],
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
    @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<PlacementApplicationWithdrawnEnvelope> = getDomainEvent<PlacementApplicationWithdrawnEnvelope>(eventId)

  @Operation(
    summary = "A 'request-for-placement-assessed' event",
    operationId = "eventsRequestForPlacementAssessedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'request-for-placement-assessed' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = RequestForPlacementAssessedEnvelope::class))],
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
    @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<RequestForPlacementAssessedEnvelope> = getDomainEvent<RequestForPlacementAssessedEnvelope>(eventId)

  @Suppress("UNCHECKED_CAST") // Safe as the return type is constant and not likely to change at runtime
  private inline fun <reified T> getDomainEvent(eventId: UUID): ResponseEntity<T> {
    val serviceMethod = when (T::class) {
      AssessmentAllocatedEnvelope::class -> domainEventService::getAssessmentAllocatedEvent
      AssessmentAppealedEnvelope::class -> domainEventService::getAssessmentAppealedEvent
      MatchRequestWithdrawnEnvelope::class -> domainEventService::getMatchRequestWithdrawnEvent
      ApplicationWithdrawnEnvelope::class -> domainEventService::getApplicationWithdrawnEvent
      ApplicationAssessedEnvelope::class -> domainEventService::getApplicationAssessedDomainEvent
      BookingMadeEnvelope::class -> domainEventService::getBookingMadeEvent
      ApplicationSubmittedEnvelope::class -> domainEventService::getApplicationSubmittedDomainEvent
      PlacementApplicationWithdrawnEnvelope::class -> domainEventService::getPlacementApplicationWithdrawnEvent
      BookingCancelledEnvelope::class -> domainEventService::getBookingCancelledEvent
      BookingChangedEnvelope::class -> domainEventService::getBookingChangedEvent
      BookingNotMadeEnvelope::class -> domainEventService::getBookingNotMadeEvent
      PersonArrivedEnvelope::class -> domainEventService::getPersonArrivedEvent
      PersonDepartedEnvelope::class -> domainEventService::getPersonDepartedEvent
      PlacementApplicationAllocatedEnvelope::class -> domainEventService::getPlacementApplicationAllocatedEvent
      PersonNotArrivedEnvelope::class -> domainEventService::getPersonNotArrivedEvent
      FurtherInformationRequestedEnvelope::class -> domainEventService::getFurtherInformationRequestMadeEvent
      RequestForPlacementAssessedEnvelope::class -> domainEventService::getRequestForPlacementAssessedEvent
      else -> throw RuntimeException("Only emittable CAS1 events are supported")
    }

    val event = serviceMethod(eventId) ?: throw NotFoundProblem(eventId, "DomainEvent")

    return ResponseEntity.ok(event.data) as ResponseEntity<T>
  }
}
