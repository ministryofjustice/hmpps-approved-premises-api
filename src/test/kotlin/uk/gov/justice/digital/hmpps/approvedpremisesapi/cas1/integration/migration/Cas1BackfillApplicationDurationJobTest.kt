package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas1.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJobService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.roundNanosToMillisToAccountForLossOfPrecisionInPostgres
import java.time.OffsetDateTime

class Cas1BackfillApplicationDurationJobTest : IntegrationTestBase() {
  @Autowired
  lateinit var migrationJobService: MigrationJobService

  @Test
  fun `backfill applications correctly`() {
    val application1ArrivalDate = OffsetDateTime.now().minusDays(2).roundNanosToMillisToAccountForLossOfPrecisionInPostgres()
    val application1OverriddenDuration = givenACas1Application(
      arrivalDate = application1ArrivalDate,
      apType = ApprovedPremisesType.PIPE,
      data = """{
                "move-on": {
                    "placement-duration": {
                        "differentDuration": "yes",
                        "duration": "25",
                        "durationDays": "1",
                        "durationWeeks": "1",
                        "reason": "asd"
                    }
                }
            }""",
    )

    val application2ArrivalDate = OffsetDateTime.now().minusDays(3).roundNanosToMillisToAccountForLossOfPrecisionInPostgres()
    val application2ApTypeStandard = givenACas1Application(
      arrivalDate = application2ArrivalDate,
      apType = ApprovedPremisesType.NORMAL,
    )

    val application3ArrivalDate = OffsetDateTime.now().minusDays(4).roundNanosToMillisToAccountForLossOfPrecisionInPostgres()
    val application3ApTypeMhapStJosephs = givenACas1Application(
      arrivalDate = application3ArrivalDate,
      apType = ApprovedPremisesType.MHAP_ST_JOSEPHS,
    )

    val application4ArrivalDate = OffsetDateTime.now().minusDays(5).roundNanosToMillisToAccountForLossOfPrecisionInPostgres()
    val application4ApTypeMhapStElliots = givenACas1Application(
      arrivalDate = application4ArrivalDate,
      apType = ApprovedPremisesType.MHAP_ELLIOTT_HOUSE,
    )

    val application5ArrivalDate = OffsetDateTime.now().minusDays(6).roundNanosToMillisToAccountForLossOfPrecisionInPostgres()
    val application5ApTypeRfap = givenACas1Application(
      arrivalDate = application5ArrivalDate,
      apType = ApprovedPremisesType.RFAP,
    )

    val application6ArrivalDate = OffsetDateTime.now().minusDays(7).roundNanosToMillisToAccountForLossOfPrecisionInPostgres()
    val application6ApTypePipe = givenACas1Application(
      arrivalDate = application6ArrivalDate,
      apType = ApprovedPremisesType.PIPE,
    )

    val application7ArrivalDate = OffsetDateTime.now().minusDays(8).roundNanosToMillisToAccountForLossOfPrecisionInPostgres()
    val application7ApTypePipe = givenACas1Application(
      arrivalDate = application7ArrivalDate,
      apType = ApprovedPremisesType.ESAP,
    )

    migrationJobService.runMigrationJob(MigrationJobType.cas1BackfillApplicationDuration)

    val updatedApplication1 = approvedPremisesApplicationRepository.findByIdOrNull(application1OverriddenDuration.id)!!
    assertThat(updatedApplication1.arrivalDate).isEqualTo(application1ArrivalDate)
    assertThat(updatedApplication1.duration).isEqualTo(25)

    val updatedApplication2 = approvedPremisesApplicationRepository.findByIdOrNull(application2ApTypeStandard.id)!!
    assertThat(updatedApplication2.arrivalDate).isEqualTo(application2ArrivalDate)
    assertThat(updatedApplication2.duration).isEqualTo(12 * 7)

    val updatedApplication3 = approvedPremisesApplicationRepository.findByIdOrNull(application3ApTypeMhapStJosephs.id)!!
    assertThat(updatedApplication3.arrivalDate).isEqualTo(application3ArrivalDate)
    assertThat(updatedApplication3.duration).isEqualTo(12 * 7)

    val updatedApplication4 = approvedPremisesApplicationRepository.findByIdOrNull(application4ApTypeMhapStElliots.id)!!
    assertThat(updatedApplication4.arrivalDate).isEqualTo(application4ArrivalDate)
    assertThat(updatedApplication4.duration).isEqualTo(12 * 7)

    val updatedApplication5 = approvedPremisesApplicationRepository.findByIdOrNull(application5ApTypeRfap.id)!!
    assertThat(updatedApplication5.arrivalDate).isEqualTo(application5ArrivalDate)
    assertThat(updatedApplication5.duration).isEqualTo(12 * 7)

    val updatedApplication6 = approvedPremisesApplicationRepository.findByIdOrNull(application6ApTypePipe.id)!!
    assertThat(updatedApplication6.arrivalDate).isEqualTo(application6ArrivalDate)
    assertThat(updatedApplication6.duration).isEqualTo(26 * 7)

    val updatedApplication7 = approvedPremisesApplicationRepository.findByIdOrNull(application7ApTypePipe.id)!!
    assertThat(updatedApplication7.arrivalDate).isEqualTo(application7ArrivalDate)
    assertThat(updatedApplication7.duration).isEqualTo(52 * 7)
  }
}
