package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationTimelinessCategory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ApprovedPremisesType
import java.time.OffsetDateTime

class ApprovedPremisesApplicationEntityTest {
  @Test
  fun `isShortNoticeApplication returns true if the arrivalDate is less than 28 days away from when the application was created`() {
    val createdAt = OffsetDateTime.now().minusDays(28)

    val user = UserEntityFactory()
      .withDefaultProbationRegion()
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .withCreatedAt(createdAt)
      .withArrivalDate(createdAt.plusDays(5))
      .produce()

    assertThat(application.isShortNoticeApplication()).isTrue()
  }

  @Test
  fun `isShortNoticeApplication returns false if the arrivalDate is more than 28 days away from when the application was created`() {
    val createdAt = OffsetDateTime.now().minusDays(40)

    val user = UserEntityFactory()
      .withDefaultProbationRegion()
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .withCreatedAt(createdAt)
      .withArrivalDate(createdAt.plusDays(29))
      .produce()

    assertThat(application.isShortNoticeApplication()).isFalse()
  }

  @Test
  fun `isShortNoticeApplication returns null if the arrivalDate is null`() {
    val user = UserEntityFactory()
      .withDefaultProbationRegion()
      .produce()

    val application = ApprovedPremisesApplicationEntityFactory()
      .withCreatedByUser(user)
      .withArrivalDate(null)
      .produce()

    assertThat(application.isShortNoticeApplication()).isNull()
  }

  @Nested
  inner class GetRequiredQualifications {
    @ParameterizedTest
    @CsvSource("EMERGENCY,EMERGENCY", "SHORT_NOTICE,EMERGENCY", "STANDARD,")
    fun `returns correctly for timeliness categories`(noticeType: Cas1ApplicationTimelinessCategory, qualification: UserQualification?) {
      val user = UserEntityFactory()
        .withDefaultProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withNoticeType(noticeType)
        .produce()

      assertThat(application.getRequiredQualifications()).isEqualTo(listOfNotNull(qualification))
    }

    @ParameterizedTest
    @CsvSource(
      "PIPE,PIPE",
      "ESAP,ESAP",
      "RFAP,RECOVERY_FOCUSED",
      "MHAP_ST_JOSEPHS,MENTAL_HEALTH_SPECIALIST",
      "MHAP_ELLIOTT_HOUSE,MENTAL_HEALTH_SPECIALIST",
    )
    fun `returns matching qualification for an application made to that type of premises`(apType: ApprovedPremisesType, qualification: UserQualification?) {
      val user = UserEntityFactory()
        .withDefaultProbationRegion()
        .produce()

      val application = ApprovedPremisesApplicationEntityFactory()
        .withCreatedByUser(user)
        .withApType(apType)
        .produce()

      assertThat(application.getRequiredQualifications()).isEqualTo(listOfNotNull(qualification))
    }
  }
}
