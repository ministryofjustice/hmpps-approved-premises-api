package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.stereotype.Controller
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.DocumentsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas1.Cas1DocumentsController
import java.util.UUID

@Controller
class DocumentsController(
  private val cas1DocumentsController: Cas1DocumentsController,
) : DocumentsApiDelegate {
  override fun documentsCrnDocumentIdGet(crn: String, documentId: UUID) = cas1DocumentsController.getDocument(crn, documentId)
}
