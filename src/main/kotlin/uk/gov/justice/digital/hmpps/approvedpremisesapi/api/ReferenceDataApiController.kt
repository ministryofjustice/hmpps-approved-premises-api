package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import java.util.Optional
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@jakarta.annotation.Generated(
  value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"],
  comments = "Generator version: 7.13.0",
)@Controller
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
