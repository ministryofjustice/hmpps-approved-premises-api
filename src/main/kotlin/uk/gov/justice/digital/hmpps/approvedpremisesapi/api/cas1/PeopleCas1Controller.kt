package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@Controller
@RequestMapping("\${openapi.communityAccommodationServicesApprovedPremisesCAS1.base-path:/cas1}")
class PeopleCas1Controller(
  delegate: PeopleCas1Delegate?,
) : PeopleCas1 {
  private lateinit var delegate: PeopleCas1Delegate

  init {
    this.delegate = Optional.ofNullable(delegate).orElse(object : PeopleCas1Delegate {})
  }

  override fun getDelegate(): PeopleCas1Delegate = delegate
}
