package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.migration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.determineInOutStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InOutStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.PrisonPeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.PrisonerInPrisonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.SignificantMovements
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

class InmateStatusOnSubmissionMigrationJobTest {

  @Test
  fun `Determine In Out Status returns OUT if no Prison Periods provided`() {
    val result = determineInOutStatus(
      OffsetDateTime.now(),
      PrisonerInPrisonSummary(emptyList()),
    )

    assertThat(result).isEqualTo(InOutStatus.OUT)
  }

  @Test
  fun `Determine In Out Status returns OUT if date lies before Prison Periods`() {
    val submissionDateTime = offsetDateTimeUtc(2023)

    val result = determineInOutStatus(
      submissionDateTime,
      PrisonerInPrisonSummary(
        listOf(
          PrisonPeriod(
            entryDate = localDateTime(2023, 10, 13),
            releaseDate = localDateTime(2023, 10, 23),
            movementDates = emptyList(),
          ),
          PrisonPeriod(
            entryDate = localDateTime(2023, 11, 13),
            releaseDate = localDateTime(2023, 11, 23),
            movementDates = emptyList(),
          ),
        ),
      ),
    )

    assertThat(result).isEqualTo(InOutStatus.OUT)
  }

  @Test
  fun `Determine In Out Status returns OUT if date lies between Prison Periods`() {
    val submissionDateTime = offsetDateTimeUtc(2023, 10, 24)

    val result = determineInOutStatus(
      submissionDateTime,
      PrisonerInPrisonSummary(
        listOf(
          PrisonPeriod(
            entryDate = localDateTime(2023, 10, 13),
            releaseDate = localDateTime(2023, 10, 23),
            movementDates = emptyList(),
          ),
          PrisonPeriod(
            entryDate = localDateTime(2023, 11, 13),
            releaseDate = localDateTime(2023, 11, 23),
            movementDates = emptyList(),
          ),
        ),
      ),
    )

    assertThat(result).isEqualTo(InOutStatus.OUT)
  }

  @Test
  fun `Determine In Out Status returns OUT if date lies after Prison Periods`() {
    val submissionDateTime = offsetDateTimeUtc(2023, 11, 24)

    val result = determineInOutStatus(
      submissionDateTime,
      PrisonerInPrisonSummary(
        listOf(
          PrisonPeriod(
            entryDate = localDateTime(2023, 10, 13),
            releaseDate = localDateTime(2023, 10, 23),
            movementDates = emptyList(),
          ),
          PrisonPeriod(
            entryDate = localDateTime(2023, 11, 13),
            releaseDate = localDateTime(2023, 11, 23),
            movementDates = emptyList(),
          ),
        ),
      ),
    )

    assertThat(result).isEqualTo(InOutStatus.OUT)
  }

  @Test
  fun `Determine In Out Status returns IN if date lies within first prison period with no movements other than entry is defined`() {
    val submissionDateTime = offsetDateTimeUtc(2023, 11, 24)

    val result = determineInOutStatus(
      submissionDateTime,
      PrisonerInPrisonSummary(
        listOf(
          PrisonPeriod(
            entryDate = localDateTime(2023, 10, 13),
            releaseDate = null,
            movementDates = listOf(
              SignificantMovements(
                dateInToPrison = localDateTime(2023, 10, 13),
              ),
            ),
          ),
          PrisonPeriod(
            entryDate = localDateTime(2023, 11, 13),
            releaseDate = localDateTime(2023, 11, 23),
            movementDates = emptyList(),
          ),
        ),
      ),
    )

    assertThat(result).isEqualTo(InOutStatus.IN)
  }

  @Test
  fun `Determine In Out Status returns IN if date lies within last prison period with no movements other than entry is defined`() {
    val submissionDateTime = offsetDateTimeUtc(2023, 11, 24)

    val result = determineInOutStatus(
      submissionDateTime,
      PrisonerInPrisonSummary(
        listOf(
          PrisonPeriod(
            entryDate = localDateTime(2023, 10, 13),
            releaseDate = null,
            movementDates = listOf(
              SignificantMovements(
                dateInToPrison = localDateTime(2023, 10, 13),
              ),
            ),
          ),
          PrisonPeriod(
            entryDate = localDateTime(2023, 10, 13),
            releaseDate = localDateTime(2023, 11, 10),
            movementDates = emptyList(),
          ),
          PrisonPeriod(
            entryDate = localDateTime(2023, 11, 13),
            releaseDate = localDateTime(2023, 11, 23),
            movementDates = emptyList(),
          ),
        ),
      ),
    )

    assertThat(result).isEqualTo(InOutStatus.IN)
  }

  @Test
  fun `Determine In Out Status returns IN if date lies within prison period's first stay`() {
    val submissionDateTime = offsetDateTimeUtc(2023, 11, 24)

    val result = determineInOutStatus(
      submissionDateTime,
      PrisonerInPrisonSummary(
        listOf(
          PrisonPeriod(
            entryDate = localDateTime(2023, 10, 13),
            releaseDate = localDateTime(2023, 12, 25),
            movementDates = listOf(
              SignificantMovements(
                dateInToPrison = localDateTime(2023, 10, 13),
                dateOutOfPrison = localDateTime(2023, 11, 25),
              ),
              SignificantMovements(
                dateInToPrison = localDateTime(2023, 12, 13),
                dateOutOfPrison = localDateTime(2023, 12, 25),
              ),
            ),
          ),
          PrisonPeriod(
            entryDate = localDateTime(2024, 11, 13),
            releaseDate = localDateTime(2024, 11, 23),
            movementDates = emptyList(),
          ),
        ),
      ),
    )

    assertThat(result).isEqualTo(InOutStatus.IN)
  }

  @Test
  fun `Determine In Out Status returns IN if date lies within prison period's second stay`() {
    val submissionDateTime = offsetDateTimeUtc(2023, 11, 24)

    val result = determineInOutStatus(
      submissionDateTime,
      PrisonerInPrisonSummary(
        listOf(
          PrisonPeriod(
            entryDate = localDateTime(2023, 10, 13),
            releaseDate = localDateTime(2023, 12, 25),
            movementDates = listOf(
              SignificantMovements(
                dateInToPrison = localDateTime(2023, 9, 13),
                dateOutOfPrison = localDateTime(2023, 9, 25),
              ),
              SignificantMovements(
                dateInToPrison = localDateTime(2023, 10, 13),
                dateOutOfPrison = localDateTime(2023, 11, 25),
              ),
              SignificantMovements(
                dateInToPrison = localDateTime(2023, 12, 13),
                dateOutOfPrison = localDateTime(2023, 12, 25),
              ),
            ),
          ),
          PrisonPeriod(
            entryDate = localDateTime(2024, 11, 13),
            releaseDate = localDateTime(2024, 11, 23),
            movementDates = emptyList(),
          ),
        ),
      ),
    )

    assertThat(result).isEqualTo(InOutStatus.IN)
  }

  @Test
  fun `Determine In Out Status returns IN if date lies within prison's periods last stay`() {
    val submissionDateTime = offsetDateTimeUtc(2024, 11, 24)

    val result = determineInOutStatus(
      submissionDateTime,
      PrisonerInPrisonSummary(
        listOf(
          PrisonPeriod(
            entryDate = localDateTime(2023, 6, 13),
            releaseDate = localDateTime(2023, 7, 13),
            movementDates = listOf(),
          ),
          PrisonPeriod(
            entryDate = localDateTime(2023, 8, 13),
            releaseDate = null,
            movementDates = listOf(
              SignificantMovements(
                dateInToPrison = localDateTime(2023, 8, 13),
                dateOutOfPrison = localDateTime(2023, 8, 25),
              ),
              SignificantMovements(
                dateInToPrison = localDateTime(2023, 9, 13),
                dateOutOfPrison = localDateTime(2023, 9, 25),
              ),
              SignificantMovements(
                dateInToPrison = localDateTime(2023, 10, 13),
              ),
            ),
          ),
        ),
      ),
    )

    assertThat(result).isEqualTo(InOutStatus.IN)
  }

  @Test
  fun `Determine In Out Status returns OUT if date lies between prison period's stays`() {
    val submissionDateTime = offsetDateTimeUtc(2023, 9, 26)

    val result = determineInOutStatus(
      submissionDateTime,
      PrisonerInPrisonSummary(
        listOf(
          PrisonPeriod(
            entryDate = localDateTime(2023, 10, 13),
            releaseDate = null,
            movementDates = listOf(
              SignificantMovements(
                dateInToPrison = localDateTime(2023, 8, 13),
                dateOutOfPrison = localDateTime(2023, 8, 25),
              ),
              SignificantMovements(
                dateInToPrison = localDateTime(2023, 9, 13),
                dateOutOfPrison = localDateTime(2023, 9, 25),
              ),
              SignificantMovements(
                dateInToPrison = localDateTime(2023, 10, 13),
              ),
            ),
          ),
        ),
      ),
    )

    assertThat(result).isEqualTo(InOutStatus.OUT)
  }

  private fun offsetDateTimeUtc(year: Int = 2023, month: Int = 1, day: Int = 1): OffsetDateTime =
    OffsetDateTime.of(year, month, day, 0, 0, 0, 0, ZoneOffset.UTC)

  private fun localDateTime(year: Int = 2023, month: Int = 1, day: Int = 1): LocalDateTime =
    LocalDateTime.of(year, month, day, 0, 0)
}
