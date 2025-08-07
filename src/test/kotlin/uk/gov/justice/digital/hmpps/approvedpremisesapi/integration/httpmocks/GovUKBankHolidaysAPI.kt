package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks

import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.bankholidaysapi.CountryBankHolidays
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.bankholidaysapi.UKBankHolidays
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase

fun IntegrationTestBase.govUKBankHolidaysAPIMockSuccessfulCall(bankHolidays: UKBankHolidays) = mockSuccessfulGetCallWithJsonResponse(
  url = "/bank-holidays.json",
  responseBody = bankHolidays,
)

fun IntegrationTestBase.govUKBankHolidaysAPIMockSuccessfullCallWithEmptyResponse() = govUKBankHolidaysAPIMockSuccessfulCall(
  UKBankHolidays(
    englandAndWales = CountryBankHolidays("england-and-wales", listOf()),
    scotland = CountryBankHolidays("scotland", listOf()),
    northernIreland = CountryBankHolidays("northern-ireland", listOf()),
  ),
)
