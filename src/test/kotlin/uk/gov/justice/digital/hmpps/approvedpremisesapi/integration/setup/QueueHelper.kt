package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.setup

import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import java.nio.file.Files
import java.nio.file.Paths

fun putMessageOnQueue(client: SqsAsyncClient, queueUrl: String) {
  val message = getMessage()
  val sendMessageRequest = SendMessageRequest.builder().queueUrl(queueUrl).messageBody(message).build()
  client.sendMessage(sendMessageRequest).get()
}

private fun getMessage(): String {
  return Files.readString(Paths.get("src/test/resources/fixtures/sqs/deallocation.json"))
}
