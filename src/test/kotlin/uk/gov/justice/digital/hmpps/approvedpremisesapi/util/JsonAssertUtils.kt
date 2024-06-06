package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

fun assertJsonEquals(
  expected: String,
  actual: String,
) {
  JSONAssert.assertEquals(
    expected.trimMargin(),
    actual,
    JSONCompareMode.NON_EXTENSIBLE,
  )
}
