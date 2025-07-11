package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@jakarta.annotation.Generated(
  value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"],
  comments = "Generator version: 7.13.0",
)
@Controller
@RequestMapping("\${openapi.communityAccommodationServicesApprovedPremisesCAS1.base-path:/cas1}")
class PlacementRequestsCas1Controller(
  delegate: PlacementRequestsCas1Delegate?,
) : PlacementRequestsCas1 {
  private lateinit var delegate: PlacementRequestsCas1Delegate

  init {
    this.delegate = Optional.ofNullable(delegate).orElse(object : PlacementRequestsCas1Delegate {})
  }

  override fun getDelegate(): PlacementRequestsCas1Delegate = delegate
}
