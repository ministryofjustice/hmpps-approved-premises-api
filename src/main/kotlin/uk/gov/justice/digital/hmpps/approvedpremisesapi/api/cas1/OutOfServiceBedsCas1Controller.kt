package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@Controller
@RequestMapping("\${openapi.communityAccommodationServicesApprovedPremisesCAS1.base-path:/cas1}")
class OutOfServiceBedsCas1Controller(
  delegate: OutOfServiceBedsCas1Delegate?,
) : OutOfServiceBedsCas1 {
  private lateinit var delegate: OutOfServiceBedsCas1Delegate

  init {
    this.delegate = Optional.ofNullable(delegate).orElse(object : OutOfServiceBedsCas1Delegate {})
  }

  override fun getDelegate(): OutOfServiceBedsCas1Delegate = delegate
}
