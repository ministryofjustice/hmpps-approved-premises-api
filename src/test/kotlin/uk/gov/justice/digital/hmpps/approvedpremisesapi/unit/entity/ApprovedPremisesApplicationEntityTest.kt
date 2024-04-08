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
    @CsvSource("emergency,EMERGENCY", "shortNotice,EMERGENCY", "standard,")
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
  }
}
