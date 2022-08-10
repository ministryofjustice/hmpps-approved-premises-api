package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.Cancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.Departure
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.DepartureReason
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.DestinationProvider
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.MoveOnCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.Nonarrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DestinationProviderEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ArrivalTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.CancellationTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DepartureTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NonArrivalTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class BookingTransformerTest {
  private val mockPersonTransformer = mockk<PersonTransformer>()
  private val mockArrivalTransformer = mockk<ArrivalTransformer>()
  private val mockNonArrivalTransformer = mockk<NonArrivalTransformer>()
  private val mockCancellationTransformer = mockk<CancellationTransformer>()
  private val mockDepartureTransformer = mockk<DepartureTransformer>()

  private val bookingTransformer = BookingTransformer(
    mockPersonTransformer,
    mockArrivalTransformer,
    mockDepartureTransformer,
    mockNonArrivalTransformer,
    mockCancellationTransformer
  )

  private val premisesEntity = PremisesEntity(
    id = UUID.fromString("9703eaaf-164f-4f35-b038-f4de79e4847b"),
    name = "AP", apCode = "APCODE", postcode = "ST8ST8", totalBeds = 50,
    probationRegion = ProbationRegionEntity(
      id = UUID.fromString("4eae0059-af28-4436-a4d8-7106523866d9"),
      identifier = "PR",
      name = "region",
      premises = mutableListOf()
    ),
    apArea = ApAreaEntity(
      id = UUID.fromString("a005f122-a0e9-4d93-b5bb-f7c5bd82a015"),
      name = "Ap Area",
      premises = mutableListOf()
    ),
    localAuthorityArea = LocalAuthorityAreaEntity(
      id = UUID.fromString("ee39d3bc-e9ad-4408-a21d-cf763aa1d981"),
      identifier = "AUTHORITY",
      name = "Local Authority Area",
      premises = mutableListOf()
    ),
    bookings = mutableListOf()
  )

  private val baseBookingEntity = BookingEntity(
    id = UUID.fromString("c0cffa2a-490a-4e8b-a970-80aea3922a18"),
    arrivalDate = LocalDate.parse("2022-08-10"),
    departureDate = LocalDate.parse("2022-08-30"),
    keyWorker = "Key Worker",
    person = null,
    arrival = null,
    departure = null,
    nonArrival = null,
    cancellation = null,
    premises = premisesEntity
  ).apply {
    person = PersonEntity(
      id = UUID.fromString("fff25958-350a-4bde-844e-03600e286f8c"),
      crn = "crn",
      name = "name",
      this
    )
  }

  init {
    every { mockArrivalTransformer.transformJpaToApi(null) } returns null
    every { mockNonArrivalTransformer.transformJpaToApi(null) } returns null
    every { mockCancellationTransformer.transformJpaToApi(null) } returns null
    every { mockDepartureTransformer.transformJpaToApi(null) } returns null

    every { mockPersonTransformer.transformJpaToApi(baseBookingEntity.person!!) } returns Person(
      crn = "crn",
      name = "name"
    )
  }

  @Test
  fun `Awaiting Arrival entity is correctly transformed`() {
    val awaitingArrivalBooking = baseBookingEntity.copy(id = UUID.fromString("5bbe785f-5ff3-46b9-b9fe-d9e6ca7a18e8"))

    val transformedBooking = bookingTransformer.transformJpaToApi(awaitingArrivalBooking)

    assertThat(transformedBooking).isEqualTo(
      Booking(
        id = UUID.fromString("5bbe785f-5ff3-46b9-b9fe-d9e6ca7a18e8"),
        person = Person(crn = "crn", name = "name"),
        arrivalDate = LocalDate.parse("2022-08-10"),
        departureDate = LocalDate.parse("2022-08-30"),
        keyWorker = "Key Worker",
        status = Booking.Status.awaitingMinusArrival
      )
    )
  }

  @Test
  fun `Non Arrival entity is correctly transformed`() {
    val nonArrivalBooking = baseBookingEntity.copy(id = UUID.fromString("655f72ba-51eb-4965-b6ac-45bcc6271b19")).apply {
      nonArrival = NonArrivalEntity(
        id = UUID.fromString("77e66712-b0a0-4968-b284-77ac1babe09c"),
        date = LocalDate.parse("2022-08-10"),
        reason = "Unknown",
        notes = null,
        booking = this
      )
    }

    every { mockNonArrivalTransformer.transformJpaToApi(nonArrivalBooking.nonArrival) } returns Nonarrival(
      bookingId = UUID.fromString("655f72ba-51eb-4965-b6ac-45bcc6271b19"),
      date = LocalDate.parse("2022-08-10"),
      reason = "Unknown",
      notes = null
    )

    val transformedBooking = bookingTransformer.transformJpaToApi(nonArrivalBooking)

    assertThat(transformedBooking).isEqualTo(
      Booking(
        id = UUID.fromString("655f72ba-51eb-4965-b6ac-45bcc6271b19"),
        person = Person(crn = "crn", name = "name"),
        arrivalDate = LocalDate.parse("2022-08-10"),
        departureDate = LocalDate.parse("2022-08-30"),
        keyWorker = "Key Worker",
        status = Booking.Status.notMinusArrived,
        nonArrival = Nonarrival(
          bookingId = UUID.fromString("655f72ba-51eb-4965-b6ac-45bcc6271b19"),
          date = LocalDate.parse("2022-08-10"),
          reason = "Unknown",
          notes = null
        )
      )
    )
  }

  @Test
  fun `Arrived entity is correctly transformed`() {
    val arrivalBooking = baseBookingEntity.copy(id = UUID.fromString("443e79a9-b10a-4ad7-8be1-ffe301d2bbf3")).apply {
      arrival = ArrivalEntity(
        id = UUID.fromString("77e66712-b0a0-4968-b284-77ac1babe09c"),
        arrivalDate = LocalDate.parse("2022-08-10"),
        expectedDepartureDate = LocalDate.parse("2022-08-16"),
        notes = null,
        booking = this
      )
    }

    every { mockArrivalTransformer.transformJpaToApi(arrivalBooking.arrival) } returns Arrival(
      bookingId = UUID.fromString("443e79a9-b10a-4ad7-8be1-ffe301d2bbf3"),
      arrivalDate = LocalDate.parse("2022-08-10"),
      expectedDepartureDate = LocalDate.parse("2022-08-16"),
      notes = null
    )

    val transformedBooking = bookingTransformer.transformJpaToApi(arrivalBooking)

    assertThat(transformedBooking).isEqualTo(
      Booking(
        id = UUID.fromString("443e79a9-b10a-4ad7-8be1-ffe301d2bbf3"),
        person = Person(crn = "crn", name = "name"),
        arrivalDate = LocalDate.parse("2022-08-10"),
        departureDate = LocalDate.parse("2022-08-30"),
        keyWorker = "Key Worker",
        status = Booking.Status.arrived,
        arrival = Arrival(
          bookingId = UUID.fromString("443e79a9-b10a-4ad7-8be1-ffe301d2bbf3"),
          arrivalDate = LocalDate.parse("2022-08-10"),
          expectedDepartureDate = LocalDate.parse("2022-08-16"),
          notes = null
        )
      )
    )
  }

  @Test
  fun `Cancelled entity is correctly transformed`() {
    val cancellationBooking = baseBookingEntity.copy(id = UUID.fromString("d182c0b8-1f5f-433b-9a0e-b0e51fee8b8d")).apply {
      cancellation = CancellationEntity(
        id = UUID.fromString("77e66712-b0a0-4968-b284-77ac1babe09c"),
        date = LocalDate.parse("2022-08-10"),
        reason = "No longer required",
        notes = null,
        booking = this
      )
    }

    every { mockCancellationTransformer.transformJpaToApi(cancellationBooking.cancellation) } returns Cancellation(
      bookingId = UUID.fromString("d182c0b8-1f5f-433b-9a0e-b0e51fee8b8d"),
      notes = null,
      date = LocalDate.parse("2022-08-10"),
      reason = "No longer required"
    )

    val transformedBooking = bookingTransformer.transformJpaToApi(cancellationBooking)

    assertThat(transformedBooking).isEqualTo(
      Booking(
        id = UUID.fromString("d182c0b8-1f5f-433b-9a0e-b0e51fee8b8d"),
        person = Person(crn = "crn", name = "name"),
        arrivalDate = LocalDate.parse("2022-08-10"),
        departureDate = LocalDate.parse("2022-08-30"),
        keyWorker = "Key Worker",
        status = Booking.Status.cancelled,
        cancellation = Cancellation(
          bookingId = UUID.fromString("d182c0b8-1f5f-433b-9a0e-b0e51fee8b8d"),
          date = LocalDate.parse("2022-08-10"),
          reason = "No longer required",
          notes = null
        )
      )
    )
  }

  @Test
  fun `Departed entity is correctly transformed`() {
    val bookingId = UUID.fromString("e0a3f9d7-0677-40bf-85a9-6673a7af33ee")
    val departedBooking = baseBookingEntity.copy(id = bookingId).apply {
      arrival = ArrivalEntity(
        id = UUID.fromString("77e66712-b0a0-4968-b284-77ac1babe09c"),
        arrivalDate = LocalDate.parse("2022-08-10"),
        expectedDepartureDate = LocalDate.parse("2022-08-16"),
        notes = null,
        booking = this
      )
      departure = DepartureEntity(
        id = UUID.fromString("0d68d5b9-44c7-46cd-a52b-8185477b5edd"),
        dateTime = OffsetDateTime.parse("2022-08-30T15:30:15+01:00"),
        reason = DepartureReasonEntity(
          id = UUID.fromString("09e74ead-cf5a-40a1-a1be-739d3b97788f"),
          name = "Departure Reason",
          isActive = true
        ),
        moveOnCategory = MoveOnCategoryEntity(
          id = UUID.fromString("bcfbb1b6-f89d-45eb-ae70-308cc6930633"),
          name = "Move on Category",
          isActive = true
        ),
        destinationProvider = DestinationProviderEntity(
          id = UUID.fromString("29669658-c8f2-492c-8eab-2dd73a208d30"),
          name = "Destination Provider",
          isActive = true
        ),
        notes = null,
        booking = this
      )
    }

    every { mockArrivalTransformer.transformJpaToApi(departedBooking.arrival) } returns Arrival(
      bookingId = bookingId,
      arrivalDate = LocalDate.parse("2022-08-10"),
      expectedDepartureDate = LocalDate.parse("2022-08-16"),
      notes = null
    )

    every { mockDepartureTransformer.transformJpaToApi(departedBooking.departure) } returns Departure(
      bookingId = bookingId,
      dateTime = OffsetDateTime.parse("2022-08-30T15:30:15+01:00"),
      reason = DepartureReason(
        id = UUID.fromString("09e74ead-cf5a-40a1-a1be-739d3b97788f"),
        name = "Departure Reason",
        isActive = true
      ),
      moveOnCategory = MoveOnCategory(
        id = UUID.fromString("bcfbb1b6-f89d-45eb-ae70-308cc6930633"),
        name = "Move on Category",
        isActive = true
      ),
      destinationProvider = DestinationProvider(
        id = UUID.fromString("29669658-c8f2-492c-8eab-2dd73a208d30"),
        name = "Destination Provider",
        isActive = true
      ),
      notes = null
    )

    val transformedBooking = bookingTransformer.transformJpaToApi(departedBooking)

    assertThat(transformedBooking).isEqualTo(
      Booking(
        id = bookingId,
        person = Person(crn = "crn", name = "name"),
        arrivalDate = LocalDate.parse("2022-08-10"),
        departureDate = LocalDate.parse("2022-08-30"),
        keyWorker = "Key Worker",
        status = Booking.Status.departed,
        arrival = Arrival(
          bookingId = bookingId,
          arrivalDate = LocalDate.parse("2022-08-10"),
          expectedDepartureDate = LocalDate.parse("2022-08-16"),
          notes = null
        ),
        departure = Departure(
          bookingId = bookingId,
          dateTime = OffsetDateTime.parse("2022-08-30T15:30:15+01:00"),
          reason = DepartureReason(
            id = UUID.fromString("09e74ead-cf5a-40a1-a1be-739d3b97788f"),
            name = "Departure Reason",
            isActive = true
          ),
          moveOnCategory = MoveOnCategory(
            id = UUID.fromString("bcfbb1b6-f89d-45eb-ae70-308cc6930633"),
            name = "Move on Category",
            isActive = true
          ),
          destinationProvider = DestinationProvider(
            id = UUID.fromString("29669658-c8f2-492c-8eab-2dd73a208d30"),
            name = "Destination Provider",
            isActive = true
          ),
          notes = null
        )
      )
    )
  }
}
