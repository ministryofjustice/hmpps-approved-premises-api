package uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.integration.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2.model.Cas2ServiceOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.cas2hdc.jpa.entity.Cas2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import java.time.LocalDate
import java.time.OffsetDateTime

class Cas2BackfillAccommodationStartDateSeedJobTest : SeedTestBase() {
  @Test
  fun `backfills only submitted other applications with an answer and no existing date`() {
    val user = cas2UserEntityFactory.produceAndPersist()
    val eligible = givenApplication(
      user,
      data = """{"cohort-selection":{"accommodation-start-date":{"accommodationStartDate":"2026-10-01","accommodationStartDate-day":"01","accommodationStartDate-month":"10","accommodationStartDate-year":"2026"}}}""",
    )
    val anotherEligible = givenApplication(user, data = dataWithDate("2026-12-15"))
    val existing = givenApplication(user, date = LocalDate.parse("2026-11-20"))
    val draft = givenApplication(user, submitted = false)
    val differentOrigin = givenApplication(user, origin = ApplicationOrigin.prisonBail)
    val missingAnswer = givenApplication(user, data = """{"cohort-selection":{}}""")
    val nullAnswer = givenApplication(user, data = """{"cohort-selection":{"accommodation-start-date":{"accommodationStartDate":null}}}""")
    val wrongSection = givenApplication(user, data = """{"cohorts":{"accommodation-start-date":{"accommodationStartDate":"2026-10-01"}}}""")
    val noData = givenApplication(user, data = null)

    seed(SeedFileType.cas2BackfillAccommodationStartDate, "run\ntrue\n")
    seed(SeedFileType.cas2BackfillAccommodationStartDate, "run\ntrue\n")

    assertThat(dateFor(eligible)).isEqualTo(LocalDate.parse("2026-10-01"))
    assertThat(dateFor(anotherEligible)).isEqualTo(LocalDate.parse("2026-12-15"))
    assertThat(dateFor(existing)).isEqualTo(LocalDate.parse("2026-11-20"))
    assertThat(dateFor(draft)).isNull()
    assertThat(dateFor(differentOrigin)).isNull()
    assertThat(dateFor(missingAnswer)).isNull()
    assertThat(dateFor(nullAnswer)).isNull()
    assertThat(dateFor(wrongSection)).isNull()
    assertThat(dateFor(noData)).isNull()
    assertThat(logEntries).noneMatch { it.level == "error" }
    assertThat(logEntries).anyMatch { it.message == "Backfilled accommodation start dates for 2 applications" }
    assertThat(logEntries).anyMatch { it.message == "Backfilled accommodation start dates for 0 applications" }
  }

  @Test
  fun `invalid dates fail the backfill without updating other applications`() {
    val user = cas2UserEntityFactory.produceAndPersist()
    val eligible = givenApplication(user)
    val invalid = givenApplication(user, data = dataWithDate("not-a-date"))

    seed(SeedFileType.cas2BackfillAccommodationStartDate, "run\ntrue\n")

    assertThat(dateFor(eligible)).isNull()
    assertThat(dateFor(invalid)).isNull()
    assertError(1, "Invalid accommodation start date for application ${invalid.id}: not-a-date")
  }

  @Test
  fun `a non-string answer fails with the application id`() {
    val user = cas2UserEntityFactory.produceAndPersist()
    val invalid = givenApplication(user, data = """{"cohort-selection":{"accommodation-start-date":{"accommodationStartDate":123}}}""")

    seed(SeedFileType.cas2BackfillAccommodationStartDate, "run\ntrue\n")

    assertThat(dateFor(invalid)).isNull()
    assertError(1, "Accommodation start date for application ${invalid.id} is not a string")
  }

  @Test
  fun `invalid seed input does not run the backfill`() {
    val user = cas2UserEntityFactory.produceAndPersist()
    val eligible = givenApplication(user)

    seed(SeedFileType.cas2BackfillAccommodationStartDate, "run\nfalse\n")

    assertThat(dateFor(eligible)).isNull()
    assertThat(logEntries).anyMatch { it.level == "error" }
  }

  private fun givenApplication(
    user: Cas2UserEntity,
    data: String? = dataWithDate("2026-10-01"),
    origin: ApplicationOrigin = ApplicationOrigin.other,
    submitted: Boolean = true,
    date: LocalDate? = null,
  ): Cas2ApplicationEntity = cas2ApplicationEntityFactory.produceAndPersist {
    withCreatedByUser(user)
    withServiceOrigin(Cas2ServiceOrigin.BAIL)
    withApplicationOrigin(origin)
    withSubmittedAt(if (submitted) OffsetDateTime.parse("2026-10-20T12:00:00Z") else null)
    withData(data)
    withAccommodationStartDate(date)
  }

  private fun dateFor(application: Cas2ApplicationEntity) = cas2ApplicationRepository.findById(application.id).orElseThrow().accommodationStartDate

  private fun dataWithDate(date: String) = """{"cohort-selection":{"accommodation-start-date":{"accommodationStartDate":"$date"}}}"""
}
