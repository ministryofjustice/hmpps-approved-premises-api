package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.domainevents

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingChanged
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1BookingChangedContentPayload
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingChangedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.EventPremisesFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.domainevent.BookingChangedTimelineFactory
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class BookingChangedTimelineFactoryTest {
  @MockK
  lateinit var domainEventService: Cas1DomainEventService

  @InjectMockKs
  lateinit var service: BookingChangedTimelineFactory

  val id: UUID = UUID.randomUUID()

  @Test
  fun `Schema version is null`() {
    val arrivalDate = LocalDate.of(2024, 1, 1)
    val departureDate = LocalDate.of(2024, 4, 1)

    every { domainEventService.get(id, BookingChanged::class) } returns buildDomainEvent(
      data = BookingChangedFactory()
        .withArrivalOn(arrivalDate)
        .withDepartureOn(departureDate)
        .withPremises(
          EventPremisesFactory()
            .withName("The Premises Name")
            .produce(),
        )
        .produce(),
      schemaVersion = null,
    )

    val result = service.produce(id)

    assertThat(result.description)
      .isEqualTo("A placement at The Premises Name had its arrival and/or departure date changed to Monday 1 January 2024 to Monday 1 April 2024")
    assertThat(result.payload).isNull()
  }

  @Test
  fun `Schema version is 2 and only previous arrival date is present`() {
    val arrivalDate = LocalDate.of(2025, 4, 5)
    val departureDate = LocalDate.of(2025, 6, 1)
    val previousArrivalOn = LocalDate.of(2025, 4, 1)

    every { domainEventService.get(id, BookingChanged::class) } returns buildDomainEvent(
      data = BookingChangedFactory()
        .withArrivalOn(arrivalDate)
        .withPreviousArrivalOn(null)
        .withDepartureOn(departureDate)
        .withPreviousArrivalOn(previousArrivalOn)
        .withPremises(
          EventPremisesFactory()
            .withName("The Premises Name")
            .produce(),
        )
        .withCharacteristics(listOf(SpaceCharacteristic.hasEnSuite))
        .withPreviousCharacteristics(null)
        .produce(),
      schemaVersion = 2,
    )

    val result = service.produce(id)

    assertThat(result.description)
      .isEqualTo("A placement at The Premises Name had its arrival date changed from Tuesday 1 April 2025 to Saturday 5 April 2025")

    val contentPayload = result.payload as Cas1BookingChangedContentPayload
    assertThat(contentPayload.premises.name).isEqualTo("The Premises Name")
    assertThat(contentPayload.expectedArrival).isEqualTo(arrivalDate)
    assertThat(contentPayload.previousExpectedArrival).isEqualTo(previousArrivalOn)
    assertThat(contentPayload.expectedDeparture).isEqualTo(departureDate)
    assertThat(contentPayload.previousExpectedDeparture).isNull()
    assertThat(contentPayload.characteristics).isEqualTo(listOf(Cas1SpaceCharacteristic.hasEnSuite))
    assertThat(contentPayload.previousCharacteristics).isNull()
  }

  @Test
  fun `Schema version is 2 and only previous departure date is present`() {
    val arrivalDate = LocalDate.of(2025, 4, 5)
    val departureDate = LocalDate.of(2025, 6, 10)
    val previousDepartureOn = LocalDate.of(2025, 6, 1)

    every { domainEventService.get(id, BookingChanged::class) } returns buildDomainEvent(
      data = BookingChangedFactory()
        .withArrivalOn(arrivalDate)
        .withPreviousArrivalOn(null)
        .withDepartureOn(departureDate)
        .withPreviousDepartureOn(previousDepartureOn)
        .withPremises(
          EventPremisesFactory()
            .withName("The Premises Name")
            .produce(),
        )
        .withCharacteristics(listOf(SpaceCharacteristic.hasEnSuite))
        .withPreviousCharacteristics(null)
        .produce(),
      schemaVersion = 2,
    )

    val result = service.produce(id)

    assertThat(result.description)
      .isEqualTo("A placement at The Premises Name had its departure date changed from Sunday 1 June 2025 to Tuesday 10 June 2025")

    val contentPayload = result.payload as Cas1BookingChangedContentPayload
    assertThat(contentPayload.premises.name).isEqualTo("The Premises Name")
    assertThat(contentPayload.expectedArrival).isEqualTo(arrivalDate)
    assertThat(contentPayload.previousExpectedArrival).isNull()
    assertThat(contentPayload.expectedDeparture).isEqualTo(departureDate)
    assertThat(contentPayload.previousExpectedDeparture).isEqualTo(previousDepartureOn)
    assertThat(contentPayload.characteristics).containsExactly(Cas1SpaceCharacteristic.hasEnSuite)
    assertThat(contentPayload.previousCharacteristics).isNull()
  }

  @Test
  fun `Schema version is 2 and previous arrival, departure dates and characteristics are present`() {
    val arrivalDate = LocalDate.of(2025, 4, 5)
    val departureDate = LocalDate.of(2025, 6, 10)
    val previousArrivalOn = LocalDate.of(2025, 4, 1)
    val previousDepartureOn = LocalDate.of(2025, 6, 1)

    every { domainEventService.get(id, BookingChanged::class) } returns buildDomainEvent(
      data = BookingChangedFactory()
        .withArrivalOn(arrivalDate)
        .withDepartureOn(departureDate)
        .withPreviousArrivalOn(previousArrivalOn)
        .withPreviousDepartureOn(previousDepartureOn)
        .withCharacteristics(listOf(SpaceCharacteristic.hasEnSuite))
        .withPreviousCharacteristics(listOf(SpaceCharacteristic.isArsonSuitable))
        .withPremises(
          EventPremisesFactory()
            .withName("The Premises Name")
            .produce(),
        )
        .produce(),
      schemaVersion = 2,
    )

    val result = service.produce(id)

    assertThat(result.description)
      .isEqualTo(
        "A placement at The Premises Name had its arrival date changed from Tuesday 1 April 2025 to Saturday 5 April 2025, " +
          "its departure date changed from Sunday 1 June 2025 to Tuesday 10 June 2025",
      )

    val contentPayload = result.payload as Cas1BookingChangedContentPayload
    assertThat(contentPayload.premises.name).isEqualTo("The Premises Name")
    assertThat(contentPayload.expectedArrival).isEqualTo(arrivalDate)
    assertThat(contentPayload.previousExpectedArrival).isEqualTo(previousArrivalOn)
    assertThat(contentPayload.expectedDeparture).isEqualTo(departureDate)
    assertThat(contentPayload.previousExpectedDeparture).isEqualTo(previousDepartureOn)
    assertThat(contentPayload.characteristics).containsExactly(Cas1SpaceCharacteristic.hasEnSuite)
    assertThat(contentPayload.previousCharacteristics).containsExactly(Cas1SpaceCharacteristic.isArsonSuitable)
  }
}
