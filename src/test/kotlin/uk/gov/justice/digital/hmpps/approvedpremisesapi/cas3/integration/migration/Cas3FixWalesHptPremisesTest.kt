package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.integration.Cas3IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.jpa.entity.Cas3PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas3.migration.Cas3FixWalesHptPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import java.time.LocalDate
import java.util.UUID

class Cas3FixWalesHptPremisesTest : Cas3IntegrationTestBase() {

  @Autowired
  lateinit var cas3FixWalesHptPremises: Cas3FixWalesHptPremises

  private val walesHptPremisesId = UUID.fromString("1cf35a14-553e-435b-8b88-eeebdf4bbc28")
  private val expectedFixedStartDate = LocalDate.of(2025, 12, 4)
  private lateinit var walesHptPremises: Cas3PremisesEntity

  @BeforeEach
  fun setupTestData() {
    val probationRegion = givenAProbationRegion()
    val localAuthorityArea = localAuthorityEntityFactory.produceAndPersist()
    val probationDeliveryUnit = probationDeliveryUnitFactory.produceAndPersist {
      withProbationRegion(probationRegion)
    }

    walesHptPremises = cas3PremisesEntityFactory.produceAndPersist {
      withId(walesHptPremisesId)
      withName("Wales HPT Premises")
      withLocalAuthorityArea(localAuthorityArea)
      withProbationDeliveryUnit(probationDeliveryUnit)
      withStartDate(LocalDate.of(2025, 12, 5))
    }
  }

  @Test
  fun `should fix the Wales HPT premises start date`() {
    val premisesBeforeMigration = cas3PremisesRepository.findById(walesHptPremisesId).get()
    assertThat(premisesBeforeMigration.startDate).isNotEqualTo(expectedFixedStartDate)

    cas3FixWalesHptPremises.process(1)

    val premisesAfterMigration = cas3PremisesRepository.findById(walesHptPremisesId).get()
    assertThat(premisesAfterMigration.startDate).isEqualTo(expectedFixedStartDate)
  }

  @Test
  fun `running the migration twice does not cause issues`() {
    cas3FixWalesHptPremises.process(1)
    val premisesAfterFirstRun = cas3PremisesRepository.findById(walesHptPremisesId).get()
    assertThat(premisesAfterFirstRun.startDate).isEqualTo(expectedFixedStartDate)

    cas3FixWalesHptPremises.process(1)
    val premisesAfterSecondRun = cas3PremisesRepository.findById(walesHptPremisesId).get()
    assertThat(premisesAfterSecondRun.startDate).isEqualTo(expectedFixedStartDate)
  }

  @Test
  fun `should handle case when premises does not exist`() {
    cas3PremisesRepository.deleteById(walesHptPremisesId)

    cas3FixWalesHptPremises.process(1)

    assertThat(cas3PremisesRepository.findById(walesHptPremisesId)).isEmpty
  }
}
