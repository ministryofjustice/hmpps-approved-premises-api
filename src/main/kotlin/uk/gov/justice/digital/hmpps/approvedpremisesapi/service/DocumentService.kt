package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.APDeliusDocument
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import java.io.OutputStream

@Service
class DocumentService(
  val apDeliusContextApiClient: ApDeliusContextApiClient,
) {
  fun getDocumentsFromApDeliusApi(crn: String): CasResult<List<APDeliusDocument>> {
    when (val documentsResult = apDeliusContextApiClient.getDocuments(crn)) {
      is ClientResult.Success -> return CasResult.Success(documentsResult.body)
      is ClientResult.Failure.StatusCode -> when (documentsResult.status) {
        HttpStatus.NOT_FOUND -> return CasResult.NotFound("Documents", crn)
        HttpStatus.FORBIDDEN -> return CasResult.Unauthorised()
        else -> documentsResult.throwException()
      }
      is ClientResult.Failure -> documentsResult.throwException()
    }
  }

  fun getDocumentFromDelius(
    crn: String,
    documentId: String,
    outputStream: OutputStream,
  ) = apDeliusContextApiClient.getDocument(crn, documentId, outputStream)
}
