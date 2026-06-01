package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service

import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.mocks.ClockConfiguration
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OASysSuitabilityService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService
import java.time.OffsetDateTime

@ExtendWith(MockKExtension::class)
class OASysSuitabilityServiceTest {
  private val clock = ClockConfiguration.MutableClock()

  @RelaxedMockK
  private lateinit var sentryService: SentryService

  @InjectMockKs
  private lateinit var service: OASysSuitabilityService

  companion object {
    const val CRN = "CRN1122"
  }

  @Nested
  inner class IsSuitable {

    @ParameterizedTest
    @CsvSource(
      // in the future (shouldn't really happen, but should allow)
      "2010-07-02T11:00:00+00:00,true",
      // very recent
      "2010-07-01T11:00:00+00:00,true",
      // 3 months old
      "2010-04-01T12:00:00+00:00,true",
      // 5 months old
      "2010-02-01T12:00:00+00:00,true",
      // just under 6 months old
      "2010-01-01T13:00:00+00:00,true",
      // just over 6 months old
      "2010-01-01T11:00:00+00:00,false",
      // 7 months old
      "2009-12-01T12:00:00+00:00,false",
      // 2 years old
      "2008-07-01T12:00:00+00:00,false",
    )
    fun `completion defined`(
      completionDateTime: OffsetDateTime,
      isUsable: Boolean,
    ) {
      clock.setNow(OffsetDateTime.parse("2010-07-01T12:00:00+00:00"))

      val result = service.isSuitable(
        OASysSuitabilityService.OASysAssessmentDates(
          crn = CRN,
          initiationDate = completionDateTime.minusMonths(12),
          dateCompleted = completionDateTime,
        ),
      )

      assertThat(result).isEqualTo(isUsable)

      if (!isUsable) {
        verify {
          sentryService.captureErrorMessage("Have received an assessment with a completion date/time more than 6 months ago for CRN1122 and date/time $completionDateTime")
        }
      }
    }

    @ParameterizedTest
    @CsvSource(
      // in the future (shouldn't really happen, but should allow)
      "2010-07-02T11:00:00+00:00,true",
      // very recent
      "2010-07-01T11:00:00+00:00,true",
      // 3 months old
      "2010-04-01T12:00:00+00:00,true",
      // 5 months old
      "2010-02-01T12:00:00+00:00,true",
      // just under 6 months old
      "2010-01-01T13:00:00+00:00,true",
      // just over 6 months old
      "2010-01-01T11:00:00+00:00,false",
      // 7 months old
      "2009-12-01T12:00:00+00:00,false",
      // 2 years old
      "2008-07-01T12:00:00+00:00,false",
    )
    fun `if completion isn't defined fall back to initiation date and raise an alert`(
      initiationDate: OffsetDateTime,
      isUsable: Boolean,
    ) {
      clock.setNow(OffsetDateTime.parse("2010-07-01T12:00:00+00:00"))

      val result = service.isSuitable(
        OASysSuitabilityService.OASysAssessmentDates(
          crn = CRN,
          initiationDate = initiationDate,
          dateCompleted = null,
        ),
      )

      assertThat(result).isEqualTo(isUsable)

      verify {
        sentryService.captureErrorMessage("No completion date defined on assessment for CRN1122. Using initiation date/time of $initiationDate")
      }
    }
  }
}
