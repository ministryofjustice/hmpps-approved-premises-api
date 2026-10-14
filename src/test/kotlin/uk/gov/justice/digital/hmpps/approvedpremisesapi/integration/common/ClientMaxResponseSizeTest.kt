package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonerAlertsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.prisoneralertsapi.Alert
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PrisonerAlertFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.client.prisoneralerts.AlertsPageFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.prisonerAlertsAPIMockSuccessfulAlertsCall

class ClientMaxResponseSizeTest : InitialiseDatabasePerClassTestBase() {

  @Autowired
  lateinit var prisonerAlertsApiClient: PrisonerAlertsApiClient

  @Test
  fun `ensure responses just below max response buffer size work`() {
    val alerts = alertsForResponseSize(3)

    // We use the alert endpoint to test because this is one that typically
    // has large responses. We can't use the get document endpoint (which would
    // be the cleanest way to demonstrate the limit), because the
    // flux streaming implementation means there is no upper limit
    prisonerAlertsAPIMockSuccessfulAlertsCall(
      "NOMS123",
      "CODE1",
      AlertsPageFactory()
        .withContent(alerts)
        .produce(),
    )

    val response = prisonerAlertsApiClient.getAlerts("NOMS123", "CODE1")

    assertThat(response).isInstanceOf(ClientResult.Success::class.java)
    response as ClientResult.Success
    assertThat(response.body.content).hasSize(alerts.size)
  }

  @Test
  fun `ensure responses over max response buffer size fails`() {
    val alerts = alertsForResponseSize(4)

    prisonerAlertsAPIMockSuccessfulAlertsCall(
      "NOMS123",
      "CODE1",
      AlertsPageFactory()
        .withContent(alerts)
        .produce(),
    )

    try {
      prisonerAlertsApiClient.getAlerts("NOMS123", "CODE1")
    } catch (e: Exception) {
      assertThat(e.message).endsWith("org.springframework.core.io.buffer.DataBufferLimitException: Exceeded limit on max bytes to buffer : 3145728")
    }
  }

  private fun alertsForResponseSize(sizeInMb: Int): List<Alert> {
    val alert = PrisonerAlertFactory().produce()
    val alertByteSize = jsonMapper.writeValueAsString(alert).toByteArray().size

    val justUnderRequiredSize = (sizeInMb * 1024 * 1024) - 5_024
    val alertsRequiredToMeetLimit = justUnderRequiredSize / alertByteSize
    return List(alertsRequiredToMeetLimit) { alert }
  }
}
