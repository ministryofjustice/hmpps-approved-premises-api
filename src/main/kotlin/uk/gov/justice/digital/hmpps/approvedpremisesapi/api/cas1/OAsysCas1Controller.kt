package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@Controller
@RequestMapping("\${openapi.communityAccommodationServicesApprovedPremisesCAS1.base-path:/cas1}")
class OAsysCas1Controller(
  delegate: OAsysCas1Delegate?,
) : OAsysCas1 {
  private lateinit var delegate: OAsysCas1Delegate

  init {
    this.delegate = Optional.ofNullable(delegate).orElse(object : OAsysCas1Delegate {})
  }

  override fun getDelegate(): OAsysCas1Delegate = delegate
}
