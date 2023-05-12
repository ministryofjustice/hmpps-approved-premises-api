package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "notify")
class NotifyConfig {
  var mode: NotifyMode = NotifyMode.DISABLED
  var apiKey: String? = null
  var guestListApiKey: String? = null
  var templates: NotifyTemplates = NotifyTemplates()
}

class NotifyTemplates {

}

enum class NotifyMode {
  DISABLED, TEST_AND_GUEST_LIST, ENABLED
}
