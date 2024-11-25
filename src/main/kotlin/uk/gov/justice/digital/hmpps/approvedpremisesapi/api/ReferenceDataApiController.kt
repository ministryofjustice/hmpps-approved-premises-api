package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.7.0")
@Controller
@RequestMapping("\${openapi.approvedPremises.base-path:}")
class ReferenceDataApiController(
  @org.springframework.beans.factory.annotation.Autowired(required = false) delegate: ReferenceDataApiDelegate?,
) : ReferenceDataApi {
  private lateinit var delegate: ReferenceDataApiDelegate

  init {
    this.delegate = Optional.ofNullable(delegate).orElse(object : ReferenceDataApiDelegate {})
  }

  override fun getDelegate(): ReferenceDataApiDelegate = delegate
}
