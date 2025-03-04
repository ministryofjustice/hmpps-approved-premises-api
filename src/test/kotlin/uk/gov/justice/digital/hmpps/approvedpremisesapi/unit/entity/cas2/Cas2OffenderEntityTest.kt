package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.entity.cas2

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2PrisonerLocationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2.Cas2OffenderEntity
import java.time.OffsetDateTime
import java.util.UUID

class Cas2OffenderEntityTest {

  private fun createLocation(
    offender: Cas2OffenderEntity,
    prisonCode: String,
    allocatedPomUserId: UUID,
    createdAt: OffsetDateTime,
  ) = Cas2PrisonerLocationEntity(
    id = UUID.randomUUID(),
    offender = offender,
    prisonCode = prisonCode,
    allocatedPomUserId = allocatedPomUserId,
    createdAt = createdAt,
  )

  private fun createOffender(
    locations: List<Cas2PrisonerLocationEntity> = emptyList(),
  ) = Cas2OffenderEntity(
    id = UUID.randomUUID(),
    nomsNumber = "A1234BC",
    crn = "CRN001",
    createdAt = OffsetDateTime.now().minusDays(10),
    updatedAt = OffsetDateTime.now(),
    locations = locations,
  )

  @Test
  fun `currentLocation should return the most recent location`() {
    val now = OffsetDateTime.now()
    val location1 =
      createLocation(
        prisonCode = "ABC",
        allocatedPomUserId = UUID.randomUUID(),
        createdAt = now.minusDays(5),
        offender = mockk(),
      )
    val location2 =
      createLocation(prisonCode = "XYZ", allocatedPomUserId = UUID.randomUUID(), createdAt = now, offender = mockk())

    val offender = createOffender(locations = listOf(location1, location2))

    assertThat(offender.currentLocation()).isEqualTo(location2)
  }

  @Test
  fun `currentLocation should return null when there are no locations`() {
    val offender = createOffender()

    assertThat(offender.currentLocation()).isNull()
  }

  @Test
  fun `hasCorrectCurrentLocation should return true if latest location matches prisonCode and userId`() {
    val userId = UUID.randomUUID()
    val prisonCode = "ABC123"
    val location =
      createLocation(
        prisonCode = prisonCode,
        allocatedPomUserId = userId,
        createdAt = OffsetDateTime.now(),
        offender = mockk(),
      )

    val offender = createOffender(locations = listOf(location))

    assertThat(offender.hasCorrectCurrentLocation(prisonCode, userId)).isTrue()
  }

  @Test
  fun `hasCorrectCurrentLocation should return false if latest location does not match prisonCode or userId`() {
    val userId = UUID.randomUUID()
    val differentUserId = UUID.randomUUID()
    val prisonCode = "ABC123"
    val differentPrisonCode = "XYZ789"
    val location =
      createLocation(
        prisonCode = prisonCode,
        allocatedPomUserId = userId,
        createdAt = OffsetDateTime.now(),
        offender = mockk(),
      )

    val offender = createOffender(locations = listOf(location))

    assertThat(offender.hasCorrectCurrentLocation(differentPrisonCode, userId)).isFalse()
    assertThat(offender.hasCorrectCurrentLocation(prisonCode, differentUserId)).isFalse()
  }
}
