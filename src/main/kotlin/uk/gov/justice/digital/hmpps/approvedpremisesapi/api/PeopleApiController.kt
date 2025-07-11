package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import java.util.Optional
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@jakarta.annotation.Generated(
  value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"],
  comments = "Generator version: 7.13.0",
)@Controller
@RequestMapping("\${openapi.approvedPremises.base-path:}")
class PeopleApiController(
  delegate: PeopleApiDelegate?,
) : PeopleApi {
  private lateinit var delegate: PeopleApiDelegate

  init {
    this.delegate = Optional.ofNullable(delegate).orElse(object : PeopleApiDelegate {})
  }

  override fun getDelegate(): PeopleApiDelegate = delegate
}
