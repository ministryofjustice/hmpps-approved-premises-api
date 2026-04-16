package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import java.util.UUID

class Cas1UpdatePremisesSupportSpaceBookingSeedJobTest : SeedTestBase() {

  @Test
  fun `Update support space booking`() {
    val premises1 = givenAnApprovedPremises(supportsSpaceBookings = false)
    val premises2 = givenAnApprovedPremises(supportsSpaceBookings = true)

    seed(
      SeedFileType.approvedPremisesUpdatePremisesSupportSpaceBooking,
      """premises_id,support_space_booking
        |${premises1.id},yes
        |${premises2.id},no
      """.trimMargin(),
    )

    assertThat(approvedPremisesRepository.findByIdOrNull(premises1.id)!!.supportsSpaceBookings).isTrue()
    assertThat(approvedPremisesRepository.findByIdOrNull(premises2.id)!!.supportsSpaceBookings).isFalse()
  }

  @Test
  fun `Update support space booking fails if premises does not exist`() {
    val randomId = UUID.randomUUID()

    seed(
      SeedFileType.approvedPremisesUpdatePremisesSupportSpaceBooking,
      """premises_id,support_space_booking
        |$randomId,yes
      """.trimMargin(),
    )

    assertError(1, "Could not find approved premises with id $randomId")
  }
}
