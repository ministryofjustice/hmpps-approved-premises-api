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
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingCancelledEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingChangedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingNotMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventTransferType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SentenceTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SituationOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OfflineApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MetaDataName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TransferType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1BookingDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.SaveCas1DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.Cas1BookingCancelledEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.Cas1BookingChangedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.Cas1BookingCreatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.springevent.TransferInfo
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.isWithinTheLastMinute
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class Cas1BookingDomainEventServiceTest {

  private val domainEventService = mockk<Cas1DomainEventService>()
  private val offenderService = mockk<OffenderService>()
  private val apDeliusContextApiClient = mockk<ApDeliusContextApiClient>()

  val service = Cas1BookingDomainEventService(
    domainEventService,
    offenderService,
    apDeliusContextApiClient,
    UrlTemplate("http://frontend/applications/#id"),
  )

  @Nested
  inner class SpaceBookingMade {

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

    val placementRequest = PlacementRequestEntityFactory()
      .withDefaults()
      .withApplication(application)
      .produce()

    val spaceBooking = Cas1SpaceBookingEntityFactory()
      .withCrn("THEBOOKINGCRN")
      .withPlacementRequest(placementRequest)
      .withCanonicalArrivalDate(LocalDate.of(2025, 12, 11))
      .withCanonicalDepartureDate(LocalDate.of(2025, 12, 12))
      .withPremises(premises)
      .withCreatedAt(createdAt)
      .withCriteria(CharacteristicEntityFactory().withModelScope("room").withPropertyName("hasEnSuite").produce())
      .produce()

    val changeRequestId = UUID.randomUUID()
    val transferredFromBooking = Cas1SpaceBookingEntityFactory().produce()

    @BeforeEach
    fun before() {
      every { domainEventService.saveBookingMadeDomainEvent(any()) } just Runs

      val assigneeUserStaffDetails = StaffDetailFactory.staffDetail()
      every { apDeliusContextApiClient.getStaffDetail(user.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        assigneeUserStaffDetails,
      )

      setupOffenderServiceMockForNomsNumber("THEBOOKINGCRN", "THENOMS")
    }

    @Test
    fun `bookingMade saves domain event`() {
      service.spaceBookingMade(
        Cas1BookingCreatedEvent(
          booking = spaceBooking,
          createdBy = user,
          transferredFrom = TransferInfo(
            type = TransferType.PLANNED,
            changeRequestId = changeRequestId,
            booking = transferredFromBooking,
          ),
        ),
      )

      val domainEventArgument = slot<SaveCas1DomainEvent<BookingMadeEnvelope>>()

      verify(exactly = 1) {
        domainEventService.saveBookingMadeDomainEvent(
          capture(domainEventArgument),
        )
      }

      val domainEvent = domainEventArgument.captured

      assertThat(domainEvent.applicationId).isEqualTo(application.id)
      assertThat(domainEvent.crn).isEqualTo("THEBOOKINGCRN")
      assertThat(domainEvent.bookingId).isNull()
      assertThat(domainEvent.cas1SpaceBookingId).isEqualTo(spaceBooking.id)
      assertThat(domainEvent.occurredAt).isEqualTo(createdAt.toInstant())
      assertThat(domainEvent.data.eventType).isEqualTo(EventType.bookingMade)
      assertThat(domainEvent.data.timestamp).isEqualTo(createdAt.toInstant())
      assertThat(domainEvent.schemaVersion).isEqualTo(2)

      val data = domainEvent.data.eventDetails
      assertThat(data.createdAt).isEqualTo(createdAt.toInstant())
      assertThat(data.applicationId).isEqualTo(application.id)
      assertThat(data.applicationUrl).isEqualTo("http://frontend/applications/${application.id}")
      assertThat(data.bookingId).isEqualTo(spaceBooking.id)
      assertThat(data.personReference.crn).isEqualTo("THEBOOKINGCRN")
      assertThat(data.personReference.noms).isEqualTo("THENOMS")
      assertThat(data.deliusEventNumber).isEqualTo("online app event number")
      assertThat(data.premises.id).isEqualTo(premises.id)
      assertThat(data.premises.name).isEqualTo("the premises name")
      assertThat(data.premises.apCode).isEqualTo("the premises ap code")
      assertThat(data.premises.legacyApCode).isEqualTo("the premises qcode")
      assertThat(data.premises.localAuthorityAreaName).isEqualTo("authority name")
      assertThat(data.arrivalOn).isEqualTo(LocalDate.of(2025, 12, 11))
      assertThat(data.departureOn).isEqualTo(LocalDate.of(2025, 12, 12))
      assertThat(data.applicationSubmittedOn).isEqualTo(application.submittedAt!!.toInstant())
      assertThat(data.releaseType).isEqualTo(application.releaseType)
      assertThat(data.sentenceType).isEqualTo(application.sentenceType)
      assertThat(data.situation).isEqualTo(application.situation)
      assertThat(data.characteristics).isEqualTo(listOf(SpaceCharacteristic.hasEnSuite))
      assertThat(data.transferredFrom!!.booking.id).isEqualTo(transferredFromBooking.id)
      assertThat(data.transferredFrom!!.changeRequestId).isEqualTo(changeRequestId)
      assertThat(data.transferredFrom!!.type).isEqualTo(EventTransferType.PLANNED)

      assertThat(domainEvent.metadata).isEqualTo(mapOf(MetaDataName.CAS1_PLACEMENT_REQUEST_ID to placementRequest.id.toString()))
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

      val assigneeUserStaffDetails = StaffDetailFactory.staffDetail(code = "the staff code")
      every { apDeliusContextApiClient.getStaffDetail(user.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        assigneeUserStaffDetails,
      )

      setupOffenderServiceMockForNomsNumber("Application CRN", "THENOMS")

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

      val domainEventArgument = slot<SaveCas1DomainEvent<BookingNotMadeEnvelope>>()

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

  @Nested
  inner class SpaceBookingCancelled {

    @Test
    fun `linked to application`() {
      val user = UserEntityFactory()
        .withDefaults()
        .withDeliusUsername("THEDELIUSUSERNAME")
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withSubmittedAt(OffsetDateTime.now())
        .produce()

      val premises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .withName("the premises name")
        .withApCode("the premises ap code")
        .withQCode("the premises qcode")
        .withLocalAuthorityArea(LocalAuthorityAreaEntityFactory().withName("authority name").produce())
        .produce()

      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withPremises(premises)
        .withApplication(application)
        .withCrn(application.crn)
        .withCancellationOccurredAt(LocalDate.parse("2025-11-15"))
        .produce()

      every { domainEventService.saveBookingCancelledEvent(any()) } just Runs

      setupOffenderServiceMockForNomsNumber(spaceBooking.crn, "THENOMS")

      val staffUserDetails = StaffDetailFactory.staffDetail()
      every { apDeliusContextApiClient.getStaffDetail(user.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        staffUserDetails,
      )

      val appealChangeRequestId = UUID.randomUUID()

      service.spaceBookingCancelled(
        Cas1BookingCancelledEvent(
          booking = spaceBooking,
          user = user,
          reason = CancellationReasonEntityFactory()
            .withName("the reason name")
            .produce(),
          appealChangeRequestId = appealChangeRequestId,
        ),
      )

      val domainEventArgument = slot<SaveCas1DomainEvent<BookingCancelledEnvelope>>()
      verify(exactly = 1) {
        domainEventService.saveBookingCancelledEvent(
          capture(domainEventArgument),
        )
      }

      val domainEvent = domainEventArgument.captured

      assertThat(domainEvent.applicationId).isEqualTo(application.id)
      assertThat(domainEvent.bookingId).isNull()
      assertThat(domainEvent.cas1SpaceBookingId).isEqualTo(spaceBooking.id)
      assertThat(domainEvent.crn).isEqualTo(application.crn)
      assertThat(domainEvent.nomsNumber).isEqualTo("THENOMS")
      assertThat(domainEvent.schemaVersion).isEqualTo(2)
      assertThat(domainEvent.occurredAt).isWithinTheLastMinute()

      val data = domainEvent.data.eventDetails
      assertThat(data.applicationId).isEqualTo(application.id)
      assertThat(data.applicationUrl).isEqualTo("http://frontend/applications/${application.id}")
      assertThat(data.bookingId).isEqualTo(spaceBooking.id)
      assertThat(data.personReference.crn).isEqualTo(spaceBooking.crn)
      assertThat(data.personReference.noms).isEqualTo("THENOMS")
      assertThat(data.deliusEventNumber).isEqualTo(application.eventNumber)

      assertThat(data.premises.id).isEqualTo(premises.id)
      assertThat(data.premises.name).isEqualTo(premises.name)
      assertThat(data.premises.apCode).isEqualTo(premises.apCode)
      assertThat(data.premises.legacyApCode).isEqualTo(premises.qCode)
      assertThat(data.premises.localAuthorityAreaName).isEqualTo(premises.localAuthorityArea!!.name)

      assertThat(data.cancelledBy.staffCode).isEqualTo(staffUserDetails.code)
      assertThat(data.cancelledAt).isEqualTo(Instant.parse("2025-11-15T00:00:00.00Z"))
      assertThat(data.cancelledAtDate).isEqualTo(LocalDate.parse("2025-11-15"))
      assertThat(data.cancellationReason).isEqualTo("the reason name")
      assertThat(data.cancellationRecordedAt).isWithinTheLastMinute()

      assertThat(data.appealChangeRequestId).isEqualTo(appealChangeRequestId)
    }

    @Test
    fun `linked to offline application`() {
      val user = UserEntityFactory()
        .withDefaults()
        .withDeliusUsername("THEDELIUSUSERNAME")
        .produce()

      val offlineApplication = OfflineApplicationEntityFactory().produce()

      val premises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .withName("the premises name")
        .withApCode("the premises ap code")
        .withQCode("the premises qcode")
        .withLocalAuthorityArea(LocalAuthorityAreaEntityFactory().withName("authority name").produce())
        .produce()

      val spaceBooking = Cas1SpaceBookingEntityFactory()
        .withPremises(premises)
        .withApplication(null)
        .withOfflineApplication(offlineApplication)
        .withCrn(offlineApplication.crn)
        .withCancellationOccurredAt(LocalDate.parse("2025-11-15"))
        .produce()

      every { domainEventService.saveBookingCancelledEvent(any()) } just Runs

      setupOffenderServiceMockForNomsNumber(spaceBooking.crn, "THENOMS")

      val staffUserDetails = StaffDetailFactory.staffDetail()
      every { apDeliusContextApiClient.getStaffDetail(user.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        staffUserDetails,
      )

      service.spaceBookingCancelled(
        Cas1BookingCancelledEvent(
          booking = spaceBooking,
          user = user,
          reason = CancellationReasonEntityFactory()
            .withName("the reason name")
            .produce(),
        ),
      )

      val domainEventArgument = slot<SaveCas1DomainEvent<BookingCancelledEnvelope>>()
      verify(exactly = 1) {
        domainEventService.saveBookingCancelledEvent(
          capture(domainEventArgument),
        )
      }

      val domainEvent = domainEventArgument.captured

      assertThat(domainEvent.applicationId).isEqualTo(offlineApplication.id)
      assertThat(domainEvent.bookingId).isNull()
      assertThat(domainEvent.cas1SpaceBookingId).isEqualTo(spaceBooking.id)
      assertThat(domainEvent.crn).isEqualTo(offlineApplication.crn)
      assertThat(domainEvent.nomsNumber).isEqualTo("THENOMS")
      assertThat(domainEvent.schemaVersion).isEqualTo(2)
      assertThat(domainEvent.occurredAt).isWithinTheLastMinute()

      val data = domainEvent.data.eventDetails
      assertThat(data.applicationId).isEqualTo(offlineApplication.id)
      assertThat(data.applicationUrl).isEqualTo("http://frontend/applications/${offlineApplication.id}")
      assertThat(data.bookingId).isEqualTo(spaceBooking.id)
      assertThat(data.personReference.crn).isEqualTo(spaceBooking.crn)
      assertThat(data.personReference.noms).isEqualTo("THENOMS")
      assertThat(data.deliusEventNumber).isEqualTo(offlineApplication.eventNumber)

      assertThat(data.premises.id).isEqualTo(premises.id)
      assertThat(data.premises.name).isEqualTo(premises.name)
      assertThat(data.premises.apCode).isEqualTo(premises.apCode)
      assertThat(data.premises.legacyApCode).isEqualTo(premises.qCode)
      assertThat(data.premises.localAuthorityAreaName).isEqualTo(premises.localAuthorityArea!!.name)

      assertThat(data.cancelledBy.staffCode).isEqualTo(staffUserDetails.code)
      assertThat(data.cancelledAt).isEqualTo(Instant.parse("2025-11-15T00:00:00.00Z"))
      assertThat(data.cancelledAtDate).isEqualTo(LocalDate.parse("2025-11-15"))
      assertThat(data.cancellationReason).isEqualTo("the reason name")
      assertThat(data.cancellationRecordedAt).isWithinTheLastMinute()
    }
  }

  @Nested
  inner class SpaceBookingChanged {

    private val changedBy = UserEntityFactory()
      .withDefaults()
      .withDeliusUsername("thedeliususername")
      .produce()

    private val premises = ApprovedPremisesEntityFactory()
      .withDefaults()
      .withName("the premises name")
      .withApCode("the premises ap code")
      .withQCode("the premises qcode")
      .withLocalAuthorityArea(LocalAuthorityAreaEntityFactory().withName("authority name").produce())
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(changedBy)
      .withSubmittedAt(OffsetDateTime.now())
      .withEventNumber("online app event number")
      .produce()

    val staffUserDetails = StaffDetailFactory.staffDetail()

    val domainEventArgument = slot<SaveCas1DomainEvent<BookingChangedEnvelope>>()

    val createdAt = OffsetDateTime.now()

    val changeRequestId = UUID.randomUUID()
    val transferredToBooking = Cas1SpaceBookingEntityFactory().produce()

    @Test
    fun `should successfully emit domain event for expected arrival date change`() {
      val booking = Cas1SpaceBookingEntityFactory()
        .withPremises(premises)
        .withApplication(application)
        .withOfflineApplication(null)
        .withCrn("THEBOOKINGCRN")
        .withExpectedArrivalDate(LocalDate.of(2025, 3, 12))
        .withExpectedDepartureDate(LocalDate.of(2025, 4, 11))
        .produce()

      every { domainEventService.saveBookingChangedEvent(any()) } just Runs

      setupOffenderServiceMockForNomsNumber("THEBOOKINGCRN", "THENOMS")

      every { apDeliusContextApiClient.getStaffDetail(changedBy.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        staffUserDetails,
      )

      service.spaceBookingChanged(
        Cas1BookingChangedEvent(
          booking,
          changedBy,
          bookingChangedAt = createdAt,
          previousArrivalDateIfChanged = LocalDate.of(2025, 2, 12),
          previousDepartureDateIfChanged = null,
          previousCharacteristicsIfChanged = null,
          transferredTo = TransferInfo(
            type = TransferType.EMERGENCY,
            changeRequestId = changeRequestId,
            booking = transferredToBooking,
          ),
        ),
      )

      verify(exactly = 1) {
        domainEventService.saveBookingChangedEvent(
          capture(domainEventArgument),
        )
      }

      val domainEvent = domainEventArgument.captured

      assertThat(domainEvent.applicationId).isEqualTo(application.id)
      assertThat(domainEvent.crn).isEqualTo("THEBOOKINGCRN")
      assertThat(domainEvent.bookingId).isNull()
      assertThat(domainEvent.cas1SpaceBookingId).isEqualTo(booking.id)
      assertThat(domainEvent.occurredAt).isEqualTo(createdAt.toInstant())
      assertThat(domainEvent.data.eventType).isEqualTo(EventType.bookingChanged)
      assertThat(domainEvent.data.timestamp).isEqualTo(createdAt.toInstant())
      assertThat(domainEvent.schemaVersion).isEqualTo(2)

      val data = domainEvent.data.eventDetails
      assertThat(data.applicationId).isEqualTo(application.id)
      assertThat(data.applicationUrl).isEqualTo("http://frontend/applications/${application.id}")
      assertThat(data.bookingId).isEqualTo(domainEvent.cas1SpaceBookingId)
      assertThat(data.personReference.crn).isEqualTo("THEBOOKINGCRN")
      assertThat(data.personReference.noms).isEqualTo("THENOMS")
      assertThat(data.deliusEventNumber).isEqualTo("online app event number")
      assertThat(data.changedAt).isEqualTo(createdAt.toInstant())

      assertThat(data.changedBy.staffCode).isEqualTo(staffUserDetails.code)
      assertThat(data.changedBy.username).isEqualTo(staffUserDetails.username)
      assertThat(data.changedBy.forenames).isEqualTo(staffUserDetails.name.forenames())
      assertThat(data.changedBy.surname).isEqualTo(staffUserDetails.name.surname)

      assertThat(data.premises.id).isEqualTo(premises.id)
      assertThat(data.premises.name).isEqualTo("the premises name")
      assertThat(data.premises.apCode).isEqualTo("the premises ap code")
      assertThat(data.premises.legacyApCode).isEqualTo("the premises qcode")
      assertThat(data.premises.localAuthorityAreaName).isEqualTo("authority name")

      assertThat(data.arrivalOn).isEqualTo(LocalDate.of(2025, 3, 12))
      assertThat(data.departureOn).isEqualTo(LocalDate.of(2025, 4, 11))
      assertThat(data.previousArrivalOn).isEqualTo(LocalDate.of(2025, 2, 12))
      assertThat(data.previousDepartureOn).isNull()
      assertThat(data.previousCharacteristics).isNull()

      assertThat(data.transferredTo!!.booking.id).isEqualTo(transferredToBooking.id)
      assertThat(data.transferredTo!!.changeRequestId).isEqualTo(changeRequestId)
      assertThat(data.transferredTo!!.type).isEqualTo(EventTransferType.EMERGENCY)
    }

    @Test
    fun `should successfully emit domain event for expected arrival departure date changes and characteristic`() {
      val roomCharacteristic = CharacteristicEntityFactory().withModelScope("room").withPropertyName("isArsonSuitable").produce()
      val previousRoomCharacteristic = CharacteristicEntityFactory().withModelScope("room").withPropertyName("hasEnSuite").produce()

      val booking = Cas1SpaceBookingEntityFactory()
        .withPremises(premises)
        .withApplication(application)
        .withOfflineApplication(null)
        .withCrn("THEBOOKINGCRN")
        .withCriteria(mutableListOf(roomCharacteristic))
        .withExpectedArrivalDate(LocalDate.of(2025, 3, 12))
        .withExpectedDepartureDate(LocalDate.of(2025, 5, 11))
        .produce()

      every { domainEventService.saveBookingChangedEvent(any()) } just Runs

      setupOffenderServiceMockForNomsNumber("THEBOOKINGCRN", "THENOMS")

      every { apDeliusContextApiClient.getStaffDetail(changedBy.deliusUsername) } returns ClientResult.Success(
        HttpStatus.OK,
        staffUserDetails,
      )

      service.spaceBookingChanged(
        Cas1BookingChangedEvent(
          booking,
          changedBy,
          bookingChangedAt = createdAt,
          previousArrivalDateIfChanged = LocalDate.of(2025, 2, 12),
          previousDepartureDateIfChanged = LocalDate.of(2025, 4, 11),
          previousCharacteristicsIfChanged = listOf(previousRoomCharacteristic),
        ),
      )

      verify(exactly = 1) {
        domainEventService.saveBookingChangedEvent(
          capture(domainEventArgument),
        )
      }

      val domainEvent = domainEventArgument.captured

      assertThat(domainEvent.applicationId).isEqualTo(application.id)
      assertThat(domainEvent.crn).isEqualTo("THEBOOKINGCRN")
      assertThat(domainEvent.bookingId).isNull()
      assertThat(domainEvent.cas1SpaceBookingId).isEqualTo(booking.id)
      assertThat(domainEvent.occurredAt).isEqualTo(createdAt.toInstant())
      assertThat(domainEvent.data.eventType).isEqualTo(EventType.bookingChanged)
      assertThat(domainEvent.data.timestamp).isEqualTo(createdAt.toInstant())
      assertThat(domainEvent.schemaVersion).isEqualTo(2)

      val data = domainEvent.data.eventDetails
      assertThat(data.applicationId).isEqualTo(application.id)
      assertThat(data.applicationUrl).isEqualTo("http://frontend/applications/${application.id}")
      assertThat(data.bookingId).isEqualTo(domainEvent.cas1SpaceBookingId)
      assertThat(data.personReference.crn).isEqualTo("THEBOOKINGCRN")
      assertThat(data.personReference.noms).isEqualTo("THENOMS")
      assertThat(data.deliusEventNumber).isEqualTo("online app event number")
      assertThat(data.changedAt).isEqualTo(createdAt.toInstant())

      assertThat(data.changedBy.staffCode).isEqualTo(staffUserDetails.code)
      assertThat(data.changedBy.username).isEqualTo(staffUserDetails.username)
      assertThat(data.changedBy.forenames).isEqualTo(staffUserDetails.name.forenames())
      assertThat(data.changedBy.surname).isEqualTo(staffUserDetails.name.surname)

      assertThat(data.premises.id).isEqualTo(premises.id)
      assertThat(data.premises.name).isEqualTo("the premises name")
      assertThat(data.premises.apCode).isEqualTo("the premises ap code")
      assertThat(data.premises.legacyApCode).isEqualTo("the premises qcode")
      assertThat(data.premises.localAuthorityAreaName).isEqualTo("authority name")

      assertThat(data.arrivalOn).isEqualTo(LocalDate.of(2025, 3, 12))
      assertThat(data.departureOn).isEqualTo(LocalDate.of(2025, 5, 11))
      assertThat(data.previousArrivalOn).isEqualTo(LocalDate.of(2025, 2, 12))
      assertThat(data.previousDepartureOn).isEqualTo(LocalDate.of(2025, 4, 11))
      assertThat(data.characteristics).isEqualTo(listOf(SpaceCharacteristic.isArsonSuitable))
      assertThat(data.previousCharacteristics).isEqualTo(listOf(SpaceCharacteristic.hasEnSuite))
    }
  }

  private fun setupOffenderServiceMockForNomsNumber(crn: String, nomsNumber: String) {
    every {
      offenderService.getPersonSummaryInfoResult(crn, LaoStrategy.NeverRestricted)
    } returns PersonSummaryInfoResult.Success.Full(
      crn = crn,
      summary = CaseSummaryFactory().withNomsId(nomsNumber).produce(),
    )
  }
}
