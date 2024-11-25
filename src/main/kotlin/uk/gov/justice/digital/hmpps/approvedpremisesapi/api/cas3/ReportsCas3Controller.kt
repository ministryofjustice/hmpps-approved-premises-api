package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.7.0")
@Controller
@RequestMapping("\${openapi.transitionalAccommodationServicesCAS3.base-path:/cas3}")
class ReportsCas3Controller(
  @org.springframework.beans.factory.annotation.Autowired(required = false) delegate: ReportsCas3Delegate?,
) : ReportsCas3 {
  private lateinit var delegate: ReportsCas3Delegate

  init {
    this.delegate = Optional.ofNullable(delegate).orElse(object : ReportsCas3Delegate {})
  }

  override fun getDelegate(): ReportsCas3Delegate = delegate
}
