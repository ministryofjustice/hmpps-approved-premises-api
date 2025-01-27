package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.setup

import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import java.nio.file.Files
import java.nio.file.Paths

fun putMessageOnQueue(client: SqsAsyncClient, queueUrl: String, message: String) {
  val sendMessageRequest = SendMessageRequest.builder().queueUrl(queueUrl).messageBody(message).build()
  client.sendMessage(sendMessageRequest).get()
}

fun putWantedMessageOnQueue(client: SqsAsyncClient, queueUrl: String) {
  val message = getMessage()
  putMessageOnQueue(client, queueUrl, message)
}

private fun getMessage(): String {
  return Files.readString(Paths.get("src/test/resources/fixtures/sqs/deallocation.json"))
}

fun putUnwantedMessageOnQueue(client: SqsAsyncClient, queueUrl: String) {
  val message = getUnwantedMessage()
  putMessageOnQueue(client, queueUrl, message)
}

private fun getUnwantedMessage(): String {
  return Files.readString(Paths.get("src/test/resources/fixtures/sqs/notdeallocation.json"))
}
