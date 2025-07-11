package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@Controller
@RequestMapping("\${openapi.communityAccommodationServicesApprovedPremisesCAS1.base-path:/cas1}")
class ReportsCas1Controller(
  delegate: ReportsCas1Delegate?,
) : ReportsCas1 {
  private lateinit var delegate: ReportsCas1Delegate

  init {
    this.delegate = Optional.ofNullable(delegate).orElse(object : ReportsCas1Delegate {})
  }

  override fun getDelegate(): ReportsCas1Delegate = delegate
}
