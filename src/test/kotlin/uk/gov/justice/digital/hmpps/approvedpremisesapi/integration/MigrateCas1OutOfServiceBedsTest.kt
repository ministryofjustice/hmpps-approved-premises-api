package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName

class MigrateCas1OutOfServiceBedsTest : MigrationJobTestBase() {
  @Test
  fun `Job migrates expected set of lost bed reasons`() {
    val approvedPremisesLostBedReasons = lostBedReasonRepository
      .findAll()
      .filter { listOf("*", ServiceName.approvedPremises.value).contains(it.serviceScope) }

    assertThat(approvedPremisesLostBedReasons).isNotEmpty
    assertThat(cas1OutOfServiceBedReasonTestRepository.findAll()).isEmpty()

    migrationJobService.runMigrationJob(MigrationJobType.cas1OutOfServiceBedReasons)

    assertThat(cas1OutOfServiceBedReasonTestRepository.findAll()).hasSize(approvedPremisesLostBedReasons.size)
  }
}
