package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.unit.transformer

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.CancellationReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DepartureReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.FullPerson
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MoveOnCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NonArrivalReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BedspaceCharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.factory.Cas3BedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3DepartureEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3NonArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.v2.Cas3v2ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.v2.Cas3v2TurnaroundEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3BedspaceSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.Cas3PremisesStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Cancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Confirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Departure
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3NonArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.model.generated.Cas3Turnaround
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3ArrivalTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3BedspaceTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3CancellationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3ConfirmationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3DepartureTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3ExtensionTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3NonArrivalTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.transformer.Cas3TurnaroundTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.InmateDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OffenderDetailsSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationDeliveryUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DestinationProviderEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@SuppressWarnings("LargeClass")
class Cas3BookingTransformerTest {
  private val mockPersonTransformer = mockk<PersonTransformer>()
  private val mockArrivalTransformer = mockk<Cas3ArrivalTransformer>()
  private val mockNonArrivalTransformer = mockk<Cas3NonArrivalTransformer>()
  private val mockCancellationTransformer = mockk<Cas3CancellationTransformer>()
  private val mockCas3ConfirmationTransformer = mockk<Cas3ConfirmationTransformer>()
  private val mockDepartureTransformer = mockk<Cas3DepartureTransformer>()
  private val mockExtensionTransformer = mockk<Cas3ExtensionTransformer>()
  private val mockBedspaceTransformer = mockk<Cas3BedspaceTransformer>()
  private val mockCas3TurnaroundTransformer = mockk<Cas3TurnaroundTransformer>()
  private val mockWorkingDayService = mockk<WorkingDayService>()

  private val bookingTransformer = Cas3BookingTransformer(
    mockPersonTransformer,
    mockArrivalTransformer,
    mockDepartureTransformer,
    mockNonArrivalTransformer,
    mockCancellationTransformer,
    mockCas3ConfirmationTransformer,
    mockExtensionTransformer,
    mockBedspaceTransformer,
    mockCas3TurnaroundTransformer,
    mockWorkingDayService,
  )

  private val premisesEntity = Cas3PremisesEntity(
    id = UUID.fromString("9703eaaf-164f-4f35-b038-f4de79e4847b"),
    name = "AP",
    localAuthorityArea = LocalAuthorityAreaEntity(
      id = UUID.fromString("ee39d3bc-e9ad-4408-a21d-cf763aa1d981"),
      identifier = "AUTHORITY",
      name = "Local Authority Area",
      premises = mutableListOf(),
    ),
    bookings = mutableListOf(),
    addressLine1 = "1 somewhere",
    addressLine2 = "Some district",
    town = "Somewhere",
    postcode = "ST8ST8",
    notes = "",
    characteristics = mutableListOf(),
    status = Cas3PremisesStatus.online,
    probationDeliveryUnit = ProbationDeliveryUnitEntityFactory().withProbationRegion(
      ProbationRegionEntity(
        id = UUID.fromString("4eae0059-af28-4436-a4d8-7106523866d9"),
        name = "region",
        deliusCode = "ABC",
        apArea = null,
      ),
    ).produce(),
    bedspaces = mutableListOf(),
    turnaroundWorkingDays = 3,
    startDate = LocalDate.now().minusDays(100),
    endDate = null,
    createdAt = OffsetDateTime.now(),
  )

  private val bedspaceCharacteristic = Cas3BedspaceCharacteristicEntityFactory().produce()
  private val bedspaceEntity = Cas3BedspaceEntityFactory()
    .withPremises(premisesEntity)
    .withCharacteristics(mutableListOf(bedspaceCharacteristic))
    .produce()

  private val baseBookingEntity = Cas3BookingEntity(
    id = UUID.fromString("c0cffa2a-490a-4e8b-a970-80aea3922a18"),
    arrivalDate = LocalDate.parse("2022-08-10"),
    departureDate = LocalDate.parse("2022-08-30"),
    crn = "CRN123",
    arrivals = mutableListOf(),
    departures = mutableListOf(),
    nonArrival = null,
    cancellations = mutableListOf(),
    confirmation = null,
    extensions = mutableListOf(),
    dateChanges = mutableListOf(),
    premises = premisesEntity,
    bedspace = bedspaceEntity,
    service = ServiceName.temporaryAccommodation.value,
    originalArrivalDate = LocalDate.parse("2022-08-10"),
    originalDepartureDate = LocalDate.parse("2022-08-30"),
    createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
    application = null,
    turnarounds = mutableListOf(),
    nomsNumber = "NOMS123",
    status = null,
    offenderName = null,
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

  private val nullDepartureEntity: Cas3DepartureEntity? = null
  private val nullConfirmationEntity: Cas3v2ConfirmationEntity? = null

  private val bedspaceSummaryModel = Cas3BedspaceSummary(
    id = bedspaceEntity.id,
    reference = bedspaceEntity.reference,
    endDate = bedspaceEntity.endDate,
  )

  init {
    every { mockArrivalTransformer.transformJpaToApi(null) } returns null
    every { mockNonArrivalTransformer.transformJpaToApi(null) } returns null
    every { mockCancellationTransformer.transformJpaToApi(null) } returns null
    every { mockCas3ConfirmationTransformer.transformJpaToApi(nullConfirmationEntity) } returns null
    every { mockBedspaceTransformer.transformJpaToCas3BedspaceSummary(bedspaceEntity) } returns bedspaceSummaryModel
    every { mockDepartureTransformer.transformJpaToApi(nullDepartureEntity) } returns null

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
  fun `Premises Provisional entity is correctly transformed`() {
    val awaitingArrivalBooking = baseBookingEntity.copy(
      id = UUID.fromString("5bbe785f-5ff3-46b9-b9fe-d9e6ca7a18e8"),
      service = ServiceName.temporaryAccommodation.value,
    )

    val transformedBooking = bookingTransformer.transformJpaToApi(
      awaitingArrivalBooking,
      PersonInfoResult.Success.Full(offenderDetails.otherIds.crn, offenderDetails, inmateDetail),
    )

    assertThat(transformedBooking).isEqualTo(
      Cas3Booking(
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
        status = Cas3BookingStatus.provisional,
        extensions = listOf(),
        originalArrivalDate = LocalDate.parse("2022-08-10"),
        originalDepartureDate = LocalDate.parse("2022-08-30"),
        createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        departures = listOf(),
        cancellations = listOf(),
        turnarounds = listOf(),
        effectiveEndDate = LocalDate.parse("2022-08-30"),
        premises = Cas3BookingPremisesSummary(premisesEntity.id, premisesEntity.name),
        bedspace = bedspaceSummaryModel,
      ),
    )
  }

  @Test
  fun `Premises Non Arrival entity is correctly transformed`() {
    val nonArrivalBooking = baseBookingEntity.copy(id = UUID.fromString("655f72ba-51eb-4965-b6ac-45bcc6271b19")).apply {
      nonArrival = Cas3NonArrivalEntity(
        id = UUID.fromString("77e66712-b0a0-4968-b284-77ac1babe09c"),
        date = LocalDate.parse("2022-08-10"),
        reason = NonArrivalReasonEntity(id = UUID.fromString("7a87f93d-b9d6-423d-a87a-dfc693ab82f9"), name = "Unknown", isActive = true, legacyDeliusReasonCode = "A"),
        notes = null,
        booking = this,
        createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
      )
    }

    every { mockNonArrivalTransformer.transformJpaToApi(nonArrivalBooking.nonArrival) } returns Cas3NonArrival(
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
      Cas3Booking(
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
        status = Cas3BookingStatus.notMinusArrived,
        nonArrival = Cas3NonArrival(
          id = UUID.fromString("77e66712-b0a0-4968-b284-77ac1babe09c"),
          bookingId = UUID.fromString("655f72ba-51eb-4965-b6ac-45bcc6271b19"),
          date = LocalDate.parse("2022-08-10"),
          reason = NonArrivalReason(id = UUID.fromString("7a87f93d-b9d6-423d-a87a-dfc693ab82f9"), name = "Unknown", isActive = true),
          notes = null,
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        ),
        extensions = listOf(),
        originalArrivalDate = LocalDate.parse("2022-08-10"),
        originalDepartureDate = LocalDate.parse("2022-08-30"),
        createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        departures = listOf(),
        cancellations = listOf(),
        turnarounds = listOf(),
        effectiveEndDate = LocalDate.parse("2022-08-30"),
        premises = Cas3BookingPremisesSummary(premisesEntity.id, premisesEntity.name),
        bedspace = bedspaceSummaryModel,
      ),
    )
  }

  @Test
  fun `Premises Arrived entity is correctly transformed`() {
    val arrivalBooking = baseBookingEntity.copy(id = UUID.fromString("443e79a9-b10a-4ad7-8be1-ffe301d2bbf3")).apply {
      arrivals += Cas3ArrivalEntity(
        id = UUID.fromString("77e66712-b0a0-4968-b284-77ac1babe09c"),
        arrivalDate = LocalDate.parse("2022-08-10"),
        arrivalDateTime = Instant.parse("2022-08-10T00:00:00Z"),
        expectedDepartureDate = LocalDate.parse("2022-08-16"),
        notes = null,
        booking = this,
        createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
      )
    }

    every { mockArrivalTransformer.transformJpaToApi(arrivalBooking.arrival) } returns Cas3Arrival(
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
      Cas3Booking(
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
        status = Cas3BookingStatus.arrived,
        arrival = Cas3Arrival(
          bookingId = UUID.fromString("443e79a9-b10a-4ad7-8be1-ffe301d2bbf3"),
          arrivalDate = LocalDate.parse("2022-08-10"),
          arrivalTime = "00:00:00",
          expectedDepartureDate = LocalDate.parse("2022-08-16"),
          notes = null,
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        ),
        extensions = listOf(),
        originalArrivalDate = LocalDate.parse("2022-08-10"),
        originalDepartureDate = LocalDate.parse("2022-08-30"),
        createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        departures = listOf(),
        cancellations = listOf(),
        turnarounds = listOf(),
        effectiveEndDate = LocalDate.parse("2022-08-30"),
        premises = Cas3BookingPremisesSummary(premisesEntity.id, premisesEntity.name),
        bedspace = bedspaceSummaryModel,
      ),
    )
  }

  @Test
  fun `Premises Cancelled entity is correctly transformed`() {
    val cancellationBooking = baseBookingEntity.copy(id = UUID.fromString("d182c0b8-1f5f-433b-9a0e-b0e51fee8b8d")).apply {
      cancellations = mutableListOf(
        Cas3CancellationEntity(
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

    every { mockCancellationTransformer.transformJpaToApi(cancellationBooking.cancellation) } returns Cas3Cancellation(
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
      Cas3Booking(
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
        status = Cas3BookingStatus.cancelled,
        cancellation = Cas3Cancellation(
          bookingId = UUID.fromString("d182c0b8-1f5f-433b-9a0e-b0e51fee8b8d"),
          date = LocalDate.parse("2022-08-10"),
          reason = CancellationReason(id = UUID.fromString("aa4ee8cf-3580-44e1-a3e1-6f3ee7d5ec67"), name = "Because", isActive = true, serviceScope = "approved-premises"),
          notes = null,
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
          premisesName = premisesEntity.name,
        ),
        extensions = listOf(),
        originalArrivalDate = LocalDate.parse("2022-08-10"),
        originalDepartureDate = LocalDate.parse("2022-08-30"),
        createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        departures = listOf(),
        cancellations = listOf(
          Cas3Cancellation(
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
        premises = Cas3BookingPremisesSummary(premisesEntity.id, premisesEntity.name),
        bedspace = bedspaceSummaryModel,
      ),
    )
  }

  @Test
  fun `Premises with edited cancellation is correctly transformed`() {
    val cancellationBooking = baseBookingEntity.copy(id = UUID.fromString("d182c0b8-1f5f-433b-9a0e-b0e51fee8b8d")).apply {
      service = ServiceName.temporaryAccommodation.value
      cancellations = mutableListOf(
        Cas3CancellationEntity(
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
        Cas3CancellationEntity(
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
      val arg = answer.invocation.args[0] as Cas3CancellationEntity

      when (arg.id) {
        UUID.fromString("77e66712-b0a0-4968-b284-77ac1babe09c") -> Cas3Cancellation(
          bookingId = UUID.fromString("d182c0b8-1f5f-433b-9a0e-b0e51fee8b8d"),
          notes = null,
          date = LocalDate.parse("2022-08-10"),
          reason = CancellationReason(id = UUID.fromString("aa4ee8cf-3580-44e1-a3e1-6f3ee7d5ec67"), name = "Because", isActive = true, serviceScope = ServiceName.temporaryAccommodation.value),
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
          premisesName = premisesEntity.name,
        )
        UUID.fromString("d34415c3-d128-45a0-9950-b84491ab8d11") -> Cas3Cancellation(
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
      Cas3Booking(
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
        status = Cas3BookingStatus.cancelled,
        cancellation = Cas3Cancellation(
          bookingId = UUID.fromString("d182c0b8-1f5f-433b-9a0e-b0e51fee8b8d"),
          notes = null,
          date = LocalDate.parse("2022-08-10"),
          reason = CancellationReason(id = UUID.fromString("dd6444f7-af56-436c-8451-ca993617471e"), name = "Some other reason", isActive = true, serviceScope = ServiceName.temporaryAccommodation.value),
          createdAt = Instant.parse("2022-07-02T12:34:56.789Z"),
          premisesName = premisesEntity.name,
        ),
        extensions = listOf(),
        originalArrivalDate = LocalDate.parse("2022-08-10"),
        originalDepartureDate = LocalDate.parse("2022-08-30"),
        createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        departures = listOf(),
        cancellations = listOf(
          Cas3Cancellation(
            bookingId = UUID.fromString("d182c0b8-1f5f-433b-9a0e-b0e51fee8b8d"),
            notes = null,
            date = LocalDate.parse("2022-08-10"),
            reason = CancellationReason(id = UUID.fromString("aa4ee8cf-3580-44e1-a3e1-6f3ee7d5ec67"), name = "Because", isActive = true, serviceScope = ServiceName.temporaryAccommodation.value),
            createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
            premisesName = premisesEntity.name,
          ),
          Cas3Cancellation(
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
        premises = Cas3BookingPremisesSummary(premisesEntity.id, premisesEntity.name),
        bedspace = bedspaceSummaryModel,
      ),
    )
  }

  @Test
  fun `Premises entity with zero day turnaround period and departure is correctly transformed to closed status`() {
    val bookingId = UUID.fromString("e0a3f9d7-0677-40bf-85a9-6673a7af33ee")
    val departedBooking = baseBookingEntity.copy(id = bookingId, service = ServiceName.temporaryAccommodation.value).apply {
      arrivals += Cas3ArrivalEntity(
        id = UUID.fromString("77e66712-b0a0-4968-b284-77ac1babe09c"),
        arrivalDate = LocalDate.parse("2022-08-10"),
        arrivalDateTime = Instant.parse("2022-08-10T00:00:00Z"),
        expectedDepartureDate = LocalDate.parse("2022-08-16"),
        notes = null,
        booking = this,
        createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
      )
      departures = mutableListOf(
        Cas3DepartureEntity(
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

      turnarounds += Cas3v2TurnaroundEntity(
        id = UUID.fromString("8c87e15d-f236-479e-b9fd-f4c5cc6bef8f"),
        workingDayCount = 0,
        createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
        booking = this,
      )
    }

    every { mockArrivalTransformer.transformJpaToApi(departedBooking.arrival) } returns Cas3Arrival(
      bookingId = bookingId,
      arrivalDate = LocalDate.parse("2022-08-10"),
      expectedDepartureDate = LocalDate.parse("2022-08-16"),
      notes = null,
      arrivalTime = "00:00:00",
      createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
    )

    every { mockDepartureTransformer.transformJpaToApi(departedBooking.departure) } returns Cas3Departure(
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
      notes = null,
      createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
    )

    every { mockCas3TurnaroundTransformer.transformJpaToApi(departedBooking.turnaround!!) } returns Cas3Turnaround(
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
      Cas3Booking(
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
        status = Cas3BookingStatus.closed,
        arrival = Cas3Arrival(
          bookingId = bookingId,
          arrivalDate = LocalDate.parse("2022-08-10"),
          expectedDepartureDate = LocalDate.parse("2022-08-16"),
          notes = null,
          arrivalTime = "00:00:00",
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        ),
        departure = Cas3Departure(
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
          notes = null,
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        ),
        extensions = listOf(),
        originalArrivalDate = LocalDate.parse("2022-08-10"),
        originalDepartureDate = LocalDate.parse("2022-08-30"),
        createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        departures = listOf(
          Cas3Departure(
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
            notes = null,
            createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
          ),
        ),
        cancellations = listOf(),
        turnaround = Cas3Turnaround(
          id = UUID.fromString("8c87e15d-f236-479e-b9fd-f4c5cc6bef8f"),
          workingDays = 0,
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
          bookingId = bookingId,
        ),
        turnarounds = listOf(
          Cas3Turnaround(
            id = UUID.fromString("8c87e15d-f236-479e-b9fd-f4c5cc6bef8f"),
            workingDays = 0,
            createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
            bookingId = bookingId,
          ),
        ),
        effectiveEndDate = LocalDate.parse("2022-08-30"),
        premises = Cas3BookingPremisesSummary(premisesEntity.id, premisesEntity.name),
        bedspace = bedspaceSummaryModel,
      ),
    )
  }

  @SuppressWarnings("LongMethod")
  @Test
  fun `Premises entity with non-zero day turnaround period and departure within turnaround period is correctly transformed to departed status`() {
    mockkStatic(OffsetDateTime::class, LocalDate::class)

    val timeNow = OffsetDateTime.parse("2022-08-17T15:30:00Z")
    val departedAt = OffsetDateTime.parse("2022-08-16T12:30:00Z")
    val expectedEffectiveEndDate = LocalDate.parse("2022-08-18")
    every { OffsetDateTime.now() } returns timeNow
    every { LocalDate.now() } returns timeNow.toLocalDate()

    val bookingId = UUID.fromString("e0a3f9d7-0677-40bf-85a9-6673a7af33ee")
    val departedBooking = baseBookingEntity.copy(id = bookingId, service = ServiceName.temporaryAccommodation.value).apply {
      departureDate = departedAt.toLocalDate()

      arrivals += Cas3ArrivalEntity(
        id = UUID.fromString("77e66712-b0a0-4968-b284-77ac1babe09c"),
        arrivalDate = LocalDate.parse("2022-08-10"),
        arrivalDateTime = Instant.parse("2022-08-10T00:00:00Z"),
        expectedDepartureDate = departedAt.toLocalDate(),
        notes = null,
        booking = this,
        createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
      )
      departures = mutableListOf(
        Cas3DepartureEntity(
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

      turnarounds += Cas3v2TurnaroundEntity(
        id = UUID.fromString("8c87e15d-f236-479e-b9fd-f4c5cc6bef8f"),
        workingDayCount = 2,
        createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
        booking = this,
      )
    }

    every { mockArrivalTransformer.transformJpaToApi(departedBooking.arrival) } returns Cas3Arrival(
      bookingId = bookingId,
      arrivalDate = LocalDate.parse("2022-08-10"),
      expectedDepartureDate = LocalDate.parse("2022-08-16"),
      notes = null,
      arrivalTime = "00:00:00",
      createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
    )

    every { mockDepartureTransformer.transformJpaToApi(departedBooking.departure) } returns Cas3Departure(
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
      notes = null,
      createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
    )

    every { mockCas3TurnaroundTransformer.transformJpaToApi(departedBooking.turnaround!!) } returns Cas3Turnaround(
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
      Cas3Booking(
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
        status = Cas3BookingStatus.departed,
        arrival = Cas3Arrival(
          bookingId = bookingId,
          arrivalDate = LocalDate.parse("2022-08-10"),
          expectedDepartureDate = LocalDate.parse("2022-08-16"),
          notes = null,
          arrivalTime = "00:00:00",
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        ),
        departure = Cas3Departure(
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
          notes = null,
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        ),
        extensions = listOf(),
        originalArrivalDate = LocalDate.parse("2022-08-10"),
        originalDepartureDate = LocalDate.parse("2022-08-30"),
        createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        departures = listOf(
          Cas3Departure(
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
            notes = null,
            createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
          ),
        ),
        cancellations = listOf(),
        turnaround = Cas3Turnaround(
          id = UUID.fromString("8c87e15d-f236-479e-b9fd-f4c5cc6bef8f"),
          workingDays = 2,
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
          bookingId = bookingId,
        ),
        turnarounds = listOf(
          Cas3Turnaround(
            id = UUID.fromString("8c87e15d-f236-479e-b9fd-f4c5cc6bef8f"),
            workingDays = 2,
            createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
            bookingId = bookingId,
          ),
        ),
        turnaroundStartDate = departedAt.toLocalDate().plusDays(1),
        effectiveEndDate = expectedEffectiveEndDate,
        premises = Cas3BookingPremisesSummary(premisesEntity.id, premisesEntity.name),
        bedspace = bedspaceSummaryModel,
      ),
    )
  }

  @SuppressWarnings("LongMethod")
  @Test
  fun `Premises entity with non-zero day turnaround period and departure with turnaround period in past is correctly transformed to closed status`() {
    mockkStatic(OffsetDateTime::class, LocalDate::class)

    val timeNow = OffsetDateTime.parse("2022-08-19T15:30:00Z")
    val departedAt = OffsetDateTime.parse("2022-08-16T12:30:00Z")
    val expectedEffectiveEndDate = LocalDate.parse("2022-08-18")
    every { OffsetDateTime.now() } returns timeNow
    every { LocalDate.now() } returns timeNow.toLocalDate()

    val bookingId = UUID.fromString("e0a3f9d7-0677-40bf-85a9-6673a7af33ee")
    val departedBooking = baseBookingEntity.copy(id = bookingId, service = ServiceName.temporaryAccommodation.value).apply {
      departureDate = departedAt.toLocalDate()

      arrivals += Cas3ArrivalEntity(
        id = UUID.fromString("77e66712-b0a0-4968-b284-77ac1babe09c"),
        arrivalDate = LocalDate.parse("2022-08-10"),
        arrivalDateTime = Instant.parse("2022-08-10T00:00:00Z"),
        expectedDepartureDate = departedAt.toLocalDate(),
        notes = null,
        booking = this,
        createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
      )
      departures = mutableListOf(
        Cas3DepartureEntity(
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

      turnarounds += Cas3v2TurnaroundEntity(
        id = UUID.fromString("8c87e15d-f236-479e-b9fd-f4c5cc6bef8f"),
        workingDayCount = 2,
        createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
        booking = this,
      )
    }

    every { mockArrivalTransformer.transformJpaToApi(departedBooking.arrival) } returns Cas3Arrival(
      bookingId = bookingId,
      arrivalDate = LocalDate.parse("2022-08-10"),
      expectedDepartureDate = LocalDate.parse("2022-08-16"),
      notes = null,
      arrivalTime = "00:00:00",
      createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
    )

    every { mockDepartureTransformer.transformJpaToApi(departedBooking.departure) } returns Cas3Departure(
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
      notes = null,
      createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
    )

    every { mockCas3TurnaroundTransformer.transformJpaToApi(departedBooking.turnaround!!) } returns Cas3Turnaround(
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
      Cas3Booking(
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
        status = Cas3BookingStatus.closed,
        arrival = Cas3Arrival(
          bookingId = bookingId,
          arrivalDate = LocalDate.parse("2022-08-10"),
          expectedDepartureDate = LocalDate.parse("2022-08-16"),
          notes = null,
          arrivalTime = "00:00:00",
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        ),
        departure = Cas3Departure(
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
          notes = null,
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        ),
        extensions = listOf(),
        originalArrivalDate = LocalDate.parse("2022-08-10"),
        originalDepartureDate = LocalDate.parse("2022-08-30"),
        createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        departures = listOf(
          Cas3Departure(
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
            notes = null,
            createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
          ),
        ),
        cancellations = listOf(),
        turnaround = Cas3Turnaround(
          id = UUID.fromString("8c87e15d-f236-479e-b9fd-f4c5cc6bef8f"),
          workingDays = 2,
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
          bookingId = bookingId,
        ),
        turnarounds = listOf(
          Cas3Turnaround(
            id = UUID.fromString("8c87e15d-f236-479e-b9fd-f4c5cc6bef8f"),
            workingDays = 2,
            createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
            bookingId = bookingId,
          ),
        ),
        turnaroundStartDate = departedAt.toLocalDate().plusDays(1),
        effectiveEndDate = expectedEffectiveEndDate,
        premises = Cas3BookingPremisesSummary(premisesEntity.id, premisesEntity.name),
        bedspace = bedspaceSummaryModel,
      ),
    )
  }

  @SuppressWarnings("LongMethod")
  @Test
  fun `Premises Entity with edited departure is correctly transformed`() {
    val bookingId = UUID.fromString("e0a3f9d7-0677-40bf-85a9-6673a7af33ee")
    val departedBooking = baseBookingEntity.copy(id = bookingId).apply {
      service = ServiceName.temporaryAccommodation.value
      arrivals += Cas3ArrivalEntity(
        id = UUID.fromString("77e66712-b0a0-4968-b284-77ac1babe09c"),
        arrivalDate = LocalDate.parse("2022-08-10"),
        arrivalDateTime = Instant.parse("2022-08-10T00:00:00Z"),
        expectedDepartureDate = LocalDate.parse("2022-08-16"),
        notes = null,
        booking = this,
        createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
      )
      departures = mutableListOf(
        Cas3DepartureEntity(
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
        Cas3DepartureEntity(
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

    every { mockArrivalTransformer.transformJpaToApi(departedBooking.arrival) } returns Cas3Arrival(
      bookingId = bookingId,
      arrivalDate = LocalDate.parse("2022-08-10"),
      expectedDepartureDate = LocalDate.parse("2022-08-16"),
      notes = null,
      arrivalTime = "00:00:00",
      createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
    )

    every { mockDepartureTransformer.transformJpaToApi(any(Cas3DepartureEntity::class)) } answers { answer ->
      val arg = answer.invocation.args[0] as Cas3DepartureEntity

      when (arg.id) {
        UUID.fromString("0d68d5b9-44c7-46cd-a52b-8185477b5edd") -> Cas3Departure(
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
          notes = null,
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        )
        UUID.fromString("f184e3e9-474d-4083-9f3d-628f462907fc") -> Cas3Departure(
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
      Cas3Booking(
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
        status = Cas3BookingStatus.closed,
        arrival = Cas3Arrival(
          bookingId = bookingId,
          arrivalDate = LocalDate.parse("2022-08-10"),
          expectedDepartureDate = LocalDate.parse("2022-08-16"),
          notes = null,
          arrivalTime = "00:00:00",
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        ),
        departure = Cas3Departure(
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
          notes = "Updated move-on category and destination provider after receiving new information",
          createdAt = Instant.parse("2022-07-02T12:34:56.789Z"),
        ),
        extensions = listOf(),
        originalArrivalDate = LocalDate.parse("2022-08-10"),
        originalDepartureDate = LocalDate.parse("2022-08-30"),
        createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        departures = listOf(
          Cas3Departure(
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
            notes = null,
            createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
          ),
          Cas3Departure(
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
            notes = "Updated move-on category and destination provider after receiving new information",
            createdAt = Instant.parse("2022-07-02T12:34:56.789Z"),
          ),
        ),
        cancellations = listOf(),
        turnarounds = listOf(),
        effectiveEndDate = LocalDate.parse("2022-08-30"),
        premises = Cas3BookingPremisesSummary(premisesEntity.id, premisesEntity.name),
        bedspace = bedspaceSummaryModel,
      ),
    )
  }

  @Test
  fun `Premises Confirmed entity is correctly transformed`() {
    val confirmationBooking = baseBookingEntity.copy(
      id = UUID.fromString("1c29a729-6059-4939-8641-1caa61a38815"),
      service = ServiceName.temporaryAccommodation.value,
    ).apply {
      confirmation = Cas3v2ConfirmationEntity(
        id = UUID.fromString("69fc6350-b2ec-4e99-9a2f-e829e83535e8"),
        dateTime = OffsetDateTime.parse("2022-11-23T12:34:56.789Z"),
        notes = null,
        booking = this,
        createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
      )
    }

    every { mockCas3ConfirmationTransformer.transformJpaToApi(confirmationBooking.confirmation) } returns Cas3Confirmation(
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
      Cas3Booking(
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
        status = Cas3BookingStatus.confirmed,
        cancellation = null,
        confirmation = Cas3Confirmation(
          id = UUID.fromString("69fc6350-b2ec-4e99-9a2f-e829e83535e8"),
          bookingId = UUID.fromString("1c29a729-6059-4939-8641-1caa61a38815"),
          notes = null,
          dateTime = Instant.parse("2022-11-23T12:34:56.789Z"),
          createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        ),
        extensions = listOf(),
        originalArrivalDate = LocalDate.parse("2022-08-10"),
        originalDepartureDate = LocalDate.parse("2022-08-30"),
        createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        departures = listOf(),
        cancellations = listOf(),
        turnarounds = listOf(),
        effectiveEndDate = LocalDate.parse("2022-08-30"),
        premises = Cas3BookingPremisesSummary(premisesEntity.id, premisesEntity.name),
        bedspace = bedspaceSummaryModel,
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

    val turnaround1 = Cas3v2TurnaroundEntity(
      id = UUID.fromString("34ae3124-cf7a-47d5-86c1-ef9ab4255e30"),
      workingDayCount = 2,
      createdAt = OffsetDateTime.parse("2022-07-01T12:34:56.789Z"),
      booking = awaitingArrivalBooking,
    )
    val turnaround2 = Cas3v2TurnaroundEntity(
      id = UUID.fromString("b2af671a-997d-443e-906d-3a1a28c71416"),
      workingDayCount = 3,
      createdAt = OffsetDateTime.parse("2022-07-02T10:11:12.345Z"),
      booking = awaitingArrivalBooking,
    )
    val turnaround3 = Cas3v2TurnaroundEntity(
      id = UUID.fromString("146d05f8-ba83-42ae-a6d7-807a16b7946d"),
      workingDayCount = 4,
      createdAt = OffsetDateTime.parse("2022-07-03T09:08:07.654Z"),
      booking = awaitingArrivalBooking,
    )

    awaitingArrivalBooking.turnarounds += listOf(turnaround1, turnaround2, turnaround3)

    every { mockCas3TurnaroundTransformer.transformJpaToApi(turnaround1) } returns Cas3Turnaround(
      id = UUID.fromString("34ae3124-cf7a-47d5-86c1-ef9ab4255e30"),
      bookingId = UUID.fromString("5bbe785f-5ff3-46b9-b9fe-d9e6ca7a18e8"),
      workingDays = 2,
      createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
    )

    every { mockCas3TurnaroundTransformer.transformJpaToApi(turnaround2) } returns Cas3Turnaround(
      id = UUID.fromString("b2af671a-997d-443e-906d-3a1a28c71416"),
      bookingId = UUID.fromString("5bbe785f-5ff3-46b9-b9fe-d9e6ca7a18e8"),
      workingDays = 3,
      createdAt = Instant.parse("2022-07-02T10:11:12.345Z"),
    )

    every { mockCas3TurnaroundTransformer.transformJpaToApi(turnaround3) } returns Cas3Turnaround(
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
      Cas3Booking(
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
        status = Cas3BookingStatus.provisional,
        extensions = listOf(),
        originalArrivalDate = LocalDate.parse("2022-08-10"),
        originalDepartureDate = LocalDate.parse("2022-08-30"),
        createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
        departures = listOf(),
        cancellations = listOf(),
        turnaround = Cas3Turnaround(
          id = UUID.fromString("146d05f8-ba83-42ae-a6d7-807a16b7946d"),
          bookingId = UUID.fromString("5bbe785f-5ff3-46b9-b9fe-d9e6ca7a18e8"),
          workingDays = 4,
          createdAt = Instant.parse("2022-07-03T09:08:07.654Z"),
        ),
        turnarounds = listOf(
          Cas3Turnaround(
            id = UUID.fromString("34ae3124-cf7a-47d5-86c1-ef9ab4255e30"),
            bookingId = UUID.fromString("5bbe785f-5ff3-46b9-b9fe-d9e6ca7a18e8"),
            workingDays = 2,
            createdAt = Instant.parse("2022-07-01T12:34:56.789Z"),
          ),
          Cas3Turnaround(
            id = UUID.fromString("b2af671a-997d-443e-906d-3a1a28c71416"),
            bookingId = UUID.fromString("5bbe785f-5ff3-46b9-b9fe-d9e6ca7a18e8"),
            workingDays = 3,
            createdAt = Instant.parse("2022-07-02T10:11:12.345Z"),
          ),
          Cas3Turnaround(
            id = UUID.fromString("146d05f8-ba83-42ae-a6d7-807a16b7946d"),
            bookingId = UUID.fromString("5bbe785f-5ff3-46b9-b9fe-d9e6ca7a18e8"),
            workingDays = 4,
            createdAt = Instant.parse("2022-07-03T09:08:07.654Z"),
          ),
        ),
        turnaroundStartDate = LocalDate.parse("2022-08-31"),
        effectiveEndDate = LocalDate.parse("2022-09-05"),
        premises = Cas3BookingPremisesSummary(premisesEntity.id, premisesEntity.name),
        bedspace = bedspaceSummaryModel,
      ),
    )
  }
}
