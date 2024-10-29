package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffWithoutUsernameUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1SpaceBookingManagementDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ApplicationTimelineTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWithinTheLastMinute
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class Cas1SpaceBookingManagementDomainEventServiceTest {

  private val domainEventService = mockk<DomainEventService>()
  private val offenderService = mockk<OffenderService>()
  private val communityApiClient = mockk<CommunityApiClient>()
  private val applicationTimelineTransformer = mockk<ApplicationTimelineTransformer>()

  val service = Cas1SpaceBookingManagementDomainEventService(
    domainEventService,
    offenderService,
    communityApiClient,
    UrlTemplate("http://frontend/applications/#id"),
    applicationTimelineTransformer,
  )

  @Nested
  inner class ArrivalRecorded {

    private val arrivalDate = LocalDateTime.now().toInstant(ZoneOffset.UTC)
    private val departureDate = LocalDate.now().plusMonths(3)

    private val application = ApprovedPremisesApplicationEntityFactory()
      .withSubmittedAt(OffsetDateTime.parse("2024-10-01T12:00:00Z"))
      .withCreatedByUser(
        UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )
      .produce()

    private val caseSummary = CaseSummaryFactory()
      .produce()

    private val premises = ApprovedPremisesEntityFactory()
      .withDefaults()
      .produce()

    val keyWorker = StaffWithoutUsernameUserDetailsFactory()
      .produce()

    private val existingSpaceBooking = Cas1SpaceBookingEntityFactory()
      .withApplication(application)
      .withPremises(premises)
      .withActualArrivalDateTime(arrivalDate)
      .withCanonicalArrivalDate(arrivalDate.toLocalDate())
      .withExpectedDepartureDate(departureDate)
      .withCanonicalDepartureDate(departureDate)
      .withKeyworkerStaffCode(keyWorker.staffCode)
      .produce()

    @BeforeEach
    fun before() {
      every { domainEventService.savePersonArrivedEvent(any(), any()) } just Runs

      every { offenderService.getPersonSummaryInfoResults(any(), any()) } returns
        listOf(PersonSummaryInfoResult.Success.Full("THEBOOKINGCRN", caseSummary))

      every { communityApiClient.getStaffUserDetailsForStaffCode(any()) } returns ClientResult.Success(
        HttpStatus.OK,
        keyWorker,
      )
    }

    @Test
    fun `record arrival and emits domain event`() {
      service.arrivalRecorded(existingSpaceBooking)

      val domainEventArgument = slot<DomainEvent<PersonArrivedEnvelope>>()

      verify(exactly = 1) {
        domainEventService.savePersonArrivedEvent(
          capture(domainEventArgument),
          emit = false,
        )
      }

      val domainEvent = domainEventArgument.captured

      assertThat(domainEvent.data.eventType).isEqualTo(EventType.personArrived)
      assertThat(domainEvent.data.timestamp).isWithinTheLastMinute()
      assertThat(domainEvent.applicationId).isEqualTo(application.id)
      assertThat(domainEvent.bookingId).isNull()
      assertThat(domainEvent.cas1SpaceBookingId).isEqualTo(existingSpaceBooking.id)
      assertThat(domainEvent.crn).isEqualTo(existingSpaceBooking.crn)
      assertThat(domainEvent.nomsNumber).isEqualTo(caseSummary.nomsId)
      assertThat(domainEvent.occurredAt).isEqualTo(existingSpaceBooking.actualArrivalDateTime)
      val data = domainEvent.data.eventDetails
      assertThat(data.previousExpectedDepartureOn).isNull()
      assertThat(data.applicationId).isEqualTo(application.id)
      assertThat(data.applicationSubmittedOn).isEqualTo(application.submittedAt!!.toLocalDate())
      assertThat(data.applicationUrl).isEqualTo("http://frontend/applications/${application.id}")
      assertThat(data.arrivedAt).isEqualTo(arrivalDate)
      assertThat(data.deliusEventNumber).isEqualTo(application.eventNumber)
      assertThat(data.premises.id).isEqualTo(premises.id)
      assertThat(data.premises.name).isEqualTo(premises.name)
      assertThat(data.premises.apCode).isEqualTo(premises.apCode)
      assertThat(data.premises.legacyApCode).isEqualTo(premises.qCode)
      assertThat(data.premises.localAuthorityAreaName).isEqualTo(premises.localAuthorityArea!!.name)
      assertThat(data.keyWorker!!.staffCode).isEqualTo(keyWorker.staffCode)
      assertThat(data.keyWorker!!.surname).isEqualTo(keyWorker.staff.surname)
      assertThat(data.keyWorker!!.forenames).isEqualTo(keyWorker.staff.forenames)
      assertThat(data.keyWorker!!.staffIdentifier).isEqualTo(keyWorker.staffIdentifier)
    }

    @Test
    fun `record arrival and emits domain event with no keyWorker information if keyWorker is not present in original booking`() {
      val existingSpaceBooking = Cas1SpaceBookingEntityFactory()
        .withApplication(application)
        .withPremises(premises)
        .withActualArrivalDateTime(arrivalDate)
        .withCanonicalArrivalDate(arrivalDate.toLocalDate())
        .withExpectedDepartureDate(departureDate)
        .withCanonicalDepartureDate(departureDate)
        .withKeyworkerStaffCode(null)
        .produce()

      service.arrivalRecorded(existingSpaceBooking)

      val domainEventArgument = slot<DomainEvent<PersonArrivedEnvelope>>()

      verify(exactly = 1) {
        domainEventService.savePersonArrivedEvent(
          capture(domainEventArgument),
          emit = false,
        )
      }

      val domainEvent = domainEventArgument.captured

      val data = domainEvent.data.eventDetails
      assertThat(data.keyWorker).isNull()
    }

    @Test
    fun `record arrival and emits domain event recognising change in expected departure date`() {
      val previousExpectedDepartureDate = departureDate.plusMonths(1)
      service.arrivalRecorded(existingSpaceBooking, previousExpectedDepartureDate)

      val domainEventArgument = slot<DomainEvent<PersonArrivedEnvelope>>()

      verify(exactly = 1) {
        domainEventService.savePersonArrivedEvent(
          capture(domainEventArgument),
          emit = false,
        )
      }

      val domainEvent = domainEventArgument.captured

      val data = domainEvent.data.eventDetails
      assertThat(data.previousExpectedDepartureOn).isEqualTo(previousExpectedDepartureDate)
      assertThat(data.expectedDepartureOn).isEqualTo(departureDate)
    }
  }

  @Nested
  inner class DepartureRecorded {

    private val arrivedDate = LocalDateTime.now().toInstant(ZoneOffset.UTC)
    private val departedDate = LocalDate.now().plusMonths(3)

    private val application = ApprovedPremisesApplicationEntityFactory()
      .withSubmittedAt(OffsetDateTime.parse("2024-10-01T12:00:00Z"))
      .withCreatedByUser(
        UserEntityFactory()
          .withUnitTestControlProbationRegion()
          .produce(),
      )
      .produce()

    private val caseSummary = CaseSummaryFactory()
      .produce()

    private val premises = ApprovedPremisesEntityFactory()
      .withDefaults()
      .produce()

    val keyWorker = StaffWithoutUsernameUserDetailsFactory()
      .produce()

    private val departedSpaceBooking = Cas1SpaceBookingEntityFactory()
      .withApplication(application)
      .withPremises(premises)
      .withActualArrivalDateTime(arrivedDate)
      .withCanonicalArrivalDate(arrivedDate.toLocalDate())
      .withExpectedDepartureDate(departedDate)
      .withCanonicalDepartureDate(departedDate)
      .withActualDepartureDateTime(departedDate.toLocalDateTime(ZoneOffset.UTC).toInstant())
      .withKeyworkerStaffCode(keyWorker.staffCode)
      .produce()

    private val departureReason = DepartureReasonEntity(
      id = UUID.randomUUID(),
      name = "departureReason",
      isActive = true,
      serviceScope = "approved-premises",
      legacyDeliusReasonCode = "legacyDeliusReasonCode",
    )
    private val moveOnCategory = MoveOnCategoryEntity(
      id = UUID.randomUUID(),
      name = "moveOnCategory",
      isActive = true,
      serviceScope = "approved-premises",
      legacyDeliusCategoryCode = "legacyDeliusReasonCode",
    )

    @BeforeEach
    fun before() {
      every { domainEventService.savePersonDepartedEvent(any(), any()) } just Runs

      every { offenderService.getPersonSummaryInfoResults(any(), any()) } returns
        listOf(PersonSummaryInfoResult.Success.Full("THEBOOKINGCRN", caseSummary))

      every { communityApiClient.getStaffUserDetailsForStaffCode(any()) } returns ClientResult.Success(
        HttpStatus.OK,
        keyWorker,
      )
    }

    @Test
    fun `record departure and emits domain event`() {
      service.departureRecorded(departedSpaceBooking, departureReason, moveOnCategory)

      val domainEventArgument = slot<DomainEvent<PersonDepartedEnvelope>>()

      verify(exactly = 1) {
        domainEventService.savePersonDepartedEvent(
          capture(domainEventArgument),
          emit = false,
        )
      }

      val domainEvent = domainEventArgument.captured

      assertThat(domainEvent.data.eventType).isEqualTo(EventType.personDeparted)
      assertThat(domainEvent.data.timestamp).isWithinTheLastMinute()
      assertThat(domainEvent.applicationId).isEqualTo(application.id)
      assertThat(domainEvent.bookingId).isNull()
      assertThat(domainEvent.cas1SpaceBookingId).isEqualTo(departedSpaceBooking.id)
      assertThat(domainEvent.crn).isEqualTo(departedSpaceBooking.crn)
      assertThat(domainEvent.nomsNumber).isEqualTo(caseSummary.nomsId)
      assertThat(domainEvent.occurredAt).isEqualTo(departedSpaceBooking.actualDepartureDateTime)
      val domainEventEventDetails = domainEvent.data.eventDetails
      assertThat(domainEventEventDetails.applicationId).isEqualTo(application.id)
      assertThat(domainEventEventDetails.applicationUrl).isEqualTo("http://frontend/applications/${application.id}")
      assertThat(domainEventEventDetails.bookingId).isEqualTo(departedSpaceBooking.id)
      assertThat(domainEventEventDetails.personReference.crn).isEqualTo(departedSpaceBooking.crn)
      assertThat(domainEventEventDetails.personReference.noms).isEqualTo(caseSummary.nomsId)
      assertThat(domainEventEventDetails.deliusEventNumber).isEqualTo(application.eventNumber)
      assertThat(domainEventEventDetails.departedAt).isEqualTo(departedDate.toLocalDateTime(ZoneOffset.UTC).toInstant())
      assertThat(domainEventEventDetails.reason).isEqualTo(departureReason.name)
      assertThat(domainEventEventDetails.legacyReasonCode).isEqualTo(departureReason.legacyDeliusReasonCode)
      val domainEventPremises = domainEventEventDetails.premises
      assertThat(domainEventPremises.id).isEqualTo(premises.id)
      assertThat(domainEventPremises.name).isEqualTo(premises.name)
      assertThat(domainEventPremises.apCode).isEqualTo(premises.apCode)
      assertThat(domainEventPremises.legacyApCode).isEqualTo(premises.qCode)
      assertThat(domainEventPremises.localAuthorityAreaName).isEqualTo(premises.localAuthorityArea!!.name)
      val domainEventKeyWorker = domainEventEventDetails.keyWorker!!
      assertThat(domainEventKeyWorker.staffCode).isEqualTo(keyWorker.staffCode)
      assertThat(domainEventKeyWorker.surname).isEqualTo(keyWorker.staff.surname)
      assertThat(domainEventKeyWorker.forenames).isEqualTo(keyWorker.staff.forenames)
      assertThat(domainEventKeyWorker.staffIdentifier).isEqualTo(keyWorker.staffIdentifier)
      val domainEventMoveOnCategory = domainEventEventDetails.destination.moveOnCategory
      assertThat(domainEventMoveOnCategory.id).isEqualTo(moveOnCategory.id)
      assertThat(domainEventMoveOnCategory.description).isEqualTo(moveOnCategory.name)
      assertThat(domainEventMoveOnCategory.legacyMoveOnCategoryCode).isEqualTo(moveOnCategory.legacyDeliusCategoryCode)
    }
  }
}
