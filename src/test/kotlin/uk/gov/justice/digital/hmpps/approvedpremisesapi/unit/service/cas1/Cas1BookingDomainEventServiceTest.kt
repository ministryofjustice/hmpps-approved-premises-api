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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.BookingNotMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SituationOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OfflineApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffUserDetailsFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MetaDataName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.LocalDate
import java.time.OffsetDateTime

class Cas1BookingDomainEventServiceTest {

  private val domainEventService = mockk<DomainEventService>()
  private val offenderService = mockk<OffenderService>()
  private val communityApiClient = mockk<CommunityApiClient>()

  val service = Cas1BookingDomainEventService(
    domainEventService,
    offenderService,
    communityApiClient,
    UrlTemplate("http://frontend/applications/#id"),
  )

  @Nested
  inner class BookingMade {

    val user = UserEntityFactory()
      .withDefaults()
      .withDeliusUsername("THEDELIUSUSERNAME")
      .produce()

    private val otherUser = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withEventNumber("online app event number")
      .withCreatedByUser(otherUser)
      .withSubmittedAt(OffsetDateTime.now())
      .withReleaseType(ReleaseTypeOption.licence.toString())
      .withSentenceType(SentenceTypeOption.nonStatutory.toString())
      .withSituation(SituationOption.bailSentence.toString())
      .produce()

    val premises = ApprovedPremisesEntityFactory()
      .withDefaults()
      .withName("the premises name")
      .withApCode("the premises ap code")
      .withQCode("the premises qcode")
      .withLocalAuthorityArea(LocalAuthorityAreaEntityFactory().withName("authority name").produce())
      .produce()

    val createdAt = OffsetDateTime.now()

    val booking = BookingEntityFactory()
      .withDefaults()
      .withCrn("THEBOOKINGCRN")
      .withArrivalDate(LocalDate.of(2025, 12, 11))
      .withPremises(premises)
      .withCreatedAt(createdAt)
      .produce()

    val placementRequest = PlacementRequestEntityFactory()
      .withDefaults()
      .produce()

    @BeforeEach
    fun before() {
      every { domainEventService.saveBookingMadeDomainEvent(any()) } just Runs

      val assigneeUserStaffDetails = StaffUserDetailsFactory().produce()
      every { communityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        assigneeUserStaffDetails,
      )

      every {
        offenderService.getOffenderByCrn(
          "THEBOOKINGCRN",
          "THEDELIUSUSERNAME",
          true,
        )
      } returns AuthorisableActionResult.Success(
        OffenderDetailsSummaryFactory()
          .withGender("male")
          .withCrn("THECRN")
          .withNomsNumber("THENOMS")
          .withDateOfBirth(LocalDate.of(1982, 3, 11))
          .produce(),
      )
    }

    @Test
    fun `bookingMade saves domain event`() {
      service.bookingMade(application, booking, user, placementRequest)

      val domainEventArgument = slot<DomainEvent<BookingMadeEnvelope>>()

      verify(exactly = 1) {
        domainEventService.saveBookingMadeDomainEvent(
          capture(domainEventArgument),
        )
      }

      val domainEvent = domainEventArgument.captured

      assertThat(domainEvent.applicationId).isEqualTo(application.id)
      assertThat(domainEvent.crn).isEqualTo("THEBOOKINGCRN")
      assertThat(domainEvent.bookingId).isEqualTo(booking.id)
      assertThat(domainEvent.occurredAt).isEqualTo(createdAt.toInstant())
      assertThat(domainEvent.data.eventType).isEqualTo(EventType.bookingMade)
      assertThat(domainEvent.data.timestamp).isEqualTo(createdAt.toInstant())

      val data = domainEvent.data.eventDetails
      assertThat(data.createdAt).isEqualTo(createdAt.toInstant())
      assertThat(data.applicationId).isEqualTo(application.id)
      assertThat(data.applicationUrl).isEqualTo("http://frontend/applications/${application.id}")
      assertThat(data.bookingId).isEqualTo(booking.id)
      assertThat(data.personReference.crn).isEqualTo("THEBOOKINGCRN")
      assertThat(data.personReference.noms).isEqualTo("THENOMS")
      assertThat(data.deliusEventNumber).isEqualTo("online app event number")
      assertThat(data.premises.id).isEqualTo(premises.id)
      assertThat(data.premises.name).isEqualTo("the premises name")
      assertThat(data.premises.apCode).isEqualTo("the premises ap code")
      assertThat(data.premises.legacyApCode).isEqualTo("the premises qcode")
      assertThat(data.premises.localAuthorityAreaName).isEqualTo("authority name")
      assertThat(data.arrivalOn).isEqualTo(LocalDate.of(2025, 12, 11))
      assertThat(data.applicationSubmittedOn).isEqualTo(application.submittedAt!!.toInstant())
      assertThat(data.releaseType).isEqualTo(application.releaseType)
      assertThat(data.sentenceType).isEqualTo(application.sentenceType)
      assertThat(data.situation).isEqualTo(application.situation)

      assertThat(domainEvent.metadata).isEqualTo(mapOf(MetaDataName.CAS1_PLACEMENT_REQUEST_ID to placementRequest.id.toString()))
    }

    @Test
    fun `adhocBookingMade for regular application saves domain event`() {
      service.adhocBookingMade(
        onlineApplication = application,
        offlineApplication = null,
        eventNumber = "adhoc event number",
        booking = booking,
        user = user,
      )

      val domainEventArgument = slot<DomainEvent<BookingMadeEnvelope>>()

      verify(exactly = 1) {
        domainEventService.saveBookingMadeDomainEvent(
          capture(domainEventArgument),
        )
      }

      val domainEvent = domainEventArgument.captured

      assertThat(domainEvent.applicationId).isEqualTo(application.id)
      assertThat(domainEvent.crn).isEqualTo("THEBOOKINGCRN")
      assertThat(domainEvent.bookingId).isEqualTo(booking.id)
      assertThat(domainEvent.occurredAt).isEqualTo(createdAt.toInstant())
      assertThat(domainEvent.data.eventType).isEqualTo(EventType.bookingMade)
      assertThat(domainEvent.data.timestamp).isEqualTo(createdAt.toInstant())

      val data = domainEvent.data.eventDetails
      assertThat(data.createdAt).isEqualTo(createdAt.toInstant())
      assertThat(data.applicationId).isEqualTo(application.id)
      assertThat(data.applicationUrl).isEqualTo("http://frontend/applications/${application.id}")
      assertThat(data.bookingId).isEqualTo(booking.id)
      assertThat(data.personReference.crn).isEqualTo("THEBOOKINGCRN")
      assertThat(data.personReference.noms).isEqualTo("THENOMS")
      assertThat(data.deliusEventNumber).isEqualTo("online app event number")
      assertThat(data.premises.id).isEqualTo(premises.id)
      assertThat(data.premises.name).isEqualTo("the premises name")
      assertThat(data.premises.apCode).isEqualTo("the premises ap code")
      assertThat(data.premises.legacyApCode).isEqualTo("the premises qcode")
      assertThat(data.premises.localAuthorityAreaName).isEqualTo("authority name")
      assertThat(data.arrivalOn).isEqualTo(LocalDate.of(2025, 12, 11))
      assertThat(data.applicationSubmittedOn).isEqualTo(application.submittedAt!!.toInstant())
      assertThat(data.releaseType).isEqualTo(application.releaseType)
      assertThat(data.sentenceType).isEqualTo(application.sentenceType)
      assertThat(data.situation).isEqualTo(application.situation)

      assertThat(domainEvent.metadata).isEmpty()
    }

    @Test
    fun `adhocBookingMade for offline application saves domain event`() {
      val offlineApplication = OfflineApplicationEntityFactory()
        .withEventNumber("offline app event number")
        .produce()

      service.adhocBookingMade(
        onlineApplication = null,
        offlineApplication = offlineApplication,
        eventNumber = "adhoc event number",
        booking = booking,
        user = user,
      )

      val domainEventArgument = slot<DomainEvent<BookingMadeEnvelope>>()

      verify(exactly = 1) {
        domainEventService.saveBookingMadeDomainEvent(
          capture(domainEventArgument),
        )
      }

      val domainEvent = domainEventArgument.captured

      assertThat(domainEvent.applicationId).isEqualTo(offlineApplication.id)
      assertThat(domainEvent.crn).isEqualTo("THEBOOKINGCRN")
      assertThat(domainEvent.bookingId).isEqualTo(booking.id)
      assertThat(domainEvent.data.eventType).isEqualTo(EventType.bookingMade)

      val data = domainEvent.data.eventDetails
      assertThat(data.applicationId).isEqualTo(offlineApplication.id)
      assertThat(data.applicationUrl).isEqualTo("http://frontend/applications/${offlineApplication.id}")
      assertThat(data.bookingId).isEqualTo(booking.id)
      assertThat(data.personReference.crn).isEqualTo("THEBOOKINGCRN")
      assertThat(data.personReference.noms).isEqualTo("THENOMS")
      assertThat(data.deliusEventNumber).isEqualTo("offline app event number")
      assertThat(data.premises.id).isEqualTo(premises.id)
      assertThat(data.premises.name).isEqualTo("the premises name")
      assertThat(data.premises.apCode).isEqualTo("the premises ap code")
      assertThat(data.premises.legacyApCode).isEqualTo("the premises qcode")
      assertThat(data.premises.localAuthorityAreaName).isEqualTo("authority name")
      assertThat(data.arrivalOn).isEqualTo(LocalDate.of(2025, 12, 11))
      assertThat(data.applicationSubmittedOn).isNull()
      assertThat(data.releaseType).isNull()
      assertThat(data.sentenceType).isNull()
      assertThat(data.situation).isNull()

      assertThat(domainEvent.metadata).isEmpty()
    }

    @Test
    fun `adhocBookingMade for offline app with no event number`() {
      val offlineApplication = OfflineApplicationEntityFactory()
        .withEventNumber(null)
        .produce()

      service.adhocBookingMade(
        onlineApplication = null,
        offlineApplication = offlineApplication,
        eventNumber = "adhoc event number",
        booking = booking,
        user = user,
      )

      val domainEventArgument = slot<DomainEvent<BookingMadeEnvelope>>()

      verify(exactly = 1) {
        domainEventService.saveBookingMadeDomainEvent(
          capture(domainEventArgument),
        )
      }

      val domainEvent = domainEventArgument.captured
      val data = domainEvent.data.eventDetails
      assertThat(data.deliusEventNumber).isEqualTo("adhoc event number")
    }
  }

  @Nested
  inner class BookingNotMade {

    @Test
    fun `bookingNotMade saves domain event`() {
      val user = UserEntityFactory()
        .withDefaults()
        .withDeliusUsername("THEDELIUSUSERNAME")
        .produce()

      val otherUser = UserEntityFactory()
        .withUnitTestControlProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withEventNumber("app event number")
        .withCrn("Application CRN")
        .withCreatedByUser(otherUser)
        .withSubmittedAt(OffsetDateTime.now())
        .withReleaseType(ReleaseTypeOption.licence.toString())
        .withSentenceType(SentenceTypeOption.nonStatutory.toString())
        .withSituation(SituationOption.bailSentence.toString())
        .produce()

      val notMadeAt = OffsetDateTime.now()

      every { domainEventService.saveBookingNotMadeEvent(any()) } just Runs

      val assigneeUserStaffDetails = StaffUserDetailsFactory().withStaffCode("the staff code").produce()
      every { communityApiClient.getStaffUserDetails(user.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        assigneeUserStaffDetails,
      )

      every {
        offenderService.getOffenderByCrn(
          "Application CRN",
          "THEDELIUSUSERNAME",
          false,
        )
      } returns AuthorisableActionResult.Success(
        OffenderDetailsSummaryFactory()
          .withGender("male")
          .withCrn("THECRN")
          .withNomsNumber("THENOMS")
          .withDateOfBirth(LocalDate.of(1982, 3, 11))
          .produce(),
      )

      val placementRequest = PlacementRequestEntityFactory()
        .withDefaults()
        .withApplication(application)
        .produce()

      service.bookingNotMade(
        user = user,
        placementRequest = placementRequest,
        bookingNotCreatedAt = notMadeAt,
        notes = "the notes",
      )

      val domainEventArgument = slot<DomainEvent<BookingNotMadeEnvelope>>()

      verify(exactly = 1) {
        domainEventService.saveBookingNotMadeEvent(
          capture(domainEventArgument),
        )
      }

      val domainEvent = domainEventArgument.captured

      assertThat(domainEvent.applicationId).isEqualTo(application.id)
      assertThat(domainEvent.crn).isEqualTo("Application CRN")
      assertThat(domainEvent.data.eventType).isEqualTo(EventType.bookingNotMade)

      val data = domainEvent.data.eventDetails
      assertThat(data.applicationId).isEqualTo(application.id)
      assertThat(data.applicationUrl).isEqualTo("http://frontend/applications/${application.id}")
      assertThat(data.personReference.crn).isEqualTo("Application CRN")
      assertThat(data.personReference.noms).isEqualTo("THENOMS")
      assertThat(data.deliusEventNumber).isEqualTo("app event number")
      assertThat(data.attemptedAt).isNotNull()
      assertThat(data.attemptedBy.staffMember!!.staffCode).isEqualTo("the staff code")
      assertThat(data.failureDescription).isEqualTo("the notes")

      assertThat(domainEvent.metadata).isEqualTo(mapOf(MetaDataName.CAS1_PLACEMENT_REQUEST_ID to placementRequest.id.toString()))
    }
  }
}
