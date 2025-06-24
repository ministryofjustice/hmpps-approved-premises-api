package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.DocumentsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.APDeliusDocument
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DocumentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserAccessService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1LaoStrategy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.io.OutputStream
import java.util.UUID

@Controller
class DocumentsController(
  private val userService: UserService,
  private val userAccessService: UserAccessService,
  private val documentService: DocumentService,
) : DocumentsApiDelegate {

  override fun documentsCrnDocumentIdGet(crn: String, documentId: UUID): ResponseEntity<StreamingResponseBody> {
    val user = userService.getUserForRequest()

    userAccessService.ensureUserCanAccessOffender(
      crn = crn,
      strategy = user.cas1LaoStrategy(),
      throwNotFound = true,
    )

    val documentsMetaData = getDocuments(crn)

    val documentFilename = getDocumentFileName(documentsMetaData, documentId)
    return ResponseEntity(
      StreamingResponseBody { outputStream ->
        getDocument(crn, documentId, outputStream)
      },
      HttpHeaders().apply {
        put("Content-Disposition", listOf("attachment; filename=\"$documentFilename\""))
      },
      HttpStatus.OK,
    )
  }

  private fun getDocument(crn: String, documentId: UUID, outputStream: OutputStream) {
    documentService.getDocumentFromDelius(crn, documentId.toString(), outputStream)
  }

  private fun getDocuments(crn: String): List<APDeliusDocument> = extractEntityFromCasResult(
    documentService.getDocumentsFromApDeliusApi(crn),
  )

  private fun getDocumentFileName(documentsMetaData: List<APDeliusDocument>, documentId: UUID): String {
    val document = documentsMetaData.firstOrNull { it.id == documentId.toString() } ?: throw NotFoundProblem(documentId, "Document")
    return document.filename
  }
}
