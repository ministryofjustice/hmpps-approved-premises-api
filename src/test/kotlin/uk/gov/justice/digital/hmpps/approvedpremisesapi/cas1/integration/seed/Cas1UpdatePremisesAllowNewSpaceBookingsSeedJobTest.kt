package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import java.util.UUID

class Cas1UpdatePremisesAllowNewSpaceBookingsSeedJobTest : SeedTestBase() {

  @Test
  fun `Update allow new space bookings`() {
    val premises1 = givenAnApprovedPremises(allowNewSpaceBookings = false)
    val premises2 = givenAnApprovedPremises(allowNewSpaceBookings = true)

    assertThat(approvedPremisesRepository.findByIdOrNull(premises1.id)!!.allowNewSpaceBookings).isFalse()
    assertThat(approvedPremisesRepository.findByIdOrNull(premises2.id)!!.allowNewSpaceBookings).isTrue()

    seed(
      SeedFileType.approvedPremisesUpdatePremisesAllowNewSpaceBookings,
      """premises_id,allow_new_space_bookings
        |${premises1.id},yes
        |${premises2.id},no
      """.trimMargin(),
    )

    assertThat(approvedPremisesRepository.findByIdOrNull(premises1.id)!!.allowNewSpaceBookings).isTrue()
    assertThat(approvedPremisesRepository.findByIdOrNull(premises2.id)!!.allowNewSpaceBookings).isFalse()
  }

  @Test
  fun `Update support space booking fails if premises does not exist`() {
    val randomId = UUID.randomUUID()

    seed(
      SeedFileType.approvedPremisesUpdatePremisesAllowNewSpaceBookings,
      """premises_id,allow_new_space_bookings
        |$randomId,yes
      """.trimMargin(),
    )

    assertError(1, "Could not find approved premises with id $randomId")
  }
}
