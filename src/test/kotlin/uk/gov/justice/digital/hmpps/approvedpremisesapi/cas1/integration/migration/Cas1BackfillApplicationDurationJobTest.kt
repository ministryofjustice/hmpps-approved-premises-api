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
    val application1NoArrivalDate = givenACas1Application(
      arrivalDate = null,
    )

    val application2ArrivalDate = OffsetDateTime.now().minusDays(2).roundNanosToMillisToAccountForLossOfPrecisionInPostgres()
    val application2OverriddenDuration = givenACas1Application(
      arrivalDate = application2ArrivalDate,
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

    val application3ArrivalDate = OffsetDateTime.now().minusDays(3).roundNanosToMillisToAccountForLossOfPrecisionInPostgres()
    val application3ApTypeStandard = givenACas1Application(
      arrivalDate = application3ArrivalDate,
      apType = ApprovedPremisesType.NORMAL,
    )

    val application4ArrivalDate = OffsetDateTime.now().minusDays(4).roundNanosToMillisToAccountForLossOfPrecisionInPostgres()
    val application4ApTypeMhapStJosephs = givenACas1Application(
      arrivalDate = application4ArrivalDate,
      apType = ApprovedPremisesType.MHAP_ST_JOSEPHS,
    )

    val application5ArrivalDate = OffsetDateTime.now().minusDays(5).roundNanosToMillisToAccountForLossOfPrecisionInPostgres()
    val application5ApTypeMhapStElliots = givenACas1Application(
      arrivalDate = application5ArrivalDate,
      apType = ApprovedPremisesType.MHAP_ELLIOTT_HOUSE,
    )

    val application6ArrivalDate = OffsetDateTime.now().minusDays(6).roundNanosToMillisToAccountForLossOfPrecisionInPostgres()
    val application6ApTypeRfap = givenACas1Application(
      arrivalDate = application6ArrivalDate,
      apType = ApprovedPremisesType.RFAP,
    )

    val application7ArrivalDate = OffsetDateTime.now().minusDays(7).roundNanosToMillisToAccountForLossOfPrecisionInPostgres()
    val application7ApTypePipe = givenACas1Application(
      arrivalDate = application7ArrivalDate,
      apType = ApprovedPremisesType.PIPE,
    )

    val application8ArrivalDate = OffsetDateTime.now().minusDays(8).roundNanosToMillisToAccountForLossOfPrecisionInPostgres()
    val application8ApTypePipe = givenACas1Application(
      arrivalDate = application8ArrivalDate,
      apType = ApprovedPremisesType.ESAP,
    )

    migrationJobService.runMigrationJob(MigrationJobType.cas1BackfillApplicationDuration)

    val updatedApplication1 = approvedPremisesApplicationRepository.findByIdOrNull(application1NoArrivalDate.id)!!
    assertThat(updatedApplication1.arrivalDate).isNull()
    assertThat(updatedApplication1.duration).isNull()

    val updatedApplication2 = approvedPremisesApplicationRepository.findByIdOrNull(application2OverriddenDuration.id)!!
    assertThat(updatedApplication2.arrivalDate).isEqualTo(application2ArrivalDate)
    assertThat(updatedApplication2.duration).isEqualTo(25)

    val updatedApplication3 = approvedPremisesApplicationRepository.findByIdOrNull(application3ApTypeStandard.id)!!
    assertThat(updatedApplication3.arrivalDate).isEqualTo(application3ArrivalDate)
    assertThat(updatedApplication3.duration).isEqualTo(12 * 7)

    val updatedApplication4 = approvedPremisesApplicationRepository.findByIdOrNull(application4ApTypeMhapStJosephs.id)!!
    assertThat(updatedApplication4.arrivalDate).isEqualTo(application4ArrivalDate)
    assertThat(updatedApplication4.duration).isEqualTo(12 * 7)

    val updatedApplication5 = approvedPremisesApplicationRepository.findByIdOrNull(application5ApTypeMhapStElliots.id)!!
    assertThat(updatedApplication5.arrivalDate).isEqualTo(application5ArrivalDate)
    assertThat(updatedApplication5.duration).isEqualTo(12 * 7)

    val updatedApplication6 = approvedPremisesApplicationRepository.findByIdOrNull(application6ApTypeRfap.id)!!
    assertThat(updatedApplication6.arrivalDate).isEqualTo(application6ArrivalDate)
    assertThat(updatedApplication6.duration).isEqualTo(12 * 7)

    val updatedApplication7 = approvedPremisesApplicationRepository.findByIdOrNull(application7ApTypePipe.id)!!
    assertThat(updatedApplication7.arrivalDate).isEqualTo(application7ArrivalDate)
    assertThat(updatedApplication7.duration).isEqualTo(26 * 7)

    val updatedApplication8 = approvedPremisesApplicationRepository.findByIdOrNull(application8ApTypePipe.id)!!
    assertThat(updatedApplication8.arrivalDate).isEqualTo(application8ArrivalDate)
    assertThat(updatedApplication8.duration).isEqualTo(52 * 7)
  }
}
