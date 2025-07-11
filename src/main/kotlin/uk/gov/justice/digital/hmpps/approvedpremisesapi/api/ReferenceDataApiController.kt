package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@Controller
@RequestMapping("\${openapi.approvedPremises.base-path:}")
class ReferenceDataApiController(
  delegate: ReferenceDataApiDelegate?,
) : ReferenceDataApi {
  private lateinit var delegate: ReferenceDataApiDelegate

  init {
    this.delegate = Optional.ofNullable(delegate).orElse(object : ReferenceDataApiDelegate {})
  }

  override fun getDelegate(): ReferenceDataApiDelegate = delegate
}
