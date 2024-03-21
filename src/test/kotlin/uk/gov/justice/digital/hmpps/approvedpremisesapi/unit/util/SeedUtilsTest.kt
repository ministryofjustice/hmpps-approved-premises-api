package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util

import io.mockk.every
import io.mockk.mockkStatic
import org.assertj.core.api.Assertions
import org.json.JSONObject
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.insertHdcDates
import java.time.OffsetDateTime
import java.time.ZoneOffset

class SeedUtilsTest {

  @Nested
  inner class InsertHdcDates {
    @Test
    fun `inserts HDC dates into JSON string`() {
      mockkStatic(OffsetDateTime::class)
      every { OffsetDateTime.now() } returns OffsetDateTime.of(
        2024,
        1,
        1,
        1,
        1,
        1,
        1,
        ZoneOffset.UTC,
      )

      val output = insertHdcDates("{}")
      val dataJson = JSONObject(output)
      val hdcDates = dataJson.getJSONObject("hdc-licence-dates").getJSONObject("hdc-licence-dates")

      Assertions.assertThat(hdcDates.toString()).isEqualTo(
        JSONObject(
          mapOf(
            "hdcEligibilityDate" to "2023-11-23",
            "hdcEligibilityDate-year" to "2023",
            "hdcEligibilityDate-month" to "11",
            "hdcEligibilityDate-day" to "23",
            "conditionalReleaseDate" to "2024-01-23",
            "conditionalReleaseDate-year" to "2024",
            "conditionalReleaseDate-month" to "1",
            "conditionalReleaseDate-day" to "23",
          ),
        ).toString(),
      )
    }
  }
}
