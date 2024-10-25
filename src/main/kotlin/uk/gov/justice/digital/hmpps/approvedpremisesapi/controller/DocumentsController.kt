package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.DocumentsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.GroupedDocuments
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.APDeliusDocument
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.FeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import java.io.OutputStream
import java.util.UUID

@Controller
class DocumentsController(
  private val userService: UserService,
  private val offenderService: OffenderService,
  private val featureFlagService: FeatureFlagService,
) : DocumentsApiDelegate {

  override fun documentsCrnDocumentIdGet(crn: String, documentId: UUID): ResponseEntity<StreamingResponseBody> {
    val user = userService.getUserForRequest()

    val offenderDetailsResult = offenderService.getOffenderByCrn(crn, user.deliusUsername, user.hasQualification(UserQualification.LAO))

    when (offenderDetailsResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Person")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> Unit
    }

    val documentsFromApDeliusFlag = featureFlagService.getBooleanFlag("get-documents-from-ap-delius")
    val documentsMetaData = when (val documentsMetaDataResult = getDocuments(isDocumentFromAPDelius = documentsFromApDeliusFlag, crn)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Documents")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> documentsMetaDataResult.entity
    }

    val documentFilename = getDocumentFileName(documentsMetaData, documentId)
    return ResponseEntity(
      StreamingResponseBody { outputStream ->
        getDocument(isDocumentFromAPDelius = documentsFromApDeliusFlag, crn, documentId, outputStream)
      },
      HttpHeaders().apply {
        put("Content-Disposition", listOf("attachment; filename=\"$documentFilename\""))
      },
      HttpStatus.OK,
    )
  }

  private fun getDocument(isDocumentFromAPDelius: Boolean, crn: String, documentId: UUID, outputStream: OutputStream) = if (isDocumentFromAPDelius) {
    offenderService.getDocumentFromDelius(
      crn,
      documentId.toString(),
      outputStream,
    )
  } else {
    offenderService.getDocumentFromCommunityApi(
      crn,
      documentId.toString(),
      outputStream,
    )
  }

  private fun getDocuments(isDocumentFromAPDelius: Boolean, crn: String) = if (isDocumentFromAPDelius) {
    offenderService.getDocumentsFromApDeliusApi(crn)
  } else {
    offenderService.getDocumentsFromCommunityApi(crn)
  }

  private fun getDocumentFileName(documentsMetaData: Any, documentId: UUID): String? = when (documentsMetaData) {
    is GroupedDocuments -> getDocumentFileName(documentsMetaData, documentId)
    is List<*> -> getDocumentFileName(documentsMetaData, documentId)
    else -> null
  }

  private fun getDocumentFileName(documentsMetaData: List<*>, documentId: UUID): String {
    val doc = documentsMetaData.firstOrNull { doc -> (doc as APDeliusDocument).id == documentId.toString() } as APDeliusDocument?
      ?: throw NotFoundProblem(documentId, "Document")
    return doc.filename
  }

  private fun getDocumentFileName(documentsMetaData: GroupedDocuments, documentId: UUID): String? {
    val documentMetaData = documentsMetaData.findDocument(documentId.toString())
      ?: throw NotFoundProblem(documentId, "Document")
    return documentMetaData.documentName
  }
}
