package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.bankholidaysapi.CountryBankHolidays
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.bankholidaysapi.UKBankHolidays

fun IntegrationTestBase.govUKBankHolidaysApiMockSuccessfulCall(bankHolidays: UKBankHolidays) =
  mockSuccessfulGetCallWithJsonResponse(
    url = "/bank-holidays.json",
    responseBody = bankHolidays,
  )

fun IntegrationTestBase.govUKBankHolidaysApiMockSuccessfullCallWithEmptyResponse() =
  govUKBankHolidaysApiMockSuccessfulCall(
    UKBankHolidays(
      englandAndWales = CountryBankHolidays("england-and-wales", listOf()),
      scotland = CountryBankHolidays("scotland", listOf()),
      northernIreland = CountryBankHolidays("northern-ireland", listOf()),
    ),
  )
