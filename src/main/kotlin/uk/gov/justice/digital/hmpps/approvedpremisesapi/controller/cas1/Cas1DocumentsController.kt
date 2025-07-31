package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.APDeliusDocument
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DocumentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.io.OutputStream
import java.util.UUID

@Cas1Controller
@Tag(name = "CAS1 Documents")
class Cas1DocumentsController(
  private val userService: UserService,
  private val offenderService: OffenderService,
  private val documentService: DocumentService,
) {

  @Operation(summary = "Downloads a document")
  @GetMapping(
    value = ["/documents/{crn}/{documentId}"],
    produces = ["application/octet-stream", "application/json"],
  )
  fun getDocument(
    @PathVariable crn: String,
    @PathVariable documentId: UUID,
  ): ResponseEntity<StreamingResponseBody> {
    val user = userService.getUserForRequest()

    getOffenderDetails(crn, user)

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

  private fun getOffenderDetails(crn: String, user: UserEntity) {
    val offenderDetailsResult =
      offenderService.getOffenderByCrn(crn, user.deliusUsername, user.hasQualification(UserQualification.LAO))

    when (offenderDetailsResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Person")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> Unit
    }
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
