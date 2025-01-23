package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonAcctAlert
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PrisonerAlertFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PrisonerAlertTransformer

class PrisonerAlertTransformerTest {
  private val prisonerAlertTransformer = PrisonerAlertTransformer()

  @Test
  fun `transforms Alert to PersonAcctAlert api representation`() {
    val alert = PrisonerAlertFactory().produce()

    val expectedPersonAcctAlert = PersonAcctAlert(
      alertId = 0,
      comment = alert.description,
      description = alert.description,
      dateCreated = alert.activeFrom,
      dateExpires = alert.activeTo,
      alertTypeDescription = alert.alertCode.alertTypeDescription,
    )

    val transformation = prisonerAlertTransformer.transformToApi(alert)

    assertThat(transformation).isEqualTo(expectedPersonAcctAlert)
  }
}
