package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import org.slf4j.LoggerFactory
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
  var emailAddresses: EmailAddressConfig = EmailAddressConfig()
}

class EmailAddressConfig {
  var cas2Assessors: String = "example@example.com"
  var cas2ReplyToId: String = "cbe00c2d-387b-4283-9b9c-13c8b7a61444"
}

class NotifyTemplates {
  val applicationSubmitted = "c9944bd8-63c4-473c-8dce-b3636e47d3dd"
  val applicationWithdrawn = "ad6d2449-f5a1-432a-9a96-0835c3f92dad"
  val assessmentAllocated = "2edd59f9-0013-4fbf-91df-7421518b447d"
  val assessmentDeallocated = "331ce452-ea83-4f0c-aec0-5eafe85094f2"
  val appealedAssessmentAllocated = "d378ad90-c20b-49d9-bd8b-5a50e1416dea"
  val assessmentAccepted = "ddf87b15-8866-4bad-a87b-47eba69eb6db"
  val assessmentRejected = "b3a98c60-8fe0-4450-8fd0-6430198ee43b"
  val assessmentWithdrawn = "44ade006-7ac6-4769-aa40-542da56f21b5"
  val bookingMade = "1e3d2ee2-250e-4755-af38-80d24cdc3480"
  val bookingMadePremises = "337bb149-6f12-4be2-b5a3-a9a73d73c1e1"
  val bookingWithdrawn = "30cdc876-40a6-41b0-b642-a6b6115c835c"
  val matchRequestWithdrawn = "bb5c68d2-cea1-4924-ba3a-3a8e2beeb640"
  val placementRequestAllocated = "375d83be-c973-44ed-939f-48ffc00230f3"
  val placementRequestDecisionAccepted = "dd6f7526-05ce-4951-ba98-a2e68962fb43"
  val placementRequestDecisionRejected = "b258a025-d1e8-47f2-833e-641d7c119ff5"
  val placementRequestSubmitted = "deb11bc6-d424-4370-bbe5-41f6a823d292"
  val placementRequestSubmittedV2 = "e7e5b481-aca8-4930-bf4e-b3098834e840"
  val placementRequestWithdrawn = "a5f44549-e849-4a26-abb1-802316081533"
  val cas2ApplicationSubmitted = "a0823218-91dd-4cf0-9835-4b90024f62c8"
  val cas2NoteAddedForReferrer = "debe17a2-9f79-4d26-88a0-690dd73e2a5b"
  val cas2NoteAddedForAssessor = "0d646bf0-d40f-4fe7-aa74-dd28b10d04f1"
  val cas2ApplicationStatusUpdated = "ef4dc5e3-b1f1-4448-a545-7a936c50fc3a"
  val appealSuccess = "ae21f3ae-3a4a-4df8-a1b8-2640ea80d101"
  val appealReject = "32c9c282-198d-4d43-960e-89c97f1bcf81"
}

enum class NotifyMode {
  DISABLED, TEST_AND_GUEST_LIST, ENABLED
}

@Configuration
class NotifyClientConfig {
  private val log = LoggerFactory.getLogger(this::class.java)

  @Bean("normalNotificationClient")
  fun normalNotificationClient(notifyConfig: NotifyConfig) = if (notifyConfig.mode != NotifyMode.DISABLED) {
    log.info("Notify Api Key secret is: " + notifyConfig.apiKey?.length + " characters")
    val client = NotificationClient(notifyConfig.apiKey)
    log.info("Notify Api Service Id is: " + client.serviceId)
    client
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
