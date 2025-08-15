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
  fun `backfill applications correctly, ignoring unsubmitted applications`() {
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
      submittedAt = OffsetDateTime.now(),
    )

    val application1OverriddenDurationNotSubmitted = givenACas1Application(
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
      submittedAt = null,
    )

    val application2ArrivalDate = OffsetDateTime.now().minusDays(3).roundNanosToMillisToAccountForLossOfPrecisionInPostgres()
    val application2ApTypeStandard = givenACas1Application(
      arrivalDate = application2ArrivalDate,
      apType = ApprovedPremisesType.NORMAL,
      submittedAt = OffsetDateTime.now(),
    )

    val application2ApTypeStandardNotSubmitted = givenACas1Application(
      arrivalDate = application2ArrivalDate,
      apType = ApprovedPremisesType.NORMAL,
      submittedAt = null,
    )

    val application3ArrivalDate = OffsetDateTime.now().minusDays(4).roundNanosToMillisToAccountForLossOfPrecisionInPostgres()
    val application3ApTypeMhapStJosephs = givenACas1Application(
      arrivalDate = application3ArrivalDate,
      apType = ApprovedPremisesType.MHAP_ST_JOSEPHS,
      submittedAt = OffsetDateTime.now(),
    )

    val application3ApTypeMhapStJosephsNotSubmitted = givenACas1Application(
      arrivalDate = application3ArrivalDate,
      apType = ApprovedPremisesType.MHAP_ST_JOSEPHS,
      submittedAt = null,
    )

    val application4ArrivalDate = OffsetDateTime.now().minusDays(5).roundNanosToMillisToAccountForLossOfPrecisionInPostgres()
    val application4ApTypeMhapStElliots = givenACas1Application(
      arrivalDate = application4ArrivalDate,
      apType = ApprovedPremisesType.MHAP_ELLIOTT_HOUSE,
      submittedAt = OffsetDateTime.now(),
    )

    val application4ApTypeMhapStElliotsNotSubmitted = givenACas1Application(
      arrivalDate = application4ArrivalDate,
      apType = ApprovedPremisesType.MHAP_ELLIOTT_HOUSE,
      submittedAt = null,
    )

    val application5ArrivalDate = OffsetDateTime.now().minusDays(6).roundNanosToMillisToAccountForLossOfPrecisionInPostgres()
    val application5ApTypeRfap = givenACas1Application(
      arrivalDate = application5ArrivalDate,
      apType = ApprovedPremisesType.RFAP,
      submittedAt = OffsetDateTime.now(),
    )

    val application5ApTypeRfapNotSubmitted = givenACas1Application(
      arrivalDate = application5ArrivalDate,
      apType = ApprovedPremisesType.RFAP,
      submittedAt = null,
    )

    val application6ArrivalDate = OffsetDateTime.now().minusDays(7).roundNanosToMillisToAccountForLossOfPrecisionInPostgres()
    val application6ApTypePipe = givenACas1Application(
      arrivalDate = application6ArrivalDate,
      apType = ApprovedPremisesType.PIPE,
      submittedAt = OffsetDateTime.now(),
    )

    val application6ApTypePipeNotSubmitted = givenACas1Application(
      arrivalDate = application6ArrivalDate,
      apType = ApprovedPremisesType.PIPE,
      submittedAt = null,
    )

    val application7ArrivalDate = OffsetDateTime.now().minusDays(8).roundNanosToMillisToAccountForLossOfPrecisionInPostgres()
    val application7ApTypePipe = givenACas1Application(
      arrivalDate = application7ArrivalDate,
      apType = ApprovedPremisesType.ESAP,
      submittedAt = OffsetDateTime.now(),
    )

    val application7ApTypePipeNotSubmitted = givenACas1Application(
      arrivalDate = application7ArrivalDate,
      apType = ApprovedPremisesType.ESAP,
      submittedAt = null,
    )

    val application8ArrivalDate = OffsetDateTime.now().minusDays(9).roundNanosToMillisToAccountForLossOfPrecisionInPostgres()
    val application8ApTypeStandardNaN = givenACas1Application(
      arrivalDate = application8ArrivalDate,
      apType = ApprovedPremisesType.NORMAL,
      submittedAt = OffsetDateTime.now(),
      data = """{
                "move-on": {
                    "placement-duration": {
                        "differentDuration": "yes",
                        "duration": "NaN",
                        "durationDays": "?",
                        "durationWeeks": "?",
                        "reason": "i don't know yet"
                    }
                }
            }""",
    )

    migrationJobService.runMigrationJob(MigrationJobType.cas1BackfillApplicationDuration)

    val updatedApplication1 = approvedPremisesApplicationRepository.findByIdOrNull(application1OverriddenDuration.id)!!
    assertThat(updatedApplication1.arrivalDate).isEqualTo(application1ArrivalDate)
    assertThat(updatedApplication1.duration).isEqualTo(25)

    val updatedApplication1NotSubmitted = approvedPremisesApplicationRepository.findByIdOrNull(application1OverriddenDurationNotSubmitted.id)!!
    assertThat(updatedApplication1NotSubmitted.arrivalDate).isEqualTo(application1ArrivalDate)
    assertThat(updatedApplication1NotSubmitted.duration).isNull()

    val updatedApplication2 = approvedPremisesApplicationRepository.findByIdOrNull(application2ApTypeStandard.id)!!
    assertThat(updatedApplication2.arrivalDate).isEqualTo(application2ArrivalDate)
    assertThat(updatedApplication2.duration).isEqualTo(12 * 7)

    val updatedApplication2NotSubmitted = approvedPremisesApplicationRepository.findByIdOrNull(application2ApTypeStandardNotSubmitted.id)!!
    assertThat(updatedApplication2NotSubmitted.arrivalDate).isEqualTo(application2ArrivalDate)
    assertThat(updatedApplication2NotSubmitted.duration).isNull()

    val updatedApplication3 = approvedPremisesApplicationRepository.findByIdOrNull(application3ApTypeMhapStJosephs.id)!!
    assertThat(updatedApplication3.arrivalDate).isEqualTo(application3ArrivalDate)
    assertThat(updatedApplication3.duration).isEqualTo(12 * 7)

    val updatedApplication3NotSubmitted = approvedPremisesApplicationRepository.findByIdOrNull(application3ApTypeMhapStJosephsNotSubmitted.id)!!
    assertThat(updatedApplication3NotSubmitted.arrivalDate).isEqualTo(application3ArrivalDate)
    assertThat(updatedApplication3NotSubmitted.duration).isNull()

    val updatedApplication4 = approvedPremisesApplicationRepository.findByIdOrNull(application4ApTypeMhapStElliots.id)!!
    assertThat(updatedApplication4.arrivalDate).isEqualTo(application4ArrivalDate)
    assertThat(updatedApplication4.duration).isEqualTo(12 * 7)

    val updatedApplication4NotSubmitted = approvedPremisesApplicationRepository.findByIdOrNull(application4ApTypeMhapStElliotsNotSubmitted.id)!!
    assertThat(updatedApplication4NotSubmitted.arrivalDate).isEqualTo(application4ArrivalDate)
    assertThat(updatedApplication4NotSubmitted.duration).isNull()

    val updatedApplication5 = approvedPremisesApplicationRepository.findByIdOrNull(application5ApTypeRfap.id)!!
    assertThat(updatedApplication5.arrivalDate).isEqualTo(application5ArrivalDate)
    assertThat(updatedApplication5.duration).isEqualTo(12 * 7)

    val updatedApplication5NotSubmitted = approvedPremisesApplicationRepository.findByIdOrNull(application5ApTypeRfapNotSubmitted.id)!!
    assertThat(updatedApplication5NotSubmitted.arrivalDate).isEqualTo(application5ArrivalDate)
    assertThat(updatedApplication5NotSubmitted.duration).isNull()

    val updatedApplication6 = approvedPremisesApplicationRepository.findByIdOrNull(application6ApTypePipe.id)!!
    assertThat(updatedApplication6.arrivalDate).isEqualTo(application6ArrivalDate)
    assertThat(updatedApplication6.duration).isEqualTo(26 * 7)

    val updatedApplication6NotSubmitted = approvedPremisesApplicationRepository.findByIdOrNull(application6ApTypePipeNotSubmitted.id)!!
    assertThat(updatedApplication6NotSubmitted.arrivalDate).isEqualTo(application6ArrivalDate)
    assertThat(updatedApplication6NotSubmitted.duration).isNull()

    val updatedApplication7 = approvedPremisesApplicationRepository.findByIdOrNull(application7ApTypePipe.id)!!
    assertThat(updatedApplication7.arrivalDate).isEqualTo(application7ArrivalDate)
    assertThat(updatedApplication7.duration).isEqualTo(52 * 7)

    val updatedApplication7NotSubmitted = approvedPremisesApplicationRepository.findByIdOrNull(application7ApTypePipeNotSubmitted.id)!!
    assertThat(updatedApplication7NotSubmitted.arrivalDate).isEqualTo(application7ArrivalDate)
    assertThat(updatedApplication7NotSubmitted.duration).isNull()

    val updatedApplication8ApTypeStandardNaN = approvedPremisesApplicationRepository.findByIdOrNull(application8ApTypeStandardNaN.id)!!
    assertThat(updatedApplication8ApTypeStandardNaN.arrivalDate).isEqualTo(application8ArrivalDate)
    assertThat(updatedApplication8ApTypeStandardNaN.duration).isEqualTo(12 * 7)
  }
}
