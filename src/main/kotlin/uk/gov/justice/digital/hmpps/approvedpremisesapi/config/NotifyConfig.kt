package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import uk.gov.service.notify.NotificationClient

@Component
@ConfigurationProperties(prefix = "notify")
class NotifyConfig {
  var mode: NotifyMode = NotifyMode.DISABLED
  var apiKey: String? = null
  var guestListApiKey: String? = null
  var templates: NotifyTemplates = NotifyTemplates()
}

class NotifyTemplates {
  var applicationSubmitted = "c9944bd8-63c4-473c-8dce-b3636e47d3dd"
  var assessmentAllocated = "f3d78814-383f-4b5f-a681-9bd3ab912888"
  var assessmentDeallocated = "331ce452-ea83-4f0c-aec0-5eafe85094f2"
  var assessmentAccepted = "ddf87b15-8866-4bad-a87b-47eba69eb6db"
  var assessmentRejected = "b3a98c60-8fe0-4450-8fd0-6430198ee43b"
  var bookingMade = "1e3d2ee2-250e-4755-af38-80d24cdc3480"
}

enum class NotifyMode {
  DISABLED, TEST_AND_GUEST_LIST, ENABLED
}

@Configuration
class NotifyClientConfig {
  @Bean("normalNotificationClient")
  fun normalNotificationClient(notifyConfig: NotifyConfig) = if (notifyConfig.mode != NotifyMode.DISABLED) {
    NotificationClient(notifyConfig.apiKey)
  } else {
    null
  }

  @Bean("guestListNotificationClient")
  fun guestListNotificationClient(notifyConfig: NotifyConfig) = if (notifyConfig.mode == NotifyMode.TEST_AND_GUEST_LIST) {
    NotificationClient(notifyConfig.guestListApiKey)
  } else {
    null
  }
}
