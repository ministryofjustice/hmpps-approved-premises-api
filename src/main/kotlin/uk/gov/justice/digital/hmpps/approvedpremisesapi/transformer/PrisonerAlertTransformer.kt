package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonAcctAlert
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisoneralertsapi.Alert

@Component
class PrisonerAlertTransformer {
  fun transformToApi(alert: Alert) = PersonAcctAlert(
    alertId = 0,
    comment = alert.description,
    description = alert.description,
    dateCreated = alert.activeFrom,
    dateExpires = alert.activeTo,
    alertTypeDescription = alert.alertCode.alertTypeDescription,
  )
}
