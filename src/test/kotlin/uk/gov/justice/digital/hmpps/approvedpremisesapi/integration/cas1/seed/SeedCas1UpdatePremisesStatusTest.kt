package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApprovedPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase

class SeedCas1UpdatePremisesStatusTest : SeedTestBase() {

  @Test
  fun `update status`() {
    val premises = givenAnApprovedPremises(status = PropertyStatus.active)

    seed(
      SeedFileType.approvedPremisesUpdatePremisesStatus,
      """premises_id,status
        |${premises.id},archived 
      """.trimMargin(),
    )

    assertThat(approvedPremisesRepository.findByIdOrNull(premises.id)!!.status).isEqualTo(PropertyStatus.archived)
  }
}
