package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@Controller
@RequestMapping("\${openapi.communityAccommodationServicesApprovedPremisesCAS1.base-path:/cas1}")
class PlacementApplicationsCas1Controller(
  delegate: PlacementApplicationsCas1Delegate?,
) : PlacementApplicationsCas1 {
  private lateinit var delegate: PlacementApplicationsCas1Delegate

  init {
    this.delegate = Optional.ofNullable(delegate).orElse(object : PlacementApplicationsCas1Delegate {})
  }

  override fun getDelegate(): PlacementApplicationsCas1Delegate = delegate
}
