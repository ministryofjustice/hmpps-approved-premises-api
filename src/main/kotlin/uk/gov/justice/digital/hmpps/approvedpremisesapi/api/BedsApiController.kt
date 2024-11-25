package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.7.0")
@Controller
@RequestMapping("\${openapi.approvedPremises.base-path:}")
class BedsApiController(
  @org.springframework.beans.factory.annotation.Autowired(required = false) delegate: BedsApiDelegate?,
) : BedsApi {
  private lateinit var delegate: BedsApiDelegate

  init {
    this.delegate = Optional.ofNullable(delegate).orElse(object : BedsApiDelegate {})
  }

  override fun getDelegate(): BedsApiDelegate = delegate
}
