package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.seed

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed.Cas1NonArrivalPlacementDataFixSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.seed.PlacementSeedRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import java.time.Instant
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1NonArrivalPlacementDataFixSeedJobTest {
  @MockK
  private lateinit var spaceBookingRepository: Cas1SpaceBookingRepository

  @InjectMockKs
  private lateinit var seedJob: Cas1NonArrivalPlacementDataFixSeedJob

  @Test
  fun `processRow throws error if placement not found`() {
    val placementId = UUID.randomUUID()
    val crn = "CRN123"
    val row = PlacementSeedRow(crn = crn, spaceBookingId = placementId)

    every { spaceBookingRepository.findByIdOrNull(placementId) } returns null

    assertThatThrownBy {
      seedJob.processRow(row)
    }.isInstanceOf(RuntimeException::class.java)
      .hasMessageContaining("Placement with ID $placementId not found for id $placementId")
  }

  @Test
  fun `processRow throws error if CRN does not match`() {
    val placementId = UUID.randomUUID()
    val crn = "CRN123"
    val row = PlacementSeedRow(crn = crn, spaceBookingId = placementId)
    val placement = Cas1SpaceBookingEntityFactory()
      .withCrn("OTHERCRN")
      .produce()

    every { spaceBookingRepository.findByIdOrNull(placementId) } returns placement

    assertThatThrownBy {
      seedJob.processRow(row)
    }.isInstanceOf(RuntimeException::class.java)
      .hasMessageContaining("Placement with ID $placementId has incorrect CRN OTHERCRN")
  }

  @Test
  fun `processRow clears non-arrival data if present`() {
    val placementId = UUID.randomUUID()
    val crn = "CRN123"
    val row = PlacementSeedRow(crn = crn, spaceBookingId = placementId)
    val placement = Cas1SpaceBookingEntityFactory()
      .withId(placementId)
      .withCrn(crn)
      .withNonArrivalConfirmedAt(Instant.now())
      .produce()

    every { spaceBookingRepository.findByIdOrNull(placementId) } returns placement
    every { spaceBookingRepository.save(any()) } returns placement

    seedJob.processRow(row)

    verify {
      spaceBookingRepository.save(
        match {
          it.nonArrivalConfirmedAt == null &&
            it.nonArrivalNotes == null &&
            it.nonArrivalReason == null
        },
      )
    }
  }

  @Test
  fun `processRow does nothing if non-arrival data is not present`() {
    val placementId = UUID.randomUUID()
    val crn = "CRN123"
    val row = PlacementSeedRow(crn = crn, spaceBookingId = placementId)
    val placement = Cas1SpaceBookingEntityFactory()
      .withCrn(crn)
      .withNonArrivalConfirmedAt(null)
      .produce()

    every { spaceBookingRepository.findByIdOrNull(placementId) } returns placement

    seedJob.processRow(row)

    verify(exactly = 0) {
      spaceBookingRepository.save(any())
    }
  }
}
