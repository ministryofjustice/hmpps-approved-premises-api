package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.ContentDisposition
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.APDeliusDocumentFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.InitialiseDatabasePerClassTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulDocumentDownloadCall
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextMockSuccessfulDocumentsCall
import java.util.UUID

class Cas1DocumentTest : InitialiseDatabasePerClassTestBase() {

  @Nested
  inner class GetDocument {

    @Test
    fun `Returns 404 when document doesn't exist for CRN`() {
      val (_, jwt) = givenAUser()
      val (offenderDetails, _) = givenAnOffender()

      apDeliusContextMockSuccessfulDocumentsCall(
        crn = offenderDetails.otherIds.crn,
        documents = listOf(
          APDeliusDocumentFactory().produce(),
          APDeliusDocumentFactory().produce(),
          APDeliusDocumentFactory().produce(),
        ),
      )

      val nonExistentDocumentId = UUID.randomUUID()
      webTestClient.get()
        .uri("/cas1/documents/${offenderDetails.otherIds.crn}/$nonExistentDocumentId")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun success() {
      val (_, jwt) = givenAUser()
      val (offenderDetails, _) = givenAnOffender()

      val requestedDocumentId = UUID.randomUUID()
      val docFileContents = "I AM A MOCK DOCUMENT".byteInputStream().readAllBytes()

      val requestedDocument = APDeliusDocumentFactory()
        .withId(requestedDocumentId.toString())
        .withFilename("conviction_level_doc.pdf")
        .produce()

      val otherDocument = APDeliusDocumentFactory()
        .withId(UUID.randomUUID().toString())
        .withFilename("offender_level_doc.pdf")
        .produce()

      apDeliusContextMockSuccessfulDocumentsCall(offenderDetails.otherIds.crn, listOf(otherDocument, requestedDocument))
      apDeliusContextMockSuccessfulDocumentDownloadCall(offenderDetails.otherIds.crn, requestedDocumentId, docFileContents)

      val result = webTestClient.get()
        .uri("/cas1/documents/${offenderDetails.otherIds.crn}/$requestedDocumentId")
        .header("Authorization", "Bearer $jwt")
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader()
        .contentDisposition(ContentDisposition.parse("attachment; filename=\"conviction_level_doc.pdf\""))
        .expectBody()
        .returnResult()

      assertThat(result.responseBody).isEqualTo(docFileContents)
    }
  }
}
