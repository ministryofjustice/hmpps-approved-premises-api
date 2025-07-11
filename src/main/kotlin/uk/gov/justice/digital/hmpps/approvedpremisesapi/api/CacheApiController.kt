package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import java.util.Optional
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@jakarta.annotation.Generated(
  value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"],
  comments = "Generator version: 7.13.0",
)@Controller
@RequestMapping("\${openapi.approvedPremises.base-path:}")
class CacheApiController(
  delegate: CacheApiDelegate?,
) : CacheApi {
  private lateinit var delegate: CacheApiDelegate

  init {
    this.delegate = Optional.ofNullable(delegate).orElse(object : CacheApiDelegate {})
  }

  override fun getDelegate(): CacheApiDelegate = delegate
}
