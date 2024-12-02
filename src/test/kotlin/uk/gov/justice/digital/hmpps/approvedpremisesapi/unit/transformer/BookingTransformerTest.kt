package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CancellationReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Confirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Departure
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DepartureReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DestinationProvider
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MoveOnCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NonArrivalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Nonarrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Turnaround
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawableType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.convert.EnumConverterFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DestinationProviderEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TurnaroundEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ArrivalTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BedTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CancellationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ConfirmationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DepartureTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ExtensionTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NonArrivalTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.TurnaroundTransformer
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class BookingTransformerTest {
  private val mockPersonTransformer = mockk<PersonTransformer>()
  private val mockArrivalTransformer = mockk<ArrivalTransformer>()
  private val mockNonArrivalTransformer = mockk<NonArrivalTransformer>()
  private val mockCancellationTransformer = mockk<CancellationTransformer>()
  private val mockConfirmationTransformer = mockk<ConfirmationTransformer>()
  private val mockDepartureTransformer = mockk<DepartureTransformer>()
  private val mockExtensionTransformer = mockk<ExtensionTransformer>()
  private val mockBedTransformer = mockk<BedTransformer>()
  private val mockTurnaroundTransformer = mockk<TurnaroundTransformer>()
  private val enumConverterFactory = EnumConverterFactory()
  private val mockWorkingDayService = mockk<WorkingDayService>()

  private val bookingTransformer = BookingTransformer(
    mockPersonTransformer,
    mockArrivalTransformer,
    mockDepartureTransformer,
    mockNonArrivalTransformer,
    mockCancellationTransformer,
    mockConfirmationTransformer,
    mockExtensionTransformer,
    mockBedTransformer,
    mockTurnaroundTransformer,
    enumConverterFactory,
    mockWorkingDayService,
  )

  private val premisesEntity = TemporaryAccommodationPremisesEntity(
    id = UUID.fromString("9703eaaf-164f-4f35-b038-f4de79e4847b"),
    name = "AP",
    probationRegion = ProbationRegionEntity(
      id = UUID.fromString("4eae0059-af28-4436-a4d8-7106523866d9"),
      name = "region",
      deliusCode = "ABC",
      apArea = null,
    ),
    localAuthorityArea = LocalAuthorityAreaEntity(
      id = UUID.fromString("ee39d3bc-e9ad-4408-a21d-cf763aa1d981"),
      identifier = "AUTHORITY",
      name = "Local Authority Area",
      premises = mutableListOf(),
    ),
    bookings = mutableListOf(),
    lostBeds = mutableListOf(),
    addressLine1 = "1 somewhere",
    addressLine2 = "Some district",
    town = "Somewhere",
    postcode = "ST8ST8",
    latitude = null,
    longitude = null,
    notes = "",
    emailAddress = "some@email",
    rooms = mutableListOf(),
    characteristics = mutableListOf(),
    status = PropertyStatus.active,
    probationDeliveryUnit = null,
    turnaroundWorkingDayCount = 2,
  )

  private val baseBookingEntity = BookingEntity(
    id = UUID.fromString("c0cffa2a-490a-4e8b-a970-80aea3922a18"),
    arrivalDate = LocalDate.parse("2022-08-10"),
    departureDate = LocalDate.parse("2022-08-30"),
    keyWorkerStaffCode = "789",
    crn = "CRN123",
    arrivals = mutableListOf(),
    departures = mutableListOf(),
    nonArrival = null,
    cancellations = mutableListOf(),
    confirmation = null,
    extensions = mutableListOf(),
    dateChanges = mutableListOf(),
    premises = premisesEntity,
    bed = null,
    service = ServiceName.approvedPremises.value,
    originalArrivalDate = LocalDate.parse("2022-08-10"),
    originalDepartureDate = LocalDate.parse("2022-08-30"),
    createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
    application = null,
    offlineApplication = null,
    turnarounds = mutableListOf(),
    nomsNumber = "NOMS123",
    placementRequest = null,
    status = null,
  )

  private val offenderDetails = OffenderDetailsSummaryFactory()
    .withCrn("crn")
    .withFirstName("first")
    .withLastName("last")
    .withNomsNumber("NOMS321")
    .produce()

  private val inmateDetail = InmateDetailFactory()
    .withOffenderNo("NOMS321")
    .produce()

  init {
    every { mockArrivalTransformer.transformJpaToApi(null) } returns null
    every { mockNonArrivalTransformer.transformJpaToApi(null) } returns null
    every { mockCancellationTransformer.transformJpaToApi(null) } returns null
    every { mockConfirmationTransformer.transformJpaToApi(null) } returns null
    every { mockDepartureTransformer.transformJpaToApi(null) } returns null

    every { mockPersonTransformer.transformModelToPersonApi(PersonInfoResult.Success.Full("crn", offenderDetails, inmateDetail)) } returns FullPerson(
      type = PersonType.fullPerson,
      crn = "crn",
      name = "first last",
      dateOfBirth = LocalDate.parse("2022-09-08"),
      sex = "Male",
      status = PersonStatus.inCommunity,
      nomsNumber = "NOMS321",
      nationality = "English",
      religionOrBelief = null,
      genderIdentity = null,
      prisonName = null,
    )
  }

  @AfterEach
  fun unmockStatics() {
    unmockkAll()
  }

  @Test
  fun `Approved Premises Awaiting Arrival entity is correctly transformed`() {
    val awaitingArrivalBooking = baseBookingEntity.copy(id = UUID.fromString("5bbe785f-5ff3-46b9-b9fe-d9e6ca7a18e8"))

    val transformedBooking = bookingTransformer.transformJpaToApi(
      awaitingArrivalBooking,
      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetail),
    )

    assertThat(transformedBooking).isEqualTo(
      Booking(
        id = UUID.fromString("5bbe785f-5ff3-46b9-b9fe-d9e6ca7a18e8"),
        person = FullPerson(
          type = PersonType.fullPerson,
          crn = "crn",
          name = "first last",
          dateOfBirth = LocalDate.parse("2022-09-08"),
          sex = "Male",
          status = PersonStatus.inCommunity,
          nomsNumber = "NOMS321",
          nationality = "English",
          religionOrBelief = null,
          genderIdentity = null,
          prisonName = null,
        ),
        arrivalDate = LocalDate.parse("2022-08-10"),
        departureDate = LocalDate.parse("2022-08-30"),
        status = BookingStatus.awaitingMinusArrival,
        extensions = listOf(),
        serviceName = ServiceName.approvedPremises,
        originalArrivalDate = LocalDate.parse("2022-08-10"),
        originalDepartureDate = LocalDate.parse("2022-08-30"),
        createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        departures = listOf(),
        cancellations = listOf(),
        turnarounds = listOf(),
        effectiveEndDate = LocalDate.parse("2022-08-30"),
        premises = BookingPremisesSummary(premisesEntity.id, premisesEntity.name),
      ),
    )
  }

  @Test
  fun `Approved Premises entity with application and assessment is correctly transformed`() {
    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(
        UserEntityFactory().withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }.produce(),
      )
      .produce()

    val oldAssessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
      .withCreatedAt(OffsetDateTime.now().minusDays(5))
      .withAllocatedToUser(
        UserEntityFactory().withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }.produce(),
      )
      .produce()

    val latestAssessment = ApprovedPremisesAssessmentEntityFactory()
      .withApplication(application)
      .withCreatedAt(OffsetDateTime.now().minusDays(2))
      .withAllocatedToUser(
        UserEntityFactory().withYieldedProbationRegion {
          ProbationRegionEntityFactory()
            .withYieldedApArea { ApAreaEntityFactory().produce() }
            .produce()
        }.produce(),
      )
      .produce()

    application.assessments = mutableListOf(oldAssessment, latestAssessment)

    val awaitingArrivalBooking = baseBookingEntity.copy(id = UUID.fromString("5bbe785f-5ff3-46b9-b9fe-d9e6ca7a18e8"), application = application)

    val transformedBooking = bookingTransformer.transformJpaToApi(
      awaitingArrivalBooking,
      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetail),
    )

    assertThat(transformedBooking).isEqualTo(
      Booking(
        id = UUID.fromString("5bbe785f-5ff3-46b9-b9fe-d9e6ca7a18e8"),
        person = FullPerson(
          type = PersonType.fullPerson,
          crn = "crn",
          name = "first last",
          dateOfBirth = LocalDate.parse("2022-09-08"),
          sex = "Male",
          status = PersonStatus.inCommunity,
          nomsNumber = "NOMS321",
          nationality = "English",
          religionOrBelief = null,
          genderIdentity = null,
          prisonName = null,
        ),
        arrivalDate = LocalDate.parse("2022-08-10"),
        departureDate = LocalDate.parse("2022-08-30"),
        status = BookingStatus.awaitingMinusArrival,
        extensions = listOf(),
        serviceName = ServiceName.approvedPremises,
        originalArrivalDate = LocalDate.parse("2022-08-10"),
        originalDepartureDate = LocalDate.parse("2022-08-30"),
        createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        departures = listOf(),
        cancellations = listOf(),
        turnarounds = listOf(),
        effectiveEndDate = LocalDate.parse("2022-08-30"),
        applicationId = application.id,
        assessmentId = latestAssessment.id,
        premises = BookingPremisesSummary(premisesEntity.id, premisesEntity.name),
      ),
    )
  }

  @Test
  fun `Temporary Accommodation Provisional entity is correctly transformed`() {
    val awaitingArrivalBooking = baseBookingEntity.copy(
      id = UUID.fromString("5bbe785f-5ff3-46b9-b9fe-d9e6ca7a18e8"),
      service = ServiceName.temporaryAccommodation.value,
    )

    val transformedBooking = bookingTransformer.transformJpaToApi(
      awaitingArrivalBooking,
      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetail),
    )

    assertThat(transformedBooking).isEqualTo(
      Booking(
        id = UUID.fromString("5bbe785f-5ff3-46b9-b9fe-d9e6ca7a18e8"),
        person = FullPerson(
          type = PersonType.fullPerson,
          crn = "crn",
          name = "first last",
          dateOfBirth = LocalDate.parse("2022-09-08"),
          sex = "Male",
          status = PersonStatus.inCommunity,
          nomsNumber = "NOMS321",
          nationality = "English",
          religionOrBelief = null,
          genderIdentity = null,
          prisonName = null,
        ),
        arrivalDate = LocalDate.parse("2022-08-10"),
        departureDate = LocalDate.parse("2022-08-30"),
        status = BookingStatus.PROVISIONAL,
        extensions = listOf(),
        serviceName = ServiceName.temporaryAccommodation,
        originalArrivalDate = LocalDate.parse("2022-08-10"),
        originalDepartureDate = LocalDate.parse("2022-08-30"),
        createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        departures = listOf(),
        cancellations = listOf(),
        turnarounds = listOf(),
        effectiveEndDate = LocalDate.parse("2022-08-30"),
        premises = BookingPremisesSummary(premisesEntity.id, premisesEntity.name),
      ),
    )
  }

  @Test
  fun `Approved Premises Non Arrival entity is correctly transformed`() {
    val nonArrivalBooking = baseBookingEntity.copy(id = UUID.fromString("655f72ba-51eb-4965-b6ac-45bcc6271b19")).apply {
      nonArrival = NonArrivalEntity(
        id = UUID.fromString("77e66712-b0a0-4968-b284-77ac1babe09c"),
        date = LocalDate.parse("2022-08-10"),
        reason = NonArrivalReasonEntity(id = UUID.fromString("7a87f93d-b9d6-423d-a87a-dfc693ab82f9"), name = "Unknown", isActive = true, legacyDeliusReasonCode = "A"),
        notes = null,
        booking = this,
        createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
      )
    }

    every { mockNonArrivalTransformer.transformJpaToApi(nonArrivalBooking.nonArrival) } returns Nonarrival(
      id = UUID.fromString("77e66712-b0a0-4968-b284-77ac1babe09c"),
      bookingId = UUID.fromString("655f72ba-51eb-4965-b6ac-45bcc6271b19"),
      date = LocalDate.parse("2022-08-10"),
      reason = NonArrivalReason(id = UUID.fromString("7a87f93d-b9d6-423d-a87a-dfc693ab82f9"), name = "Unknown", isActive = true),
      notes = null,
      createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
    )

    val transformedBooking = bookingTransformer.transformJpaToApi(
      nonArrivalBooking,
      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetail),
    )

    assertThat(transformedBooking).isEqualTo(
      Booking(
        id = UUID.fromString("655f72ba-51eb-4965-b6ac-45bcc6271b19"),
        person = FullPerson(
          type = PersonType.fullPerson,
          crn = "crn",
          name = "first last",
          dateOfBirth = LocalDate.parse("2022-09-08"),
          sex = "Male",
          status = PersonStatus.inCommunity,
          nomsNumber = "NOMS321",
          nationality = "English",
          religionOrBelief = null,
          genderIdentity = null,
          prisonName = null,
        ),
        arrivalDate = LocalDate.parse("2022-08-10"),
        departureDate = LocalDate.parse("2022-08-30"),
        keyWorker = null,
        status = BookingStatus.notMinusArrived,
        nonArrival = Nonarrival(
          id = UUID.fromString("77e66712-b0a0-4968-b284-77ac1babe09c"),
          bookingId = UUID.fromString("655f72ba-51eb-4965-b6ac-45bcc6271b19"),
          date = LocalDate.parse("2022-08-10"),
          reason = NonArrivalReason(id = UUID.fromString("7a87f93d-b9d6-423d-a87a-dfc693ab82f9"), name = "Unknown", isActive = true),
          notes = null,
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        ),
        extensions = listOf(),
        serviceName = ServiceName.approvedPremises,
        originalArrivalDate = LocalDate.parse("2022-08-10"),
        originalDepartureDate = LocalDate.parse("2022-08-30"),
        createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        departures = listOf(),
        cancellations = listOf(),
        turnarounds = listOf(),
        effectiveEndDate = LocalDate.parse("2022-08-30"),
        premises = BookingPremisesSummary(premisesEntity.id, premisesEntity.name),
      ),
    )
  }

  @Test
  fun `Approved Premises Arrived entity is correctly transformed`() {
    val arrivalBooking = baseBookingEntity.copy(id = UUID.fromString("443e79a9-b10a-4ad7-8be1-ffe301d2bbf3")).apply {
      arrivals += ArrivalEntity(
        id = UUID.fromString("77e66712-b0a0-4968-b284-77ac1babe09c"),
        arrivalDate = LocalDate.parse("2022-08-10"),
        arrivalDateTime = Instant.parse("2022-08-10T00:00:00Z"),
        expectedDepartureDate = LocalDate.parse("2022-08-16"),
        notes = null,
        booking = this,
        createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
      )
    }

    every { mockArrivalTransformer.transformJpaToApi(arrivalBooking.arrival) } returns Arrival(
      bookingId = UUID.fromString("443e79a9-b10a-4ad7-8be1-ffe301d2bbf3"),
      arrivalDate = LocalDate.parse("2022-08-10"),
      arrivalTime = "00:00:00",
      expectedDepartureDate = LocalDate.parse("2022-08-16"),
      notes = null,
      createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
    )

    val transformedBooking = bookingTransformer.transformJpaToApi(
      arrivalBooking,
      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetail),
    )

    assertThat(transformedBooking).isEqualTo(
      Booking(
        id = UUID.fromString("443e79a9-b10a-4ad7-8be1-ffe301d2bbf3"),
        person = FullPerson(
          type = PersonType.fullPerson,
          crn = "crn",
          name = "first last",
          dateOfBirth = LocalDate.parse("2022-09-08"),
          sex = "Male",
          status = PersonStatus.inCommunity,
          nomsNumber = "NOMS321",
          nationality = "English",
          religionOrBelief = null,
          genderIdentity = null,
          prisonName = null,
        ),
        arrivalDate = LocalDate.parse("2022-08-10"),
        departureDate = LocalDate.parse("2022-08-30"),
        keyWorker = null,
        status = BookingStatus.ARRIVED,
        arrival = Arrival(
          bookingId = UUID.fromString("443e79a9-b10a-4ad7-8be1-ffe301d2bbf3"),
          arrivalDate = LocalDate.parse("2022-08-10"),
          arrivalTime = "00:00:00",
          expectedDepartureDate = LocalDate.parse("2022-08-16"),
          notes = null,
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        ),
        extensions = listOf(),
        serviceName = ServiceName.approvedPremises,
        originalArrivalDate = LocalDate.parse("2022-08-10"),
        originalDepartureDate = LocalDate.parse("2022-08-30"),
        createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        departures = listOf(),
        cancellations = listOf(),
        turnarounds = listOf(),
        effectiveEndDate = LocalDate.parse("2022-08-30"),
        premises = BookingPremisesSummary(premisesEntity.id, premisesEntity.name),
      ),
    )
  }

  @Test
  fun `Approved Premises Cancelled entity is correctly transformed`() {
    val cancellationBooking = baseBookingEntity.copy(id = UUID.fromString("d182c0b8-1f5f-433b-9a0e-b0e51fee8b8d")).apply {
      cancellations = mutableListOf(
        CancellationEntity(
          id = UUID.fromString("77e66712-b0a0-4968-b284-77ac1babe09c"),
          date = LocalDate.parse("2022-08-10"),
          reason = CancellationReasonEntity(id = UUID.fromString("aa4ee8cf-3580-44e1-a3e1-6f3ee7d5ec67"), name = "Because", isActive = true, serviceScope = "approved-premises", sortOrder = 0),
          notes = null,
          booking = this,
          createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
          otherReason = null,
        ),
      )
    }

    every { mockCancellationTransformer.transformJpaToApi(cancellationBooking.cancellation) } returns Cancellation(
      bookingId = UUID.fromString("d182c0b8-1f5f-433b-9a0e-b0e51fee8b8d"),
      notes = null,
      date = LocalDate.parse("2022-08-10"),
      reason = CancellationReason(id = UUID.fromString("aa4ee8cf-3580-44e1-a3e1-6f3ee7d5ec67"), name = "Because", isActive = true, serviceScope = "approved-premises"),
      createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
      premisesName = premisesEntity.name,
    )

    val transformedBooking = bookingTransformer.transformJpaToApi(
      cancellationBooking,
      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetail),
    )

    assertThat(transformedBooking).isEqualTo(
      Booking(
        id = UUID.fromString("d182c0b8-1f5f-433b-9a0e-b0e51fee8b8d"),
        person = FullPerson(
          type = PersonType.fullPerson,
          crn = "crn",
          name = "first last",
          dateOfBirth = LocalDate.parse("2022-09-08"),
          sex = "Male",
          status = PersonStatus.inCommunity,
          nomsNumber = "NOMS321",
          nationality = "English",
          religionOrBelief = null,
          genderIdentity = null,
          prisonName = null,
        ),
        arrivalDate = LocalDate.parse("2022-08-10"),
        departureDate = LocalDate.parse("2022-08-30"),
        keyWorker = null,
        status = BookingStatus.cancelled,
        cancellation = Cancellation(
          bookingId = UUID.fromString("d182c0b8-1f5f-433b-9a0e-b0e51fee8b8d"),
          date = LocalDate.parse("2022-08-10"),
          reason = CancellationReason(id = UUID.fromString("aa4ee8cf-3580-44e1-a3e1-6f3ee7d5ec67"), name = "Because", isActive = true, serviceScope = "approved-premises"),
          notes = null,
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
          premisesName = premisesEntity.name,
        ),
        extensions = listOf(),
        serviceName = ServiceName.approvedPremises,
        originalArrivalDate = LocalDate.parse("2022-08-10"),
        originalDepartureDate = LocalDate.parse("2022-08-30"),
        createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        departures = listOf(),
        cancellations = listOf(
          Cancellation(
            bookingId = UUID.fromString("d182c0b8-1f5f-433b-9a0e-b0e51fee8b8d"),
            date = LocalDate.parse("2022-08-10"),
            reason = CancellationReason(id = UUID.fromString("aa4ee8cf-3580-44e1-a3e1-6f3ee7d5ec67"), name = "Because", isActive = true, serviceScope = "approved-premises"),
            notes = null,
            createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
            premisesName = premisesEntity.name,
          ),
        ),
        turnarounds = listOf(),
        effectiveEndDate = LocalDate.parse("2022-08-30"),
        premises = BookingPremisesSummary(premisesEntity.id, premisesEntity.name),
      ),
    )
  }

  @Test
  fun `Temporary Accommodation Entity with edited cancellation is correctly transformed`() {
    val cancellationBooking = baseBookingEntity.copy(id = UUID.fromString("d182c0b8-1f5f-433b-9a0e-b0e51fee8b8d")).apply {
      service = ServiceName.temporaryAccommodation.value
      cancellations = mutableListOf(
        CancellationEntity(
          id = UUID.fromString("77e66712-b0a0-4968-b284-77ac1babe09c"),
          date = LocalDate.parse("2022-08-10"),
          reason = CancellationReasonEntity(
            id = UUID.fromString("aa4ee8cf-3580-44e1-a3e1-6f3ee7d5ec67"),
            name = "Because",
            isActive = true,
            serviceScope = ServiceName.temporaryAccommodation.value,
            sortOrder = 0,
          ),
          notes = null,
          booking = this,
          createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
          otherReason = null,
        ),
        CancellationEntity(
          id = UUID.fromString("d34415c3-d128-45a0-9950-b84491ab8d11"),
          date = LocalDate.parse("2022-08-10"),
          reason = CancellationReasonEntity(
            id = UUID.fromString("dd6444f7-af56-436c-8451-ca993617471e"),
            name = "Some other reason",
            isActive = true,
            serviceScope = ServiceName.temporaryAccommodation.value,
            sortOrder = 0,
          ),
          notes = "Original reason chosen in error",
          booking = this,
          createdAt = OffsetDateTime.parse("2022-07-02T12:34:56.789Z"),
          otherReason = null,
        ),
      )
    }

    every { mockCancellationTransformer.transformJpaToApi(any()) } answers { answer ->
      val arg = answer.invocation.args[0] as CancellationEntity

      when (arg.id) {
        UUID.fromString("77e66712-b0a0-4968-b284-77ac1babe09c") -> Cancellation(
          bookingId = UUID.fromString("d182c0b8-1f5f-433b-9a0e-b0e51fee8b8d"),
          notes = null,
          date = LocalDate.parse("2022-08-10"),
          reason = CancellationReason(id = UUID.fromString("aa4ee8cf-3580-44e1-a3e1-6f3ee7d5ec67"), name = "Because", isActive = true, serviceScope = ServiceName.temporaryAccommodation.value),
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
          premisesName = premisesEntity.name,
        )
        UUID.fromString("d34415c3-d128-45a0-9950-b84491ab8d11") -> Cancellation(
          bookingId = UUID.fromString("d182c0b8-1f5f-433b-9a0e-b0e51fee8b8d"),
          notes = null,
          date = LocalDate.parse("2022-08-10"),
          reason = CancellationReason(id = UUID.fromString("dd6444f7-af56-436c-8451-ca993617471e"), name = "Some other reason", isActive = true, serviceScope = ServiceName.temporaryAccommodation.value),
          createdAt = Instant.parse("2022-07-02T12:34:56.789Z"),
          premisesName = premisesEntity.name,
        )
        else -> null
      }
    }

    val transformedBooking = bookingTransformer.transformJpaToApi(
      cancellationBooking,
      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetail),
    )

    assertThat(transformedBooking).isEqualTo(
      Booking(
        id = UUID.fromString("d182c0b8-1f5f-433b-9a0e-b0e51fee8b8d"),
        person = FullPerson(
          type = PersonType.fullPerson,
          crn = "crn",
          name = "first last",
          dateOfBirth = LocalDate.parse("2022-09-08"),
          sex = "Male",
          status = PersonStatus.inCommunity,
          nomsNumber = "NOMS321",
          nationality = "English",
          religionOrBelief = null,
          genderIdentity = null,
          prisonName = null,
        ),
        arrivalDate = LocalDate.parse("2022-08-10"),
        departureDate = LocalDate.parse("2022-08-30"),
        keyWorker = null,
        status = BookingStatus.cancelled,
        cancellation = Cancellation(
          bookingId = UUID.fromString("d182c0b8-1f5f-433b-9a0e-b0e51fee8b8d"),
          notes = null,
          date = LocalDate.parse("2022-08-10"),
          reason = CancellationReason(id = UUID.fromString("dd6444f7-af56-436c-8451-ca993617471e"), name = "Some other reason", isActive = true, serviceScope = ServiceName.temporaryAccommodation.value),
          createdAt = Instant.parse("2022-07-02T12:34:56.789Z"),
          premisesName = premisesEntity.name,
        ),
        extensions = listOf(),
        serviceName = ServiceName.temporaryAccommodation,
        originalArrivalDate = LocalDate.parse("2022-08-10"),
        originalDepartureDate = LocalDate.parse("2022-08-30"),
        createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        departures = listOf(),
        cancellations = listOf(
          Cancellation(
            bookingId = UUID.fromString("d182c0b8-1f5f-433b-9a0e-b0e51fee8b8d"),
            notes = null,
            date = LocalDate.parse("2022-08-10"),
            reason = CancellationReason(id = UUID.fromString("aa4ee8cf-3580-44e1-a3e1-6f3ee7d5ec67"), name = "Because", isActive = true, serviceScope = ServiceName.temporaryAccommodation.value),
            createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
            premisesName = premisesEntity.name,
          ),
          Cancellation(
            bookingId = UUID.fromString("d182c0b8-1f5f-433b-9a0e-b0e51fee8b8d"),
            notes = null,
            date = LocalDate.parse("2022-08-10"),
            reason = CancellationReason(id = UUID.fromString("dd6444f7-af56-436c-8451-ca993617471e"), name = "Some other reason", isActive = true, serviceScope = ServiceName.temporaryAccommodation.value),
            createdAt = Instant.parse("2022-07-02T12:34:56.789Z"),
            premisesName = premisesEntity.name,
          ),
        ),
        turnarounds = listOf(),
        effectiveEndDate = LocalDate.parse("2022-08-30"),
        premises = BookingPremisesSummary(premisesEntity.id, premisesEntity.name),
      ),
    )
  }

  @Test
  fun `Approved Premises Departed entity is correctly transformed`() {
    val bookingId = UUID.fromString("e0a3f9d7-0677-40bf-85a9-6673a7af33ee")
    val departedBooking = baseBookingEntity.copy(id = bookingId).apply {
      arrivals += ArrivalEntity(
        id = UUID.fromString("77e66712-b0a0-4968-b284-77ac1babe09c"),
        arrivalDate = LocalDate.parse("2022-08-10"),
        arrivalDateTime = Instant.parse("2022-08-10T00:00:00Z"),
        expectedDepartureDate = LocalDate.parse("2022-08-16"),
        notes = null,
        booking = this,
        createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
      )
      departures = mutableListOf(
        DepartureEntity(
          id = UUID.fromString("0d68d5b9-44c7-46cd-a52b-8185477b5edd"),
          dateTime = OffsetDateTime.parse("2022-08-30T15:30:15+01:00"),
          reason = DepartureReasonEntity(
            id = UUID.fromString("09e74ead-cf5a-40a1-a1be-739d3b97788f"),
            name = "Departure Reason",
            isActive = true,
            serviceScope = "*",
            legacyDeliusReasonCode = "A",
            parentReasonId = null,
          ),
          moveOnCategory = MoveOnCategoryEntity(
            id = UUID.fromString("bcfbb1b6-f89d-45eb-ae70-308cc6930633"),
            name = "Move on Category",
            isActive = true,
            serviceScope = "*",
            legacyDeliusCategoryCode = "CAT",
          ),
          destinationProvider = DestinationProviderEntity(
            id = UUID.fromString("29669658-c8f2-492c-8eab-2dd73a208d30"),
            name = "Destination Provider",
            isActive = true,
          ),
          notes = null,
          booking = this,
          createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
        ),
      )
    }

    every { mockArrivalTransformer.transformJpaToApi(departedBooking.arrival) } returns Arrival(
      bookingId = bookingId,
      arrivalDate = LocalDate.parse("2022-08-10"),
      expectedDepartureDate = LocalDate.parse("2022-08-16"),
      notes = null,
      arrivalTime = "00:00:00",
      createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
    )

    every { mockDepartureTransformer.transformJpaToApi(departedBooking.departure) } returns Departure(
      id = UUID.fromString("0d68d5b9-44c7-46cd-a52b-8185477b5edd"),
      bookingId = bookingId,
      dateTime = Instant.parse("2022-08-30T15:30:15+01:00"),
      reason = DepartureReason(
        id = UUID.fromString("09e74ead-cf5a-40a1-a1be-739d3b97788f"),
        name = "Departure Reason",
        isActive = true,
        serviceScope = "*",
      ),
      moveOnCategory = MoveOnCategory(
        id = UUID.fromString("bcfbb1b6-f89d-45eb-ae70-308cc6930633"),
        name = "Move on Category",
        isActive = true,
        serviceScope = "*",
      ),
      destinationProvider = DestinationProvider(
        id = UUID.fromString("29669658-c8f2-492c-8eab-2dd73a208d30"),
        name = "Destination Provider",
        isActive = true,
      ),
      notes = null,
      createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
    )

    val transformedBooking = bookingTransformer.transformJpaToApi(
      departedBooking,
      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetail),
    )

    assertThat(transformedBooking).isEqualTo(
      Booking(
        id = bookingId,
        person = FullPerson(
          type = PersonType.fullPerson,
          crn = "crn",
          name = "first last",
          dateOfBirth = LocalDate.parse("2022-09-08"),
          sex = "Male",
          status = PersonStatus.inCommunity,
          nomsNumber = "NOMS321",
          nationality = "English",
          religionOrBelief = null,
          genderIdentity = null,
          prisonName = null,
        ),
        arrivalDate = LocalDate.parse("2022-08-10"),
        departureDate = LocalDate.parse("2022-08-30"),
        keyWorker = null,
        status = BookingStatus.departed,
        arrival = Arrival(
          bookingId = bookingId,
          arrivalDate = LocalDate.parse("2022-08-10"),
          expectedDepartureDate = LocalDate.parse("2022-08-16"),
          notes = null,
          arrivalTime = "00:00:00",
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        ),
        departure = Departure(
          id = UUID.fromString("0d68d5b9-44c7-46cd-a52b-8185477b5edd"),
          bookingId = bookingId,
          dateTime = Instant.parse("2022-08-30T15:30:15+01:00"),
          reason = DepartureReason(
            id = UUID.fromString("09e74ead-cf5a-40a1-a1be-739d3b97788f"),
            name = "Departure Reason",
            isActive = true,
            serviceScope = "*",
          ),
          moveOnCategory = MoveOnCategory(
            id = UUID.fromString("bcfbb1b6-f89d-45eb-ae70-308cc6930633"),
            name = "Move on Category",
            isActive = true,
            serviceScope = "*",
          ),
          destinationProvider = DestinationProvider(
            id = UUID.fromString("29669658-c8f2-492c-8eab-2dd73a208d30"),
            name = "Destination Provider",
            isActive = true,
          ),
          notes = null,
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        ),
        extensions = listOf(),
        serviceName = ServiceName.approvedPremises,
        originalArrivalDate = LocalDate.parse("2022-08-10"),
        originalDepartureDate = LocalDate.parse("2022-08-30"),
        createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        departures = listOf(
          Departure(
            id = UUID.fromString("0d68d5b9-44c7-46cd-a52b-8185477b5edd"),
            bookingId = bookingId,
            dateTime = Instant.parse("2022-08-30T15:30:15+01:00"),
            reason = DepartureReason(
              id = UUID.fromString("09e74ead-cf5a-40a1-a1be-739d3b97788f"),
              name = "Departure Reason",
              isActive = true,
              serviceScope = "*",
            ),
            moveOnCategory = MoveOnCategory(
              id = UUID.fromString("bcfbb1b6-f89d-45eb-ae70-308cc6930633"),
              name = "Move on Category",
              isActive = true,
              serviceScope = "*",
            ),
            destinationProvider = DestinationProvider(
              id = UUID.fromString("29669658-c8f2-492c-8eab-2dd73a208d30"),
              name = "Destination Provider",
              isActive = true,
            ),
            notes = null,
            createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
          ),
        ),
        cancellations = listOf(),
        turnarounds = listOf(),
        effectiveEndDate = LocalDate.parse("2022-08-30"),
        premises = BookingPremisesSummary(premisesEntity.id, premisesEntity.name),
      ),
    )
  }

  @Test
  fun `Temporary Accommodation entity with zero day turnaround period and departure is correctly transformed to closed status`() {
    val bookingId = UUID.fromString("e0a3f9d7-0677-40bf-85a9-6673a7af33ee")
    val departedBooking = baseBookingEntity.copy(id = bookingId, service = ServiceName.temporaryAccommodation.value).apply {
      arrivals += ArrivalEntity(
        id = UUID.fromString("77e66712-b0a0-4968-b284-77ac1babe09c"),
        arrivalDate = LocalDate.parse("2022-08-10"),
        arrivalDateTime = Instant.parse("2022-08-10T00:00:00Z"),
        expectedDepartureDate = LocalDate.parse("2022-08-16"),
        notes = null,
        booking = this,
        createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
      )
      departures = mutableListOf(
        DepartureEntity(
          id = UUID.fromString("0d68d5b9-44c7-46cd-a52b-8185477b5edd"),
          dateTime = OffsetDateTime.parse("2022-08-30T15:30:15+01:00"),
          reason = DepartureReasonEntity(
            id = UUID.fromString("09e74ead-cf5a-40a1-a1be-739d3b97788f"),
            name = "Departure Reason",
            isActive = true,
            serviceScope = "*",
            legacyDeliusReasonCode = "A",
            parentReasonId = null,
          ),
          moveOnCategory = MoveOnCategoryEntity(
            id = UUID.fromString("bcfbb1b6-f89d-45eb-ae70-308cc6930633"),
            name = "Move on Category",
            isActive = true,
            serviceScope = "*",
            legacyDeliusCategoryCode = "CAT",
          ),
          destinationProvider = DestinationProviderEntity(
            id = UUID.fromString("29669658-c8f2-492c-8eab-2dd73a208d30"),
            name = "Destination Provider",
            isActive = true,
          ),
          notes = null,
          booking = this,
          createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
        ),
      )

      turnarounds += TurnaroundEntity(
        id = UUID.fromString("8c87e15d-f236-479e-b9fd-f4c5cc6bef8f"),
        workingDayCount = 0,
        createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
        booking = this,
      )
    }

    every { mockArrivalTransformer.transformJpaToApi(departedBooking.arrival) } returns Arrival(
      bookingId = bookingId,
      arrivalDate = LocalDate.parse("2022-08-10"),
      expectedDepartureDate = LocalDate.parse("2022-08-16"),
      notes = null,
      arrivalTime = "00:00:00",
      createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
    )

    every { mockDepartureTransformer.transformJpaToApi(departedBooking.departure) } returns Departure(
      id = UUID.fromString("0d68d5b9-44c7-46cd-a52b-8185477b5edd"),
      bookingId = bookingId,
      dateTime = Instant.parse("2022-08-30T15:30:15+01:00"),
      reason = DepartureReason(
        id = UUID.fromString("09e74ead-cf5a-40a1-a1be-739d3b97788f"),
        name = "Departure Reason",
        isActive = true,
        serviceScope = "*",
      ),
      moveOnCategory = MoveOnCategory(
        id = UUID.fromString("bcfbb1b6-f89d-45eb-ae70-308cc6930633"),
        name = "Move on Category",
        isActive = true,
        serviceScope = "*",
      ),
      destinationProvider = DestinationProvider(
        id = UUID.fromString("29669658-c8f2-492c-8eab-2dd73a208d30"),
        name = "Destination Provider",
        isActive = true,
      ),
      notes = null,
      createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
    )

    every { mockTurnaroundTransformer.transformJpaToApi(departedBooking.turnaround!!) } returns Turnaround(
      id = UUID.fromString("8c87e15d-f236-479e-b9fd-f4c5cc6bef8f"),
      workingDays = 0,
      createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
      bookingId = bookingId,
    )

    val transformedBooking = bookingTransformer.transformJpaToApi(
      departedBooking,
      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetail),
    )

    assertThat(transformedBooking).isEqualTo(
      Booking(
        id = bookingId,
        person = FullPerson(
          type = PersonType.fullPerson,
          crn = "crn",
          name = "first last",
          dateOfBirth = LocalDate.parse("2022-09-08"),
          sex = "Male",
          status = PersonStatus.inCommunity,
          nomsNumber = "NOMS321",
          nationality = "English",
          religionOrBelief = null,
          genderIdentity = null,
          prisonName = null,
        ),
        arrivalDate = LocalDate.parse("2022-08-10"),
        departureDate = LocalDate.parse("2022-08-30"),
        keyWorker = null,
        status = BookingStatus.closed,
        arrival = Arrival(
          bookingId = bookingId,
          arrivalDate = LocalDate.parse("2022-08-10"),
          expectedDepartureDate = LocalDate.parse("2022-08-16"),
          notes = null,
          arrivalTime = "00:00:00",
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        ),
        departure = Departure(
          id = UUID.fromString("0d68d5b9-44c7-46cd-a52b-8185477b5edd"),
          bookingId = bookingId,
          dateTime = Instant.parse("2022-08-30T15:30:15+01:00"),
          reason = DepartureReason(
            id = UUID.fromString("09e74ead-cf5a-40a1-a1be-739d3b97788f"),
            name = "Departure Reason",
            isActive = true,
            serviceScope = "*",
          ),
          moveOnCategory = MoveOnCategory(
            id = UUID.fromString("bcfbb1b6-f89d-45eb-ae70-308cc6930633"),
            name = "Move on Category",
            isActive = true,
            serviceScope = "*",
          ),
          destinationProvider = DestinationProvider(
            id = UUID.fromString("29669658-c8f2-492c-8eab-2dd73a208d30"),
            name = "Destination Provider",
            isActive = true,
          ),
          notes = null,
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        ),
        extensions = listOf(),
        serviceName = ServiceName.temporaryAccommodation,
        originalArrivalDate = LocalDate.parse("2022-08-10"),
        originalDepartureDate = LocalDate.parse("2022-08-30"),
        createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        departures = listOf(
          Departure(
            id = UUID.fromString("0d68d5b9-44c7-46cd-a52b-8185477b5edd"),
            bookingId = bookingId,
            dateTime = Instant.parse("2022-08-30T15:30:15+01:00"),
            reason = DepartureReason(
              id = UUID.fromString("09e74ead-cf5a-40a1-a1be-739d3b97788f"),
              name = "Departure Reason",
              isActive = true,
              serviceScope = "*",
            ),
            moveOnCategory = MoveOnCategory(
              id = UUID.fromString("bcfbb1b6-f89d-45eb-ae70-308cc6930633"),
              name = "Move on Category",
              isActive = true,
              serviceScope = "*",
            ),
            destinationProvider = DestinationProvider(
              id = UUID.fromString("29669658-c8f2-492c-8eab-2dd73a208d30"),
              name = "Destination Provider",
              isActive = true,
            ),
            notes = null,
            createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
          ),
        ),
        cancellations = listOf(),
        turnaround = Turnaround(
          id = UUID.fromString("8c87e15d-f236-479e-b9fd-f4c5cc6bef8f"),
          workingDays = 0,
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
          bookingId = bookingId,
        ),
        turnarounds = listOf(
          Turnaround(
            id = UUID.fromString("8c87e15d-f236-479e-b9fd-f4c5cc6bef8f"),
            workingDays = 0,
            createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
            bookingId = bookingId,
          ),
        ),
        effectiveEndDate = LocalDate.parse("2022-08-30"),
        premises = BookingPremisesSummary(premisesEntity.id, premisesEntity.name),
      ),
    )
  }

  @Test
  fun `Temporary Accommodation entity with non-zero day turnaround period and departure within turnaround period is correctly transformed to departed status`() {
    mockkStatic(OffsetDateTime::class, LocalDate::class)

    val timeNow = OffsetDateTime.parse("2022-08-17T15:30:00Z")
    val departedAt = OffsetDateTime.parse("2022-08-16T12:30:00Z")
    val expectedEffectiveEndDate = LocalDate.parse("2022-08-18")
    every { OffsetDateTime.now() } returns timeNow
    every { LocalDate.now() } returns timeNow.toLocalDate()

    val bookingId = UUID.fromString("e0a3f9d7-0677-40bf-85a9-6673a7af33ee")
    val departedBooking = baseBookingEntity.copy(id = bookingId, service = ServiceName.temporaryAccommodation.value).apply {
      departureDate = departedAt.toLocalDate()

      arrivals += ArrivalEntity(
        id = UUID.fromString("77e66712-b0a0-4968-b284-77ac1babe09c"),
        arrivalDate = LocalDate.parse("2022-08-10"),
        arrivalDateTime = Instant.parse("2022-08-10T00:00:00Z"),
        expectedDepartureDate = departedAt.toLocalDate(),
        notes = null,
        booking = this,
        createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
      )
      departures = mutableListOf(
        DepartureEntity(
          id = UUID.fromString("0d68d5b9-44c7-46cd-a52b-8185477b5edd"),
          dateTime = departedAt,
          reason = DepartureReasonEntity(
            id = UUID.fromString("09e74ead-cf5a-40a1-a1be-739d3b97788f"),
            name = "Departure Reason",
            isActive = true,
            serviceScope = "*",
            legacyDeliusReasonCode = "A",
            parentReasonId = null,
          ),
          moveOnCategory = MoveOnCategoryEntity(
            id = UUID.fromString("bcfbb1b6-f89d-45eb-ae70-308cc6930633"),
            name = "Move on Category",
            isActive = true,
            serviceScope = "*",
            legacyDeliusCategoryCode = "CAT",
          ),
          destinationProvider = DestinationProviderEntity(
            id = UUID.fromString("29669658-c8f2-492c-8eab-2dd73a208d30"),
            name = "Destination Provider",
            isActive = true,
          ),
          notes = null,
          booking = this,
          createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
        ),
      )

      turnarounds += TurnaroundEntity(
        id = UUID.fromString("8c87e15d-f236-479e-b9fd-f4c5cc6bef8f"),
        workingDayCount = 2,
        createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
        booking = this,
      )
    }

    every { mockArrivalTransformer.transformJpaToApi(departedBooking.arrival) } returns Arrival(
      bookingId = bookingId,
      arrivalDate = LocalDate.parse("2022-08-10"),
      expectedDepartureDate = LocalDate.parse("2022-08-16"),
      notes = null,
      arrivalTime = "00:00:00",
      createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
    )

    every { mockDepartureTransformer.transformJpaToApi(departedBooking.departure) } returns Departure(
      id = UUID.fromString("0d68d5b9-44c7-46cd-a52b-8185477b5edd"),
      bookingId = bookingId,
      dateTime = Instant.parse("2022-08-30T15:30:15+01:00"),
      reason = DepartureReason(
        id = UUID.fromString("09e74ead-cf5a-40a1-a1be-739d3b97788f"),
        name = "Departure Reason",
        isActive = true,
        serviceScope = "*",
      ),
      moveOnCategory = MoveOnCategory(
        id = UUID.fromString("bcfbb1b6-f89d-45eb-ae70-308cc6930633"),
        name = "Move on Category",
        isActive = true,
        serviceScope = "*",
      ),
      destinationProvider = DestinationProvider(
        id = UUID.fromString("29669658-c8f2-492c-8eab-2dd73a208d30"),
        name = "Destination Provider",
        isActive = true,
      ),
      notes = null,
      createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
    )

    every { mockTurnaroundTransformer.transformJpaToApi(departedBooking.turnaround!!) } returns Turnaround(
      id = UUID.fromString("8c87e15d-f236-479e-b9fd-f4c5cc6bef8f"),
      workingDays = 2,
      createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
      bookingId = bookingId,
    )

    every { mockWorkingDayService.addWorkingDays(departedAt.toLocalDate(), 1) } returns departedAt.toLocalDate().plusDays(1)
    every { mockWorkingDayService.addWorkingDays(departedAt.toLocalDate(), 2) } returns expectedEffectiveEndDate

    val transformedBooking = bookingTransformer.transformJpaToApi(
      departedBooking,
      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetail),
    )

    assertThat(transformedBooking).isEqualTo(
      Booking(
        id = bookingId,
        person = FullPerson(
          type = PersonType.fullPerson,
          crn = "crn",
          name = "first last",
          dateOfBirth = LocalDate.parse("2022-09-08"),
          sex = "Male",
          status = PersonStatus.inCommunity,
          nomsNumber = "NOMS321",
          nationality = "English",
          religionOrBelief = null,
          genderIdentity = null,
          prisonName = null,
        ),
        arrivalDate = LocalDate.parse("2022-08-10"),
        departureDate = departedAt.toLocalDate(),
        keyWorker = null,
        status = BookingStatus.departed,
        arrival = Arrival(
          bookingId = bookingId,
          arrivalDate = LocalDate.parse("2022-08-10"),
          expectedDepartureDate = LocalDate.parse("2022-08-16"),
          notes = null,
          arrivalTime = "00:00:00",
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        ),
        departure = Departure(
          id = UUID.fromString("0d68d5b9-44c7-46cd-a52b-8185477b5edd"),
          bookingId = bookingId,
          dateTime = Instant.parse("2022-08-30T15:30:15+01:00"),
          reason = DepartureReason(
            id = UUID.fromString("09e74ead-cf5a-40a1-a1be-739d3b97788f"),
            name = "Departure Reason",
            isActive = true,
            serviceScope = "*",
          ),
          moveOnCategory = MoveOnCategory(
            id = UUID.fromString("bcfbb1b6-f89d-45eb-ae70-308cc6930633"),
            name = "Move on Category",
            isActive = true,
            serviceScope = "*",
          ),
          destinationProvider = DestinationProvider(
            id = UUID.fromString("29669658-c8f2-492c-8eab-2dd73a208d30"),
            name = "Destination Provider",
            isActive = true,
          ),
          notes = null,
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        ),
        extensions = listOf(),
        serviceName = ServiceName.temporaryAccommodation,
        originalArrivalDate = LocalDate.parse("2022-08-10"),
        originalDepartureDate = LocalDate.parse("2022-08-30"),
        createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        departures = listOf(
          Departure(
            id = UUID.fromString("0d68d5b9-44c7-46cd-a52b-8185477b5edd"),
            bookingId = bookingId,
            dateTime = Instant.parse("2022-08-30T15:30:15+01:00"),
            reason = DepartureReason(
              id = UUID.fromString("09e74ead-cf5a-40a1-a1be-739d3b97788f"),
              name = "Departure Reason",
              isActive = true,
              serviceScope = "*",
            ),
            moveOnCategory = MoveOnCategory(
              id = UUID.fromString("bcfbb1b6-f89d-45eb-ae70-308cc6930633"),
              name = "Move on Category",
              isActive = true,
              serviceScope = "*",
            ),
            destinationProvider = DestinationProvider(
              id = UUID.fromString("29669658-c8f2-492c-8eab-2dd73a208d30"),
              name = "Destination Provider",
              isActive = true,
            ),
            notes = null,
            createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
          ),
        ),
        cancellations = listOf(),
        turnaround = Turnaround(
          id = UUID.fromString("8c87e15d-f236-479e-b9fd-f4c5cc6bef8f"),
          workingDays = 2,
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
          bookingId = bookingId,
        ),
        turnarounds = listOf(
          Turnaround(
            id = UUID.fromString("8c87e15d-f236-479e-b9fd-f4c5cc6bef8f"),
            workingDays = 2,
            createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
            bookingId = bookingId,
          ),
        ),
        turnaroundStartDate = departedAt.toLocalDate().plusDays(1),
        effectiveEndDate = expectedEffectiveEndDate,
        premises = BookingPremisesSummary(premisesEntity.id, premisesEntity.name),
      ),
    )
  }

  @Test
  fun `Temporary Accommodation entity with non-zero day turnaround period and departure with turnaround period in past is correctly transformed to closed status`() {
    mockkStatic(OffsetDateTime::class, LocalDate::class)

    val timeNow = OffsetDateTime.parse("2022-08-19T15:30:00Z")
    val departedAt = OffsetDateTime.parse("2022-08-16T12:30:00Z")
    val expectedEffectiveEndDate = LocalDate.parse("2022-08-18")
    every { OffsetDateTime.now() } returns timeNow
    every { LocalDate.now() } returns timeNow.toLocalDate()

    val bookingId = UUID.fromString("e0a3f9d7-0677-40bf-85a9-6673a7af33ee")
    val departedBooking = baseBookingEntity.copy(id = bookingId, service = ServiceName.temporaryAccommodation.value).apply {
      departureDate = departedAt.toLocalDate()

      arrivals += ArrivalEntity(
        id = UUID.fromString("77e66712-b0a0-4968-b284-77ac1babe09c"),
        arrivalDate = LocalDate.parse("2022-08-10"),
        arrivalDateTime = Instant.parse("2022-08-10T00:00:00Z"),
        expectedDepartureDate = departedAt.toLocalDate(),
        notes = null,
        booking = this,
        createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
      )
      departures = mutableListOf(
        DepartureEntity(
          id = UUID.fromString("0d68d5b9-44c7-46cd-a52b-8185477b5edd"),
          dateTime = departedAt,
          reason = DepartureReasonEntity(
            id = UUID.fromString("09e74ead-cf5a-40a1-a1be-739d3b97788f"),
            name = "Departure Reason",
            isActive = true,
            serviceScope = "*",
            legacyDeliusReasonCode = "A",
            parentReasonId = null,
          ),
          moveOnCategory = MoveOnCategoryEntity(
            id = UUID.fromString("bcfbb1b6-f89d-45eb-ae70-308cc6930633"),
            name = "Move on Category",
            isActive = true,
            serviceScope = "*",
            legacyDeliusCategoryCode = "CAT",
          ),
          destinationProvider = DestinationProviderEntity(
            id = UUID.fromString("29669658-c8f2-492c-8eab-2dd73a208d30"),
            name = "Destination Provider",
            isActive = true,
          ),
          notes = null,
          booking = this,
          createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
        ),
      )

      turnarounds += TurnaroundEntity(
        id = UUID.fromString("8c87e15d-f236-479e-b9fd-f4c5cc6bef8f"),
        workingDayCount = 2,
        createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
        booking = this,
      )
    }

    every { mockArrivalTransformer.transformJpaToApi(departedBooking.arrival) } returns Arrival(
      bookingId = bookingId,
      arrivalDate = LocalDate.parse("2022-08-10"),
      expectedDepartureDate = LocalDate.parse("2022-08-16"),
      notes = null,
      arrivalTime = "00:00:00",
      createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
    )

    every { mockDepartureTransformer.transformJpaToApi(departedBooking.departure) } returns Departure(
      id = UUID.fromString("0d68d5b9-44c7-46cd-a52b-8185477b5edd"),
      bookingId = bookingId,
      dateTime = Instant.parse("2022-08-30T15:30:15+01:00"),
      reason = DepartureReason(
        id = UUID.fromString("09e74ead-cf5a-40a1-a1be-739d3b97788f"),
        name = "Departure Reason",
        isActive = true,
        serviceScope = "*",
      ),
      moveOnCategory = MoveOnCategory(
        id = UUID.fromString("bcfbb1b6-f89d-45eb-ae70-308cc6930633"),
        name = "Move on Category",
        isActive = true,
        serviceScope = "*",
      ),
      destinationProvider = DestinationProvider(
        id = UUID.fromString("29669658-c8f2-492c-8eab-2dd73a208d30"),
        name = "Destination Provider",
        isActive = true,
      ),
      notes = null,
      createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
    )

    every { mockTurnaroundTransformer.transformJpaToApi(departedBooking.turnaround!!) } returns Turnaround(
      id = UUID.fromString("8c87e15d-f236-479e-b9fd-f4c5cc6bef8f"),
      workingDays = 2,
      createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
      bookingId = bookingId,
    )

    every { mockWorkingDayService.addWorkingDays(departedAt.toLocalDate(), 1) } returns departedAt.toLocalDate().plusDays(1)
    every { mockWorkingDayService.addWorkingDays(departedAt.toLocalDate(), 2) } returns expectedEffectiveEndDate

    val transformedBooking = bookingTransformer.transformJpaToApi(
      departedBooking,
      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetail),
    )

    assertThat(transformedBooking).isEqualTo(
      Booking(
        id = bookingId,
        person = FullPerson(
          type = PersonType.fullPerson,
          crn = "crn",
          name = "first last",
          dateOfBirth = LocalDate.parse("2022-09-08"),
          sex = "Male",
          status = PersonStatus.inCommunity,
          nomsNumber = "NOMS321",
          nationality = "English",
          religionOrBelief = null,
          genderIdentity = null,
          prisonName = null,
        ),
        arrivalDate = LocalDate.parse("2022-08-10"),
        departureDate = departedAt.toLocalDate(),
        keyWorker = null,
        status = BookingStatus.closed,
        arrival = Arrival(
          bookingId = bookingId,
          arrivalDate = LocalDate.parse("2022-08-10"),
          expectedDepartureDate = LocalDate.parse("2022-08-16"),
          notes = null,
          arrivalTime = "00:00:00",
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        ),
        departure = Departure(
          id = UUID.fromString("0d68d5b9-44c7-46cd-a52b-8185477b5edd"),
          bookingId = bookingId,
          dateTime = Instant.parse("2022-08-30T15:30:15+01:00"),
          reason = DepartureReason(
            id = UUID.fromString("09e74ead-cf5a-40a1-a1be-739d3b97788f"),
            name = "Departure Reason",
            isActive = true,
            serviceScope = "*",
          ),
          moveOnCategory = MoveOnCategory(
            id = UUID.fromString("bcfbb1b6-f89d-45eb-ae70-308cc6930633"),
            name = "Move on Category",
            isActive = true,
            serviceScope = "*",
          ),
          destinationProvider = DestinationProvider(
            id = UUID.fromString("29669658-c8f2-492c-8eab-2dd73a208d30"),
            name = "Destination Provider",
            isActive = true,
          ),
          notes = null,
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        ),
        extensions = listOf(),
        serviceName = ServiceName.temporaryAccommodation,
        originalArrivalDate = LocalDate.parse("2022-08-10"),
        originalDepartureDate = LocalDate.parse("2022-08-30"),
        createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        departures = listOf(
          Departure(
            id = UUID.fromString("0d68d5b9-44c7-46cd-a52b-8185477b5edd"),
            bookingId = bookingId,
            dateTime = Instant.parse("2022-08-30T15:30:15+01:00"),
            reason = DepartureReason(
              id = UUID.fromString("09e74ead-cf5a-40a1-a1be-739d3b97788f"),
              name = "Departure Reason",
              isActive = true,
              serviceScope = "*",
            ),
            moveOnCategory = MoveOnCategory(
              id = UUID.fromString("bcfbb1b6-f89d-45eb-ae70-308cc6930633"),
              name = "Move on Category",
              isActive = true,
              serviceScope = "*",
            ),
            destinationProvider = DestinationProvider(
              id = UUID.fromString("29669658-c8f2-492c-8eab-2dd73a208d30"),
              name = "Destination Provider",
              isActive = true,
            ),
            notes = null,
            createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
          ),
        ),
        cancellations = listOf(),
        turnaround = Turnaround(
          id = UUID.fromString("8c87e15d-f236-479e-b9fd-f4c5cc6bef8f"),
          workingDays = 2,
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
          bookingId = bookingId,
        ),
        turnarounds = listOf(
          Turnaround(
            id = UUID.fromString("8c87e15d-f236-479e-b9fd-f4c5cc6bef8f"),
            workingDays = 2,
            createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
            bookingId = bookingId,
          ),
        ),
        turnaroundStartDate = departedAt.toLocalDate().plusDays(1),
        effectiveEndDate = expectedEffectiveEndDate,
        premises = BookingPremisesSummary(premisesEntity.id, premisesEntity.name),
      ),
    )
  }

  @Test
  fun `Temporary Accommodation Entity with edited departure is correctly transformed`() {
    val bookingId = UUID.fromString("e0a3f9d7-0677-40bf-85a9-6673a7af33ee")
    val departedBooking = baseBookingEntity.copy(id = bookingId).apply {
      service = ServiceName.temporaryAccommodation.value
      arrivals += ArrivalEntity(
        id = UUID.fromString("77e66712-b0a0-4968-b284-77ac1babe09c"),
        arrivalDate = LocalDate.parse("2022-08-10"),
        arrivalDateTime = Instant.parse("2022-08-10T00:00:00Z"),
        expectedDepartureDate = LocalDate.parse("2022-08-16"),
        notes = null,
        booking = this,
        createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
      )
      departures = mutableListOf(
        DepartureEntity(
          id = UUID.fromString("0d68d5b9-44c7-46cd-a52b-8185477b5edd"),
          dateTime = OffsetDateTime.parse("2022-08-30T15:30:15+01:00"),
          reason = DepartureReasonEntity(
            id = UUID.fromString("09e74ead-cf5a-40a1-a1be-739d3b97788f"),
            name = "Departure Reason",
            isActive = true,
            serviceScope = "*",
            legacyDeliusReasonCode = "A",
            parentReasonId = null,
          ),
          moveOnCategory = MoveOnCategoryEntity(
            id = UUID.fromString("bcfbb1b6-f89d-45eb-ae70-308cc6930633"),
            name = "Move on Category",
            isActive = true,
            serviceScope = "*",
            legacyDeliusCategoryCode = "CAT",
          ),
          destinationProvider = DestinationProviderEntity(
            id = UUID.fromString("29669658-c8f2-492c-8eab-2dd73a208d30"),
            name = "Destination Provider",
            isActive = true,
          ),
          notes = null,
          booking = this,
          createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
        ),
        DepartureEntity(
          id = UUID.fromString("f184e3e9-474d-4083-9f3d-628f462907fc"),
          dateTime = OffsetDateTime.parse("2022-08-30T15:30:15+01:00"),
          reason = DepartureReasonEntity(
            id = UUID.fromString("09e74ead-cf5a-40a1-a1be-739d3b97788f"),
            name = "Departure Reason",
            isActive = true,
            serviceScope = "*",
            legacyDeliusReasonCode = "A",
            parentReasonId = null,
          ),
          moveOnCategory = MoveOnCategoryEntity(
            id = UUID.fromString("3fc011f3-81ae-46fa-a066-84de8423ab87"),
            name = "Some other category",
            isActive = true,
            serviceScope = "*",
            legacyDeliusCategoryCode = "CAT",
          ),
          destinationProvider = DestinationProviderEntity(
            id = UUID.fromString("48a5fac2-6ac6-4dad-8389-b59cc6b6112c"),
            name = "Some other destination provider",
            isActive = true,
          ),
          notes = "Updated move-on category and destination provider after receiving new information",
          booking = this,
          createdAt = OffsetDateTime.parse("2022-07-02T12:34:56.789Z"),
        ),
      )
    }

    every { mockArrivalTransformer.transformJpaToApi(departedBooking.arrival) } returns Arrival(
      bookingId = bookingId,
      arrivalDate = LocalDate.parse("2022-08-10"),
      expectedDepartureDate = LocalDate.parse("2022-08-16"),
      notes = null,
      arrivalTime = "00:00:00",
      createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
    )

    every { mockDepartureTransformer.transformJpaToApi(any()) } answers { answer ->
      val arg = answer.invocation.args[0] as DepartureEntity

      when (arg.id) {
        UUID.fromString("0d68d5b9-44c7-46cd-a52b-8185477b5edd") -> Departure(
          id = UUID.fromString("0d68d5b9-44c7-46cd-a52b-8185477b5edd"),
          bookingId = bookingId,
          dateTime = Instant.parse("2022-08-30T15:30:15+01:00"),
          reason = DepartureReason(
            id = UUID.fromString("09e74ead-cf5a-40a1-a1be-739d3b97788f"),
            name = "Departure Reason",
            isActive = true,
            serviceScope = "*",
          ),
          moveOnCategory = MoveOnCategory(
            id = UUID.fromString("bcfbb1b6-f89d-45eb-ae70-308cc6930633"),
            name = "Move on Category",
            isActive = true,
            serviceScope = "*",
          ),
          destinationProvider = DestinationProvider(
            id = UUID.fromString("29669658-c8f2-492c-8eab-2dd73a208d30"),
            name = "Destination Provider",
            isActive = true,
          ),
          notes = null,
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        )
        UUID.fromString("f184e3e9-474d-4083-9f3d-628f462907fc") -> Departure(
          id = UUID.fromString("f184e3e9-474d-4083-9f3d-628f462907fc"),
          bookingId = bookingId,
          dateTime = Instant.parse("2022-08-30T15:30:15+01:00"),
          reason = DepartureReason(
            id = UUID.fromString("09e74ead-cf5a-40a1-a1be-739d3b97788f"),
            name = "Departure Reason",
            isActive = true,
            serviceScope = "*",
          ),
          moveOnCategory = MoveOnCategory(
            id = UUID.fromString("3fc011f3-81ae-46fa-a066-84de8423ab87"),
            name = "Some other category",
            isActive = true,
            serviceScope = "*",
          ),
          destinationProvider = DestinationProvider(
            id = UUID.fromString("48a5fac2-6ac6-4dad-8389-b59cc6b6112c"),
            name = "Some other destination provider",
            isActive = true,
          ),
          notes = "Updated move-on category and destination provider after receiving new information",
          createdAt = Instant.parse("2022-07-02T12:34:56.789Z"),
        )

        else -> null
      }
    }

    val transformedBooking = bookingTransformer.transformJpaToApi(
      departedBooking,
      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetail),
    )

    assertThat(transformedBooking).isEqualTo(
      Booking(
        id = bookingId,
        person = FullPerson(
          type = PersonType.fullPerson,
          crn = "crn",
          name = "first last",
          dateOfBirth = LocalDate.parse("2022-09-08"),
          sex = "Male",
          status = PersonStatus.inCommunity,
          nomsNumber = "NOMS321",
          nationality = "English",
          religionOrBelief = null,
          genderIdentity = null,
          prisonName = null,
        ),
        arrivalDate = LocalDate.parse("2022-08-10"),
        departureDate = LocalDate.parse("2022-08-30"),
        keyWorker = null,
        status = BookingStatus.closed,
        arrival = Arrival(
          bookingId = bookingId,
          arrivalDate = LocalDate.parse("2022-08-10"),
          expectedDepartureDate = LocalDate.parse("2022-08-16"),
          notes = null,
          arrivalTime = "00:00:00",
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        ),
        departure = Departure(
          id = UUID.fromString("f184e3e9-474d-4083-9f3d-628f462907fc"),
          bookingId = bookingId,
          dateTime = Instant.parse("2022-08-30T15:30:15+01:00"),
          reason = DepartureReason(
            id = UUID.fromString("09e74ead-cf5a-40a1-a1be-739d3b97788f"),
            name = "Departure Reason",
            isActive = true,
            serviceScope = "*",
          ),
          moveOnCategory = MoveOnCategory(
            id = UUID.fromString("3fc011f3-81ae-46fa-a066-84de8423ab87"),
            name = "Some other category",
            isActive = true,
            serviceScope = "*",
          ),
          destinationProvider = DestinationProvider(
            id = UUID.fromString("48a5fac2-6ac6-4dad-8389-b59cc6b6112c"),
            name = "Some other destination provider",
            isActive = true,
          ),
          notes = "Updated move-on category and destination provider after receiving new information",
          createdAt = Instant.parse("2022-07-02T12:34:56.789Z"),
        ),
        extensions = listOf(),
        serviceName = ServiceName.temporaryAccommodation,
        originalArrivalDate = LocalDate.parse("2022-08-10"),
        originalDepartureDate = LocalDate.parse("2022-08-30"),
        createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        departures = listOf(
          Departure(
            id = UUID.fromString("0d68d5b9-44c7-46cd-a52b-8185477b5edd"),
            bookingId = bookingId,
            dateTime = Instant.parse("2022-08-30T15:30:15+01:00"),
            reason = DepartureReason(
              id = UUID.fromString("09e74ead-cf5a-40a1-a1be-739d3b97788f"),
              name = "Departure Reason",
              isActive = true,
              serviceScope = "*",
            ),
            moveOnCategory = MoveOnCategory(
              id = UUID.fromString("bcfbb1b6-f89d-45eb-ae70-308cc6930633"),
              name = "Move on Category",
              isActive = true,
              serviceScope = "*",
            ),
            destinationProvider = DestinationProvider(
              id = UUID.fromString("29669658-c8f2-492c-8eab-2dd73a208d30"),
              name = "Destination Provider",
              isActive = true,
            ),
            notes = null,
            createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
          ),
          Departure(
            id = UUID.fromString("f184e3e9-474d-4083-9f3d-628f462907fc"),
            bookingId = bookingId,
            dateTime = Instant.parse("2022-08-30T15:30:15+01:00"),
            reason = DepartureReason(
              id = UUID.fromString("09e74ead-cf5a-40a1-a1be-739d3b97788f"),
              name = "Departure Reason",
              isActive = true,
              serviceScope = "*",
            ),
            moveOnCategory = MoveOnCategory(
              id = UUID.fromString("3fc011f3-81ae-46fa-a066-84de8423ab87"),
              name = "Some other category",
              isActive = true,
              serviceScope = "*",
            ),
            destinationProvider = DestinationProvider(
              id = UUID.fromString("48a5fac2-6ac6-4dad-8389-b59cc6b6112c"),
              name = "Some other destination provider",
              isActive = true,
            ),
            notes = "Updated move-on category and destination provider after receiving new information",
            createdAt = Instant.parse("2022-07-02T12:34:56.789Z"),
          ),
        ),
        cancellations = listOf(),
        turnarounds = listOf(),
        effectiveEndDate = LocalDate.parse("2022-08-30"),
        premises = BookingPremisesSummary(premisesEntity.id, premisesEntity.name),
      ),
    )
  }

  @Test
  fun `Temporary Accommodation Confirmed entity is correctly transformed`() {
    val confirmationBooking = baseBookingEntity.copy(
      id = UUID.fromString("1c29a729-6059-4939-8641-1caa61a38815"),
      service = ServiceName.temporaryAccommodation.value,
    ).apply {
      confirmation = ConfirmationEntity(
        id = UUID.fromString("69fc6350-b2ec-4e99-9a2f-e829e83535e8"),
        dateTime = OffsetDateTime.parse("2022-11-23T12:34:56.789Z"),
        notes = null,
        booking = this,
        createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
      )
    }

    every { mockConfirmationTransformer.transformJpaToApi(confirmationBooking.confirmation) } returns Confirmation(
      id = UUID.fromString("69fc6350-b2ec-4e99-9a2f-e829e83535e8"),
      bookingId = UUID.fromString("1c29a729-6059-4939-8641-1caa61a38815"),
      notes = null,
      dateTime = Instant.parse("2022-11-23T12:34:56.789Z"),
      createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
    )

    val transformedBooking = bookingTransformer.transformJpaToApi(
      confirmationBooking,
      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetail),
    )

    assertThat(transformedBooking).isEqualTo(
      Booking(
        id = UUID.fromString("1c29a729-6059-4939-8641-1caa61a38815"),
        person = FullPerson(
          type = PersonType.fullPerson,
          crn = "crn",
          name = "first last",
          dateOfBirth = LocalDate.parse("2022-09-08"),
          sex = "Male",
          status = PersonStatus.inCommunity,
          nomsNumber = "NOMS321",
          nationality = "English",
          religionOrBelief = null,
          genderIdentity = null,
          prisonName = null,
        ),
        arrivalDate = LocalDate.parse("2022-08-10"),
        departureDate = LocalDate.parse("2022-08-30"),
        keyWorker = null,
        status = BookingStatus.CONFIRMED,
        cancellation = null,
        confirmation = Confirmation(
          id = UUID.fromString("69fc6350-b2ec-4e99-9a2f-e829e83535e8"),
          bookingId = UUID.fromString("1c29a729-6059-4939-8641-1caa61a38815"),
          notes = null,
          dateTime = Instant.parse("2022-11-23T12:34:56.789Z"),
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        ),
        extensions = listOf(),
        serviceName = ServiceName.temporaryAccommodation,
        originalArrivalDate = LocalDate.parse("2022-08-10"),
        originalDepartureDate = LocalDate.parse("2022-08-30"),
        createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        departures = listOf(),
        cancellations = listOf(),
        turnarounds = listOf(),
        effectiveEndDate = LocalDate.parse("2022-08-30"),
        premises = BookingPremisesSummary(premisesEntity.id, premisesEntity.name),
      ),
    )
  }

  @Test
  fun `Turnarounds on a booking are correctly transformed`() {
    val awaitingArrivalBooking = baseBookingEntity.copy(
      id = UUID.fromString("5bbe785f-5ff3-46b9-b9fe-d9e6ca7a18e8"),
      service = ServiceName.temporaryAccommodation.value,
      turnarounds = mutableListOf(),
    )

    val turnaround1 = TurnaroundEntity(
      id = UUID.fromString("34ae3124-cf7a-47d5-86c1-ef9ab4255e30"),
      workingDayCount = 2,
      createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
      booking = awaitingArrivalBooking,
    )
    val turnaround2 = TurnaroundEntity(
      id = UUID.fromString("b2af671a-997d-443e-906d-3a1a28c71416"),
      workingDayCount = 3,
      createdAt = OffsetDateTime.parse("2022-07-02T10:11:12.345Z"),
      booking = awaitingArrivalBooking,
    )
    val turnaround3 = TurnaroundEntity(
      id = UUID.fromString("146d05f8-ba83-42ae-a6d7-807a16b7946d"),
      workingDayCount = 4,
      createdAt = OffsetDateTime.parse("2022-07-03T09:08:07.654Z"),
      booking = awaitingArrivalBooking,
    )

    awaitingArrivalBooking.turnarounds += listOf(turnaround1, turnaround2, turnaround3)

    every { mockTurnaroundTransformer.transformJpaToApi(turnaround1) } returns Turnaround(
      id = UUID.fromString("34ae3124-cf7a-47d5-86c1-ef9ab4255e30"),
      bookingId = UUID.fromString("5bbe785f-5ff3-46b9-b9fe-d9e6ca7a18e8"),
      workingDays = 2,
      createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
    )

    every { mockTurnaroundTransformer.transformJpaToApi(turnaround2) } returns Turnaround(
      id = UUID.fromString("b2af671a-997d-443e-906d-3a1a28c71416"),
      bookingId = UUID.fromString("5bbe785f-5ff3-46b9-b9fe-d9e6ca7a18e8"),
      workingDays = 3,
      createdAt = Instant.parse("2022-07-02T10:11:12.345Z"),
    )

    every { mockTurnaroundTransformer.transformJpaToApi(turnaround3) } returns Turnaround(
      id = UUID.fromString("146d05f8-ba83-42ae-a6d7-807a16b7946d"),
      bookingId = UUID.fromString("5bbe785f-5ff3-46b9-b9fe-d9e6ca7a18e8"),
      workingDays = 4,
      createdAt = Instant.parse("2022-07-03T09:08:07.654Z"),
    )

    every { mockWorkingDayService.addWorkingDays(LocalDate.parse("2022-08-30"), 1) } returns LocalDate.parse("2022-08-31")
    every { mockWorkingDayService.addWorkingDays(LocalDate.parse("2022-08-30"), 4) } returns LocalDate.parse("2022-09-05")

    val transformedBooking = bookingTransformer.transformJpaToApi(
      awaitingArrivalBooking,
      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetail),
    )

    assertThat(transformedBooking).isEqualTo(
      Booking(
        id = UUID.fromString("5bbe785f-5ff3-46b9-b9fe-d9e6ca7a18e8"),
        person = FullPerson(
          type = PersonType.fullPerson,
          crn = "crn",
          name = "first last",
          dateOfBirth = LocalDate.parse("2022-09-08"),
          sex = "Male",
          status = PersonStatus.inCommunity,
          nomsNumber = "NOMS321",
          nationality = "English",
          religionOrBelief = null,
          genderIdentity = null,
          prisonName = null,
        ),
        arrivalDate = LocalDate.parse("2022-08-10"),
        departureDate = LocalDate.parse("2022-08-30"),
        status = BookingStatus.PROVISIONAL,
        extensions = listOf(),
        serviceName = ServiceName.temporaryAccommodation,
        originalArrivalDate = LocalDate.parse("2022-08-10"),
        originalDepartureDate = LocalDate.parse("2022-08-30"),
        createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        departures = listOf(),
        cancellations = listOf(),
        turnaround = Turnaround(
          id = UUID.fromString("146d05f8-ba83-42ae-a6d7-807a16b7946d"),
          bookingId = UUID.fromString("5bbe785f-5ff3-46b9-b9fe-d9e6ca7a18e8"),
          workingDays = 4,
          createdAt = Instant.parse("2022-07-03T09:08:07.654Z"),
        ),
        turnarounds = listOf(
          Turnaround(
            id = UUID.fromString("34ae3124-cf7a-47d5-86c1-ef9ab4255e30"),
            bookingId = UUID.fromString("5bbe785f-5ff3-46b9-b9fe-d9e6ca7a18e8"),
            workingDays = 2,
            createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
          ),
          Turnaround(
            id = UUID.fromString("b2af671a-997d-443e-906d-3a1a28c71416"),
            bookingId = UUID.fromString("5bbe785f-5ff3-46b9-b9fe-d9e6ca7a18e8"),
            workingDays = 3,
            createdAt = Instant.parse("2022-07-02T10:11:12.345Z"),
          ),
          Turnaround(
            id = UUID.fromString("146d05f8-ba83-42ae-a6d7-807a16b7946d"),
            bookingId = UUID.fromString("5bbe785f-5ff3-46b9-b9fe-d9e6ca7a18e8"),
            workingDays = 4,
            createdAt = Instant.parse("2022-07-03T09:08:07.654Z"),
          ),
        ),
        turnaroundStartDate = LocalDate.parse("2022-08-31"),
        effectiveEndDate = LocalDate.parse("2022-09-05"),
        premises = BookingPremisesSummary(premisesEntity.id, premisesEntity.name),
      ),
    )
  }

  @Test
  fun `Booking is correctly transformed to withdrawable`() {
    val id = UUID.randomUUID()

    val jpa = baseBookingEntity.copy(
      id = id,
      arrivalDate = LocalDate.of(2023, 12, 11),
      departureDate = LocalDate.of(2024, 11, 29),
    )

    val result = bookingTransformer.transformToWithdrawable(jpa)

    assertThat(result).isEqualTo(
      Withdrawable(
        id,
        WithdrawableType.booking,
        listOf(
          DatePeriod(
            LocalDate.of(2023, 12, 11),
            LocalDate.of(2024, 11, 29),
          ),
        ),
      ),
    )
  }
}
