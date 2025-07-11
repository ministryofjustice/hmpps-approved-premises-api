package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@Controller
@RequestMapping("\${openapi.approvedPremises.base-path:}")
class DocumentsApiController(
  delegate: DocumentsApiDelegate?,
) : DocumentsApi {
  private lateinit var delegate: DocumentsApiDelegate

  init {
    this.delegate = Optional.ofNullable(delegate).orElse(object : DocumentsApiDelegate {})
  }

  override fun getDelegate(): DocumentsApiDelegate = delegate
}
