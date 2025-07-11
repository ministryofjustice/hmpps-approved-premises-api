package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@Controller
@RequestMapping("\${openapi.communityAccommodationServicesApprovedPremisesCAS1.base-path:/cas1}")
class ChangeRequestsCas1Controller(
  delegate: ChangeRequestsCas1Delegate?,
) : ChangeRequestsCas1 {
  private lateinit var delegate: ChangeRequestsCas1Delegate

  init {
    this.delegate = Optional.ofNullable(delegate).orElse(object : ChangeRequestsCas1Delegate {})
  }

  override fun getDelegate(): ChangeRequestsCas1Delegate = delegate
}
