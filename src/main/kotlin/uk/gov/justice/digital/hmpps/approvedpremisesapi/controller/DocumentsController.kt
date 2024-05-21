package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.DocumentsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import java.util.UUID

@Controller
class DocumentsController(
  private val userService: UserService,
  private val offenderService: OffenderService,
) : DocumentsApiDelegate {

  override fun documentsCrnDocumentIdGet(crn: String, documentId: UUID): ResponseEntity<StreamingResponseBody> {
    val user = userService.getUserForRequest()

    val offenderDetailsResult = offenderService.getOffenderByCrn(crn, user.deliusUsername, user.hasQualification(UserQualification.LAO))

    when (offenderDetailsResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Person")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> Unit
    }

    val documentsMetaDataResult = offenderService.getDocuments(crn)
    val documentsMetaData = when (documentsMetaDataResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Documents")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> documentsMetaDataResult.entity
    }

    val documentMetaData = documentsMetaData.findDocument(documentId.toString())
      ?: throw NotFoundProblem(documentId, "Document")

    return ResponseEntity(
      StreamingResponseBody { outputStream ->
        offenderService.getDocument(crn, documentId.toString(), outputStream)
      },
      HttpHeaders().apply {
        put("Content-Disposition", listOf("attachment; filename=\"${documentMetaData.documentName}\""))
      },
      HttpStatus.OK,
    )
  }
}
