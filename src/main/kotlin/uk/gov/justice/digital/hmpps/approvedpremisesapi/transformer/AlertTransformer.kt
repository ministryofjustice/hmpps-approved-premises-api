package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonAcctAlert
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.Alert

@Component
class AlertTransformer {
  fun transformToApi(alert: Alert) = PersonAcctAlert(
    alertId = alert.alertId,
    comment = alert.comment,
    dateCreated = alert.dateCreated,
    dateExpires = alert.dateExpires,
    alertTypeDescription = alert.alertTypeDescription,
  )
}
