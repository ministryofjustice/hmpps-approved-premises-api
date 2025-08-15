package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
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
}
