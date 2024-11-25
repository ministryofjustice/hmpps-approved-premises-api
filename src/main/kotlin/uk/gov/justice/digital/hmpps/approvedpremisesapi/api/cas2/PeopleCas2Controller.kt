package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.7.0")
@Controller
@RequestMapping("\${openapi.communityAccommodationServicesTier2CAS2.base-path:/cas2}")
class PeopleCas2Controller(
  @org.springframework.beans.factory.annotation.Autowired(required = false) delegate: PeopleCas2Delegate?,
) : PeopleCas2 {
  private lateinit var delegate: PeopleCas2Delegate

  init {
    this.delegate = Optional.ofNullable(delegate).orElse(object : PeopleCas2Delegate {})
  }

  override fun getDelegate(): PeopleCas2Delegate = delegate
}
