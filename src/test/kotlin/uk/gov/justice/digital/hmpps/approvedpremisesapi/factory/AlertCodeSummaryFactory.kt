package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisoneralertsapi.AlertCodeSummary

class AlertCodeSummaryFactory {
  fun produce(
    alertTypeCode: String = "H",
    alertTypeDescription: String = "Self Harm",
    code: String = "HA",
    description: String = "ACCT Open (HMPS)",
  ): AlertCodeSummary = AlertCodeSummary(
    alertTypeCode = alertTypeCode,
    alertTypeDescription = alertTypeDescription,
    code = code,
    description = description,
  )
}
