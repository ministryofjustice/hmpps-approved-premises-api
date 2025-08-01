package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.unit.seed

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed.Cas1UpdateActualArrivalDateSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed.Cas1UpdateActualArrivalDateSeedJobCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ApplicationTimelineNoteService
import java.time.LocalDate
import java.util.UUID

@SuppressWarnings("UnusedPrivateProperty")
@ExtendWith(MockKExtension::class)
class Cas1UpdateActualArrivalDateSeedJobTest {

  private companion object {
    val BOOKING_ID: UUID = UUID.fromString("46335983-2742-4736-8b6d-9113629a5286")
  }

  @MockK
  private lateinit var cas1SpaceBookingRepository: Cas1SpaceBookingRepository

  @MockK
  private lateinit var cas1ApplicationTimelineNoteService: Cas1ApplicationTimelineNoteService

  @MockK
  private lateinit var domainEventService: DomainEventRepository

  @MockK
  private lateinit var objectMapper: ObjectMapper

  @InjectMockKs
  private lateinit var seedJob: Cas1UpdateActualArrivalDateSeedJob

  @Test
  fun `error if space booking not found`() {
    every { cas1SpaceBookingRepository.findByIdOrNull(BOOKING_ID) } returns null

    assertThatThrownBy {
      seedJob.processRow(
        Cas1UpdateActualArrivalDateSeedJobCsvRow(
          spaceBookingId = BOOKING_ID,
          currentArrivalDate = LocalDate.of(2025, 7, 1),
          updatedArrivalDate = LocalDate.of(2025, 7, 2),
        ),
      )
    }.hasMessageContaining("Could not find space booking with id 46335983-2742-4736-8b6d-9113629a5286")
  }

  @Test
  fun `error if current arrival date isn't as expected`() {
    every { cas1SpaceBookingRepository.findByIdOrNull(BOOKING_ID) } returns Cas1SpaceBookingEntityFactory()
      .withActualArrivalDate(LocalDate.of(2025, 7, 3))
      .produce()

    assertThatThrownBy {
      seedJob.processRow(
        Cas1UpdateActualArrivalDateSeedJobCsvRow(
          spaceBookingId = BOOKING_ID,
          currentArrivalDate = LocalDate.of(2025, 7, 1),
          updatedArrivalDate = LocalDate.of(2025, 7, 2),
        ),
      )
    }.hasMessageContaining("Expected current actual arrival date to be 2025-07-01, but was actually 2025-07-03")
  }

  // Note: Success paths are tested by the integration test SeedCas1UpdateActualArrivalDateTest
}
