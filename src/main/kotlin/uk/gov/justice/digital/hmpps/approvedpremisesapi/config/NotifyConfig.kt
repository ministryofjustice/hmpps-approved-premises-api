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
  val applicationWithdrawnV2 = "335b4d51-0be0-432e-9819-469d768d06d5"
  val assessmentAllocated = "2edd59f9-0013-4fbf-91df-7421518b447d"
  val assessmentDeallocated = "331ce452-ea83-4f0c-aec0-5eafe85094f2"
  val appealedAssessmentAllocated = "d378ad90-c20b-49d9-bd8b-5a50e1416dea"
  val assessmentAccepted = "ddf87b15-8866-4bad-a87b-47eba69eb6db"
  val assessmentRejected = "b3a98c60-8fe0-4450-8fd0-6430198ee43b"
  val assessmentWithdrawnV2 = "a43968bd-ec69-46a4-bb6f-aacb8eb51cf3"
  val bookingMade = "1e3d2ee2-250e-4755-af38-80d24cdc3480"
  val bookingMadePremises = "337bb149-6f12-4be2-b5a3-a9a73d73c1e1"
  val bookingWithdrawnV2 = "01324632-4450-4416-b48d-5b8fb8922d98"
  val matchRequestWithdrawnV2 = "d9830236-6546-4fb7-90b7-8c81ba278123"
  val placementRequestAllocatedV2 = "1321c66a-6429-47a7-942c-ce288f1a0648"
  val placementRequestDecisionAcceptedV2 = "ec3d3f8f-83de-4039-a184-0e5c7de1021f"
  val placementRequestDecisionRejectedV2 = "210e065a-29de-4740-80bf-3bc2ab5c1c84"
  val placementRequestSubmitted = "deb11bc6-d424-4370-bbe5-41f6a823d292"
  val placementRequestSubmittedV2 = "e7e5b481-aca8-4930-bf4e-b3098834e840"
  val placementRequestWithdrawnV2 = "58bda3a6-c091-4d78-a533-de5991777300"

  val cas2ApplicationSubmitted = "a0823218-91dd-4cf0-9835-4b90024f62c8"
  val cas2NoteAddedForReferrer = "debe17a2-9f79-4d26-88a0-690dd73e2a5b"
  val cas2NoteAddedForAssessor = "0d646bf0-d40f-4fe7-aa74-dd28b10d04f1"
  val cas2ApplicationStatusUpdated = "ef4dc5e3-b1f1-4448-a545-7a936c50fc3a"

  val cas2v2ApplicationSubmittedCourtBail = "e181a9d5-7ca2-491f-a563-ac9f3fac8777"
  val cas2v2ApplicationSubmittedPrisonBail = "f0faefb9-f300-4b67-ac0d-4b41c0b3f7bc"
  val cas2v2ApplicationStatusUpdatedCourtBail = "cec7e570-b3e3-4dd4-9a99-d03c1cc34655"
  val cas2v2ApplicationStatusUpdatedPrisonBail = "51a71a7a-a427-41bc-8fdd-77a73861880c"
  val cas2v2NoteAddedForReferrerCourtBail = "6a258c9b-cc0e-402e-af05-e52d94a45298"
  val cas2v2NoteAddedForReferrerPrisonBail = "b277e84a-c72b-4afa-a388-72a70e588fb2"
  val cas2v2NoteAddedForAssessorCourtBail = "df58e948-6672-4b0b-b6ab-2940b0c6ef22"
  val cas2v2NoteAddedForAssessorPrisonBail = "9a37fb66-5215-40f2-8fa4-210e2e27d693"

  val appealSuccess = "ae21f3ae-3a4a-4df8-a1b8-2640ea80d101"
  val appealReject = "32c9c282-198d-4d43-960e-89c97f1bcf81"
  val bookingAmended = "da50a791-fe7c-4bc3-b31f-62b88b62f2d6"

  val toTransferringPomApplicationTransferredToAnotherPrison = "5adb6390-0c95-4458-a8b5-3e61ff780715"
}

enum class NotifyMode {
  DISABLED,
  TEST_AND_GUEST_LIST,
  ENABLED,
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
