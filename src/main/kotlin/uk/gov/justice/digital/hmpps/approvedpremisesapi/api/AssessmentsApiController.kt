package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import java.util.Optional
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@jakarta.annotation.Generated(
  value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"],
  comments = "Generator version: 7.13.0",
)@Controller
@RequestMapping("\${openapi.approvedPremises.base-path:}")
class AssessmentsApiController(
  delegate: AssessmentsApiDelegate?,
) : AssessmentsApi {
  private lateinit var delegate: AssessmentsApiDelegate

  init {
    this.delegate = Optional.ofNullable(delegate).orElse(object : AssessmentsApiDelegate {})
  }

  override fun getDelegate(): AssessmentsApiDelegate = delegate
}
